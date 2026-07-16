# Rapport de finition et de test réel — Pixel 8

Date du contrôle final : 16 juillet 2026  
Appareil : Google Pixel 8 (`shiba`)  
Application : `fr.projetrenaissance`

## Résultat

L'incrément de finition est compilé, installé et lancé sur le Pixel 8. Les données
existantes de Gérard ont été restaurées après la stabilisation de la signature de
développement. Aucun plantage Android fatal n'a été observé au lancement final.

Le paquet d'instrumentation `fr.projetrenaissance.test` a été retiré du téléphone
après les essais. L'application principale reste installée.

## Compilation et tests automatisés

- `testDebugUnitTest assembleDebugAndroidTest assembleDebug` : **BUILD SUCCESSFUL**
- durée du dernier lot complet : 1 min 02 s ;
- 71 tâches Gradle ;
- 36 tests unitaires, 0 échec, 0 erreur, 0 ignoré ;
- 4 tests instrumentés exécutés ensemble : **OK (4 tests)** ;
- test ciblé de nettoyage de la série temporaire : **OK (1 test)**.

Tests Room/appareil couverts :

- migration Room 1 vers 2 ;
- migration Room 2 vers 3 et ajout de `isTest` ;
- suppression ciblée de la série de validation du chronomètre ;
- suppression exacte de la série temporaire utilisée pour la mémoire de charge.

## Santé et sources

- Health Connect accessible sur l'appareil réel ;
- 17 permissions de lecture accordées, y compris la lecture en arrière-plan ;
- données Withings détectées et conservées ;
- choix de priorité des pas passé à Google Fit, vérifié après arrêt/reprise ;
- choix remis à Withings, vérifié après un second arrêt/reprise ;
- écrans de diagnostic et de priorité disponibles pour sommeil, pas et séances ;
- préparation affichée après restauration : « Bonne préparation », confiance 66 %.

Les marques Withings, Google Fit et Basic-Fit désignent des sources détectées ou
sélectionnables. L'application ne fabrique aucune donnée pour une source absente.

## Déduplication et données de test

La série demandée, créée le 15 juillet 2026 vers 18:19 pour valider le
chronomètre (0 kg, 10 répétitions, RPE 7), a été supprimée par une règle étroite
sur le profil, les valeurs et la fenêtre temporelle.

Deux autres séries distinctes ayant les mêmes valeurs, enregistrées vers 23:02,
ont été volontairement conservées. Cela confirme que le nettoyage n'efface pas
globalement l'historique.

Une série temporaire à 42 kg a ensuite servi au test réel de mémoire :

1. validation de la série et démarrage automatique du repos ;
2. fermeture et réouverture de la séance ;
3. affichage de 42 kg comme charge précédente et meilleure charge ;
4. suppression de cette seule série par son identifiant ;
5. réouverture et retour à la valeur historique précédente.

Les futures données de validation peuvent être marquées avec `isTest=true` puis
nettoyées sans toucher aux séries réelles.

## Contrôles fonctionnels réels

- navigation basse avec icônes Material et libellés ;
- séance compacte utilisable pendant l'entraînement ;
- saisie décimale de charge, charge précédente, meilleure charge et raccourcis ;
- repos automatique et actions ±15 secondes ;
- programme de 12 semaines découpé en quatre phases ;
- graphiques calendrier sur 7 jours, 30 jours et 12 semaines ;
- jours sans données visibles et sélection tactile d'un point ;
- nutrition locale, recettes, favoris et liste de courses ;
- protections Gérard contre toutes les protéines de lait de vache ;
- détail exercice avec trajectoire, appui, pivot, bonne exécution et erreur ;
- priorité des sources Health Connect persistante.

Les guides de mouvement locaux couvrent les 12 exercices actuels :

1. vélo ;
2. presse à cuisses ;
3. développé assis ;
4. tirage assis ;
5. leg curl ;
6. élévation latérale ;
7. mollets à la presse ;
8. hip thrust ;
9. leg extension ;
10. abducteurs ;
11. dead bug ;
12. reverse crunch.

## Signature et sauvegarde

La signature debug est désormais stable dans le dossier de travail ignoré par
Git : `work/signing/renaissance-debug.keystore`.

Avant le changement de signature, les données internes ont été sauvegardées dans :

`work/renaissance-internal-backup-20260716-v2.tar`

- taille : 17 264 128 octets ;
- SHA-256 :
  `08BB2A5B3038AA9CB06C676810B9968876556F9D042E93D44A030386ECEA385D`.

La base Room, son journal WAL et les préférences DataStore étaient présents dans
l'archive. La restauration a été contrôlée dans l'interface.

## APK final

- fichier Gradle : `app/build/outputs/apk/debug/app-debug.apk` ;
- copie livrée : `outputs/Projet-Renaissance-0.1.0-finishing-debug.apk` ;
- taille : 12 245 328 octets ;
- SHA-256 :
  `4741F157B8EF8DCD789833B88B59A9D598FAC55A2B785860925FB5E4E408568B`.

## Fichiers de code modifiés dans cet incrément

- `app/build.gradle.kts`
- `app/src/main/java/fr/projetrenaissance/RenaissanceApplication.kt`
- `app/src/main/java/fr/projetrenaissance/data/AppDatabase.kt`
- `app/src/main/java/fr/projetrenaissance/data/Preferences.kt`
- `app/src/main/java/fr/projetrenaissance/data/RenaissanceRepository.kt`
- `app/src/main/java/fr/projetrenaissance/data/health/HealthDataRepository.kt`
- `app/src/main/java/fr/projetrenaissance/presentation/AppViewModel.kt`
- `app/src/main/java/fr/projetrenaissance/presentation/DesignSystem.kt`
- `app/src/main/java/fr/projetrenaissance/presentation/ExerciseMediaUi.kt`
- `app/src/main/java/fr/projetrenaissance/presentation/RenaissanceApp.kt`
- `app/src/androidTest/java/fr/projetrenaissance/Migration2To3Test.kt`
- `app/src/androidTest/java/fr/projetrenaissance/ValidationCleanupDeviceTest.kt`
- `app/src/androidTest/java/fr/projetrenaissance/SessionChargeCleanupDeviceTest.kt`

## Restant avant une validation complète du parcours Sonia

- exécuter sur appareil une séance entière avec le profil Sonia ;
- contrôler chaque adaptation d'épaule avec Sonia et son professionnel de santé ;
- tester les masquages d'exercices non validés sur toutes les semaines ;
- vérifier les retours douleur, migraine et mobilité sur plusieurs séances réelles ;
- réaliser un test d'accessibilité complet (TalkBack, contraste et grandes polices).

Ces points ne sont pas déclarés terminés dans ce lot.
