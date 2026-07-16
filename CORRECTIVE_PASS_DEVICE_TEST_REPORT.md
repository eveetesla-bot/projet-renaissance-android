# Rapport de passe corrective limitée — Pixel 8

Date : 16 juillet 2026  
Profil contrôlé : Gérard  
Appareil : Google Pixel 8 (`shiba`)  
Application : `fr.projetrenaissance`

## Résultat

La passe corrective est compilée, testée et installée sur le Pixel 8. Elle ne
contient aucune refonte graphique et aucune modification spécifique au parcours
Sonia.

L'installation a été effectuée avec mise à jour de l'application existante. Elle
n'a pas désinstallé l'application et n'a pas effacé ses données. Aucun bilan,
aucune série et aucune mesure Gérard n'a été créé, modifié ou supprimé pendant
les contrôles visuels.

## Corrections vérifiées

### 1. Préparation du jour

Le score ponctuel n'est plus présenté seul lorsque la confiance est inférieure à
85 %. Sur les données réelles du téléphone :

- estimation affichée : `93–100` ;
- libellé : « Préparation favorable estimée » ;
- fiabilité : « Fiabilité moyenne » ;
- confiance : 66 % ;
- conseil : « Séance normale envisagée, à confirmer à l'échauffement. »

La fourchette est calculée autour du score avec une demi-largeur égale à
`ceil((100 - confiance) × 0,15)`, puis bornée entre 0 et 100. La confiance mesure
la couverture et la fraîcheur des facteurs ; elle ne remplace pas le score et ne
modifie pas les règles de sécurité.

### 2. Sources Health Connect

L'éligibilité est vérifiée pour chaque couple type/source. Une source sans donnée
compatible est désactivée dans l'interface et refusée une seconde fois par le
dépôt de données.

État observé :

- sommeil : Withings 33 données, Google Fit 9, Basic-Fit aucune ;
- pas : Withings 4 943 données, Google Fit 7 246, Basic-Fit aucune ;
- séances : Withings 190 données, Google Fit 1, Basic-Fit aucune.

Pour les trois types, Basic-Fit :

- est grisé ;
- affiche « Aucune donnée détectée » ;
- a `enabled=false` ;
- reste non sélectionné après un appui de contrôle.

Les 17 permissions Health Connect précédemment accordées sont toujours actives.
Aucune nouvelle synchronisation n'a été déclenchée pendant cette passe afin de
respecter l'interdiction de modifier les données réelles. L'écran, les données
déjà synchronisées, les priorités et l'état des permissions ont été contrôlés.

### 3. Programme

La cause des coches répétées était l'utilisation du même identifiant de modèle A
pour toutes les semaines de sa phase. L'interface considérait donc une séance A
comme terminée dans chaque semaine.

La progression regroupe maintenant les séries réelles d'un modèle en séances :
des séries séparées par moins de quatre heures appartiennent à la même séance.
Chaque séance enregistrée occupe ensuite un seul créneau hebdomadaire.

État réel observé :

- phase Ancrer : `1 / 9 séances enregistrées` ;
- semaine 1 : A cochée, B et C non cochées ;
- semaines 2 et 3 : aucune séance cochée ;
- phase Construire visible à `0 / 9`.

Aucun état de démonstration ne participe au calcul et les séries `isTest=true`
sont exclues.

### 4. Charges

La séance B a été ouverte sur « Hip thrust guidé », exercice sans historique :

- « Aucune charge précédente » ;
- « Aucune meilleure charge » ;
- champ de charge vide ;
- bouton « Série terminée » désactivé ;
- rappel : saisir `0` uniquement si la charge réelle est de 0 kg.

Une valeur vide n'est donc plus convertie silencieusement en `0 kg`. Les valeurs
`0 kg` réellement enregistrées restent valides et visibles.

## Compilation et tests

Commande :

```powershell
powershell -ExecutionPolicy Bypass -File .\build-local.ps1
```

Résultat final :

- **BUILD SUCCESSFUL in 2m 32s** ;
- 71 tâches Gradle ;
- 42 tests unitaires ;
- 0 échec, 0 erreur, 0 test ignoré ;
- APK application généré ;
- APK d'instrumentation généré.

Tests instrumentés exécutés sur le Pixel 8 :

- migration Room 1 vers 2 ;
- migration Room 2 vers 3 ;
- absence de la série ciblée de validation du chronomètre ;
- absence de séries marquées comme test, en lecture seule.

Résultat final : **OK (4 tests)** en 1,193 s.

Le test historique de nettoyage à 42 kg a été rendu strictement en lecture seule
afin qu'une future charge réelle de 42 kg ne puisse jamais être supprimée par la
suite de tests.

## Contrôle d'exécution

- application installée et lancée ;
- paquet `fr.projetrenaissance.test` retiré après les essais ;
- 17 permissions Health Connect accordées ;
- aucune exception `AndroidRuntime` fatale observée ;
- Accueil, Programme, Séance et Santé et synchronisation inspectés ;
- captures relues visuellement après transfert binaire depuis le téléphone.

## APK

- source : `app/build/outputs/apk/debug/app-debug.apk` ;
- livraison : `outputs/Projet-Renaissance-0.1.0-corrective-debug.apk` ;
- taille : 12 569 973 octets ;
- SHA-256 :
  `34E16C1E31047E24B8CE9739CBA68760ED7E39B407785409C4A50F6973DC8C74`.

## Fichiers modifiés

- `README.md`
- `app/src/main/java/fr/projetrenaissance/domain/DailyReadinessEngine.kt`
- `app/src/main/java/fr/projetrenaissance/domain/ProgramProgress.kt`
- `app/src/main/java/fr/projetrenaissance/data/health/HealthDataRepository.kt`
- `app/src/main/java/fr/projetrenaissance/presentation/AppViewModel.kt`
- `app/src/main/java/fr/projetrenaissance/presentation/DesignSystem.kt`
- `app/src/main/java/fr/projetrenaissance/presentation/RenaissanceApp.kt`
- `app/src/test/java/fr/projetrenaissance/domain/DailyReadinessEngineTest.kt`
- `app/src/test/java/fr/projetrenaissance/domain/ProgramProgressTest.kt`
- `app/src/androidTest/java/fr/projetrenaissance/SessionChargeCleanupDeviceTest.kt`
- `CORRECTIVE_PASS_DEVICE_TEST_REPORT.md`

## Captures

- `outputs/corrective-home-readiness.png`
- `outputs/corrective-program-real-progress.png`
- `outputs/corrective-session-no-history.png`
- `outputs/corrective-health-sources.png`

## Restant

Cette passe ne démarre aucun travail Sonia. Les éléments futurs et partiels sont
listés séparément dans `README.md`.
