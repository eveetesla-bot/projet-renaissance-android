# Synchronisation et déduplication Health Connect

## Déclencheurs

### Synchronisation manuelle

L'utilisateur déclenche une synchronisation depuis les paramètres ou le diagnostic. Elle vérifie la disponibilité et les permissions, puis synchronise uniquement les types autorisés.

### Synchronisation périodique

- Réalisée par un `HealthConnectSyncWorker` unique.
- Période initiale raisonnable : 12 heures, avec flexibilité et politique de remplacement `KEEP`.
- Contraintes : batterie non faible ; aucune contrainte réseau, car Health Connect et Room sont locaux.
- Activée seulement après consentement à la lecture en arrière-plan et désactivée dès que l'utilisateur coupe la synchronisation.
- Aucun polling rapide et aucune boucle permanente.

## Algorithme incrémental par type

1. Lire les permissions actuellement accordées.
2. Pour chaque type autorisé, charger son `changesToken` propre au profil.
3. Sans jeton : lire une fenêtre initiale limitée, stocker/dédupliquer, puis demander un jeton de changements.
4. Avec jeton : appeler `getChanges` jusqu'à `hasMore == false`.
5. `UpsertionChange` : ignorer les records de `fr.projetrenaissance` déjà connus, sinon effectuer un upsert transactionnel par identifiant Health Connect.
6. `DeletionChange` : marquer/supprimer le record local correspondant à l'identifiant. Le type est connu grâce au jeton séparé.
7. Enregistrer le nouveau jeton seulement après le succès de toute la transaction du type.
8. Recalculer les gagnants de déduplication et les cartes de synthèse.

Un jeton inutilisé peut expirer. En cas de jeton invalide, l'application relit une fenêtre de recouvrement allant du dernier succès connu jusqu'au présent, bornée par les permissions d'historique, puis déduplique par identifiant et signature.

## Permissions partielles ou révoquées

- Chaque type est indépendant.
- Une permission absente produit l'état `NOT_GRANTED`, pas un échec global.
- Une `SecurityException` entraîne l'arrêt du type concerné, l'effacement de son jeton et la mise à jour du diagnostic.
- Les données déjà importées restent locales tant que l'utilisateur ne demande pas leur suppression.
- Une nouvelle autorisation déclenche une réimportation du type.

## Clés de déduplication

### Niveau 1 : identité Health Connect

`profileId + healthConnectId` est l'identité exacte. Une mise à jour remplace le record local ; une suppression le retire ou le marque supprimé.

### Niveau 2 : équivalence métier inter-sources

Une signature normalisée sert uniquement à éviter le double comptage :

```text
recordType + fenêtre temporelle arrondie + valeur normalisée + attributs métier
```

Tolérances proposées :

| Type | Fenêtre / équivalence |
|---|---|
| Pas, distance, calories, étages | intervalles qui se chevauchent ; sélection par fenêtre, jamais somme brute multi-source |
| Poids, masse grasse, masse maigre | même profil, valeurs proches, horodatage à ±5 minutes |
| Fréquence cardiaque | mêmes échantillons/instants à quelques secondes près ; source mesurée prioritaire |
| Sommeil | chevauchement important de la session et début/fin proches |
| Séance | type compatible, début à ±10 minutes et recouvrement d'au moins 70 % |
| Hydratation | même quantité et saisie à ±2 minutes |

Les tolérances sont des règles de présentation et d'agrégation ; elles ne détruisent jamais un record brut.

## Choix du gagnant

1. appareil de type montre/capteur et méthode mesurée ;
2. source prioritaire configurée pour le type ;
3. méthode automatique plutôt que manuelle ou inconnue ;
4. métadonnées les plus complètes ;
5. `lastModifiedTime` le plus récent ;
6. identifiant Health Connect pour un résultat déterministe.

## Éviter les boucles d'écriture

- Tout record écrit par Renaissance possède un `clientRecordId` stable dérivé de l'identifiant local et un `clientRecordVersion`.
- Les changements dont `dataOrigin.packageName == fr.projetrenaissance` ne sont pas réimportés comme données externes.
- Une fréquence cardiaque externe reste attribuée à sa source et n'est pas copiée vers Health Connect.
- Une séance n'est écrite qu'après validation explicite de sa fin et consentement d'écriture.

## Diagnostic

Le diagnostic est calculé à partir des records réellement lisibles et des métadonnées :

- statut SDK : indisponible, installation/mise à jour requise, disponible ;
- permissions demandées et accordées par type ;
- source et package ;
- types détectés et date du dernier record par source ;
- dernière synchronisation ;
- dernière erreur non sensible ;
- nombre de suppressions et doublons traités lors de la dernière exécution.

Il n'affirme jamais qu'une application ne partage « rien » au-delà de la période lue ou des permissions accordées.

## Plan de tests

### Tests unitaires avec source simulée

- mêmes pas Withings/Google Fit ;
- Withings seul ;
- Basic-Fit absent ;
- séance Basic-Fit présente ;
- permissions partielles ;
- permission révoquée entre deux pages ;
- séances proches avec et sans seuil de recouvrement ;
- `DeletionChange` ;
- changement de source prioritaire ;
- jeton expiré et relecture avec recouvrement.

### Tests instrumentés

- Health Connect indisponible sur API non compatible ;
- parcours d'autorisation et retour dans l'application ;
- Worker unique et annulation ;
- suppression des données locales ;
- attribution affichée.

### Health Connect Toolbox

Sur un émulateur compatible : injecter chaque type, vérifier sa présence dans Health Connect, accorder la lecture, synchroniser, puis comparer les valeurs et sources. Les cas de refus, révocation et suppression doivent être rejoués. Ce test nécessite un émulateur/appareil Health Connect actif ; il ne peut pas être déclaré réussi par une simple compilation JVM.
