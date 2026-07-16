# Rapport Sonia — test fonctionnel proxy sur Pixel 8

**Statut : TEST FONCTIONNEL PROXY VALIDÉ**

Date : 16 juillet 2026  
Téléphone : Google Pixel 8 (`shiba`)  
Numéro de série ADB : `3A240DLJH001ZG`  
Android : 17, API 37, build `CP2A.260705.006`  
Health Connect : `2026.05.14.00.release`

## Portée et séparation

Le test a été réalisé sur le téléphone de Gérard, à la demande de l'utilisateur,
avec une seconde application :

- Gérard : `fr.projetrenaissance` ;
- Sonia Test : `fr.projetrenaissance.soniatest`.

Les deux applications ont des bases Room, préférences DataStore, permissions et
tâches WorkManager séparées. Sonia Test est verrouillée sur le profil Sonia et
ne propose pas le changement vers Gérard.

Les mesures Health Connect de ce rapport appartiennent au téléphone de Gérard et
servent uniquement de données proxy pour valider le fonctionnement Sonia. Elles
ne doivent pas être interprétées comme des mesures médicales ou sportives de
Sonia.

Le chemin du package Gérard est resté inchangé pendant le test. Ses 17
permissions Health Connect sont toujours accordées.

## APK testé

- nom visible : `Renaissance Sonia Test` ;
- version : `0.1.0-sonia-test` ;
- APK : `outputs/Projet-Renaissance-0.1.0-sonia-test-debug.apk` ;
- taille : 11 857 804 octets ;
- SHA-256 :
  `524CE5BC9A23419255CAF0A1BAD1402353AECEAE1F6F50CF9620722B44203ED7`.

L'inspection du contenu a confirmé l'absence de base, DataStore, sauvegarde,
JSON, CSV ou autre donnée personnelle embarquée.

## Installation vierge

Avant synchronisation :

- Sonia sélectionnée automatiquement ;
- aucun changement de profil proposé ;
- aucun score de préparation ;
- aucune évaluation locale ;
- sommeil : `0/4 nuits` ;
- FC au repos : `0/3 valeurs` ;
- VFC RMSSD : `0/4 valeurs` ;
- aucune source sélectionnable ;
- six mentions « Aucune donnée détectée » sur les priorités affichées.

## Autorisations et première synchronisation

- permissions de lecture Health Connect accordées : 17 ;
- types synchronisés : 16/16 ;
- importés : 42 280 ;
- supprimés signalés par Health Connect : 0 ;
- erreurs de synchronisation : 0 ;
- résultat : « Synchronisation terminée ».

## Données importées par source

| Source | Paquet | Importées | Types |
|---|---|---:|---|
| Withings | `com.withings.wiscale2` | 24 701 | calories actives, distance, pas, fréquence cardiaque, RMSSD, sommeil, séances, étages |
| Google Fit | `com.google.android.apps.fitness` | 17 579 | calories totales, distance, pas, vitesse, FC au repos, sommeil, séances |
| Basic-Fit | aucune source détectée | 0 | aucun |
| Total |  | 42 280 |  |

Basic-Fit est resté grisé, non sélectionnable et accompagné de « Aucune donnée
détectée ».

## Déduplication

- enregistrements locaux : 42 280 ;
- enregistrements préférés utilisés dans les agrégats : 29 498 ;
- doublons écartés des totaux : 12 782.

La priorité par défaut a retenu Withings pour les types disponibles chez
plusieurs sources. Google Fit reste utilisé pour ses types complémentaires,
notamment la FC au repos.

## Métriques disponibles

| Métrique | Résultat observé | Source principale | Baseline |
|---|---|---|---|
| Sommeil | dernière nuit 7,4 h ; résumé 7,3 h | Withings | 25 nuits, référence 5 h 34 |
| FC au repos | 63 bpm | Google Fit | 26 valeurs, référence 62,0 bpm |
| VFC RMSSD | dernière valeur 52,0 ms | Withings | 2 160 valeurs, référence 37,4 ms |
| Pas | 546 au moment de la capture | Withings | sans baseline Readiness |
| Poids | absent | aucune | aucune |
| Séances | Withings 182, Google Fit 1 | Withings | charge récente disponible |

Autres métriques détectées : calories actives, calories totales, distance,
fréquence cardiaque, vitesse et étages.

Données absentes : poids, hydratation, masse grasse, masse maigre et VO2 max.

## Préparation du jour

- score interne calculé : 99/100 ;
- affichage prudent : `93–100` ;
- classification : « Préparation favorable estimée » ;
- confiance : 66 % ;
- niveau : « Fiabilité moyenne » ;
- conseil : « Séance normale envisagée, à confirmer à l'échauffement. »

Facteurs inclus :

- sommeil réel agrégé : 100 ;
- VFC RMSSD comparée à la moyenne personnelle : 100 ;
- charge réelle des dernières 72 h : 95.

Facteurs absents :

- douleur/confort ;
- ressenti subjectif.

Le résumé Suivi affichait initialement `99 / 100`. Ce défaut de fausse précision
a été corrigé pendant le test ; la version finale affiche également `93–100`
avec « Fiabilité moyenne · confiance 66 % ».

## Sécurité Sonia

- capsulite de l'épaule droite et migraines visibles dans le profil ;
- validation médicale désactivée ;
- rappel permanent sur les mouvements au-dessus de la tête ;
- aucun changement vers Gérard disponible dans Sonia Test ;
- programme initial à 0/9 séance ;
- aucun mouvement au-dessus de la tête activé ;
- aucun exercice conditionnel d'épaule proposé dans la séance A.

Séance A parcourue sans enregistrer de série :

1. vélo ;
2. presse à cuisses ;
3. leg curl assis ;
4. abducteurs machine ;
5. mollets à la presse ;
6. dead bug bras au sol.

## Tests et stabilité

- script officiel final `build-local.ps1` : **BUILD SUCCESSFUL in 55s**,
  108 tâches ;
- compilation finale Sonia Test : **BUILD SUCCESSFUL in 5m 33s** ;
- 46 tests unitaires, 0 échec ;
- APK d'instrumentation Sonia : **BUILD SUCCESSFUL in 3m 19s** ;
- 6 tests instrumentés sur le Pixel 8 : **OK (6 tests)** en 0,615 s ;
- aucune exception `AndroidRuntime` fatale observée.

Les tests couvrent les migrations Room, l'installation vierge sans donnée
personnelle, l'isolation Gérard/Sonia et la réinitialisation sur base en mémoire.
Les contrôles visant la base réelle sont en lecture seule.

## Réinitialisation et nettoyage

La réinitialisation complète de Sonia Test a été confirmée sur l'appareil :

- enregistrements : 42 280 vers 0 ;
- sommeil : retour à `0/4` ;
- FC au repos : retour à `0/3` ;
- RMSSD : retour à `0/4` ;
- sources : retour à « Aucune donnée détectée » ;
- aucune suppression dans Health Connect, Withings ou Google Fit.

Après vérification, les packages suivants ont été retirés :

- `fr.projetrenaissance.soniatest.test` ;
- `fr.projetrenaissance.soniatest`.

L'application Gérard `fr.projetrenaissance` reste installée et fonctionnelle.

## Captures

- `outputs/sonia-proxy-profile.png`
- `outputs/sonia-proxy-clean-health.png`
- `outputs/sonia-proxy-baselines.png`
- `outputs/sonia-proxy-sources.png`
- `outputs/sonia-proxy-readiness.png`
- `outputs/sonia-proxy-tracking.png`
- `outputs/sonia-proxy-program.png`
- `outputs/sonia-proxy-safe-session.png`
- `outputs/sonia-proxy-reset-confirmation.png`
- `outputs/sonia-proxy-after-reset.png`

## Conclusion

Le fonctionnement Sonia est validé techniquement avec des données proxy sur le
Pixel 8 de Gérard, sans mélange de stockage entre les deux applications. La
validation avec les données réelles de Sonia reste une étape distincte à
effectuer sur son téléphone ou avec ses comptes Health Connect.
