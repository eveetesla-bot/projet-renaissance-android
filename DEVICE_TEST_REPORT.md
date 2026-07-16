# Rapport de test réel — Health Connect

Date : 15 juillet 2026  
Application : Projet Renaissance 0.1.0 debug  
Profil local testé : Gérard

## Appareil

- Google Pixel 8 ;
- Android 17, API 37 ;
- Health Connect installé et disponible ;
- Withings (`com.withings.wiscale2`) installé ;
- Google Fit (`com.google.android.apps.fitness`) installé ;
- Basic-Fit (`com.basicfit.trainingApp`) installé.

Le numéro de série ADB n'est pas consigné dans le rapport.

## Compilation et installation

- Android Gradle Plugin 8.11.1 ;
- Gradle 8.13 ;
- JDK 17.0.19 ;
- compileSdk 36, targetSdk 35 ;
- `BUILD SUCCESSFUL` ;
- 71 tâches Gradle ;
- installation ADB avec conservation des données : réussie ;
- lancement à froid observé : entre 634 ms et 994 ms ;
- aucun crash, `FATAL EXCEPTION`, `SecurityException` ou `SQLiteException` de l'application.

## Tests automatisés

- tests JVM : 15 réussis, 0 échec ;
- test de charge de déduplication : 10 000 intervalles traités dans la limite de 5 secondes ;
- APK de test Android : compilé ;
- migration Room 1→2 exécutée directement sur le Pixel 8 avec AndroidJUnitRunner : `OK (1 test)` en 0,11 s ;
- intégrité de la base extraite après synchronisation : `ok`.

Le lancement Gradle `connectedDebugAndroidTest` a d'abord été bloqué avant exécution par une dépendance UTP non présente et l'accès réseau de la session. Le même test instrumenté a ensuite été installé et exécuté directement par ADB avec succès ; il ne s'agit donc pas d'un échec du test Room.

## Permissions

- Health Connect disponible dans l'application ;
- écran de consentement système affiché correctement ;
- 15 permissions de lecture demandées ;
- 15 permissions accordées ;
- l'application affiche `15 / 15` ;
- aucune permission d'écriture n'a été demandée pendant ce test ;
- la synchronisation périodique est restée désactivée, donc la lecture en arrière-plan n'a pas été testée.

## Résultat de la synchronisation

La première importation porte sur la fenêtre autorisée des 30 derniers jours.

| Indicateur | Résultat |
|---|---:|
| Enregistrements bruts | 40 672 |
| Enregistrements retenus | 27 530 |
| Doublons écartés des totaux | 13 142 |
| Profils locaux destinataires | Gérard uniquement |
| États de synchronisation | 15 accordés, 0 erreur |

Une synchronisation incrémentale ultérieure a traité correctement une insertion et une suppression Health Connect : `1 importée, 1 supprimée`.

## Sources réellement détectées

### Withings — 22 632 enregistrements

| Type | Bruts | Retenus après déduplication |
|---|---:|---:|
| Calories actives | 4 898 | 4 898 |
| Distance | 4 911 | 3 475 |
| Séances | 189 | 184 |
| Étages gravis | 1 | 1 |
| Fréquence cardiaque | 7 690 | 7 690 |
| Sommeil | 32 | 32 |
| Pas | 4 911 | 4 827 |

### Google Fit — 18 040 enregistrements

| Type | Bruts | Retenus après déduplication |
|---|---:|---:|
| Distance | 7 322 | 1 338 |
| Séances | 1 | 0 |
| Fréquence cardiaque au repos | 29 | 29 |
| Sommeil | 9 | 0 |
| Vitesse | 2 303 | 2 303 |
| Pas | 7 228 | 1 605 |
| Calories totales | 1 148 | 1 148 |

### Basic-Fit — 0 enregistrement

L'application Basic-Fit est installée, mais aucun record Health Connect attribué au package Basic-Fit n'a été détecté dans la période lisible et pour les permissions accordées. Projet Renaissance n'affiche donc aucune donnée comme venant de Basic-Fit. Ce résultat ne prouve pas que Basic-Fit ne publie jamais de données : il décrit uniquement ce téléphone, cette période et l'état actuel de Health Connect.

### Types sans donnée détectée

Poids, masse grasse, masse maigre, hydratation et VO₂ max étaient autorisés mais sans record disponible dans la fenêtre importée.

## Déduplication contrôlée

- les pas Withings et Google Fit couvrant les mêmes périodes ne sont pas additionnés deux fois ;
- Withings est prioritaire par défaut pour les pas et le sommeil ;
- les données Google Fit non couvertes par Withings restent retenues ;
- les 32 sessions de sommeil Withings sont retenues et les 9 sessions Google Fit concurrentes sont écartées ;
- 184 séances Withings sont retenues ; la séance Google Fit concurrente est écartée ;
- une suppression signalée par Health Connect a été traitée lors de la synchronisation incrémentale ;
- les records bruts ne sont pas détruits : le champ local `isPreferred` pilote uniquement les totaux et l'affichage.

## Interface vérifiée

- carte « Santé du jour » visible sur l'accueil ;
- écran « Santé et synchronisation » accessible par le bouton Diagnostic et depuis le profil ;
- disponibilité, permissions, synchronisation manuelle et option périodique visibles ;
- sources affichées sous les noms « Google Fit » et « Withings », avec package factuel en dessous ;
- volumes et dernières valeurs visibles par type ;
- historique local et nombre de doublons visibles ;
- suppression locale protégée par une confirmation ;
- grandes zones tactiles et lisibilité validées sur l'écran du Pixel 8.

## Problèmes détectés et corrigés pendant le test

1. Les sources anciennes s'affichaient avec leur package Android. Correction : libellé canonique lisible tout en conservant le package d'origine.
2. Le premier calcul de déduplication était quadratique sur 40 000 lignes. Correction : balayage temporel borné, validation par test de charge et nouvelle synchronisation réelle en quelques secondes.
3. Le journal WAL Room devait être inclus dans l'audit de la base. Le rapport final utilise le fichier principal, le WAL et le SHM afin de refléter l'état exact visible dans l'application.

## Points restant à tester ou développer

- tester le profil Sonia sur son propre téléphone et son propre Health Connect ;
- activer puis tester la synchronisation périodique et la permission de lecture en arrière-plan ;
- ajouter l'écran permettant de modifier effectivement les priorités par type ;
- tester un cas positif Basic-Fit si l'application publie ultérieurement un record attribuable ;
- tester l'écriture optionnelle de séances et poids après son implémentation et un consentement distinct ;
- rejouer les scénarios avec Health Connect Toolbox sur un environnement dédié.
