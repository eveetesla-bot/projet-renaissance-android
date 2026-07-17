# Projet Renaissance

## Médias réalistes hors connexion

- 12 rendus quasi-photoréalistes originaux, sans logo ni modèle constructeur ;
- masters PNG 1536 × 1024 dans `assets/media/primary/` ;
- 12 ressources WebP 1200 × 800 et 12 vignettes 600 × 400 dans l’application ;
- affichage 3:2 dans la bibliothèque, la séance guidée et les fiches ;
- plein écran au toucher ;
- quatre accès distincts : Mouvement, Machine, Vidéo et Livre ;
- photo de machine personnelle réellement fonctionnelle : prendre, choisir,
  remplacer et supprimer ;
- photo personnelle isolée par profil et prioritaire sur le rendu embarqué ;
- aucune URL vidéo activée tant qu’elle n’est pas réellement validée.

Documents du lot : `MEDIA_REDESIGN_PLAN.md`, `MEDIA_SOURCES_INDEX.md`,
`VERIFIED_VIDEO_LINKS.md`, `MACHINE_MEDIA_STATUS.md` et
`MEDIA_INTEGRATION_REPORT.md`.

## Séance et réinitialisations

- Les 12 fiches conservent trajectoire, pivot, appuis, amplitude, respiration et
  zone d’effort avec un visuel principal réaliste.
- « Recommencer la séance » utilise un identifiant de session Room persistant et
  remet atomiquement à zéro séries, RPE, note, douleur et repos.
- Le profil propose trois niveaux : journée, profil local et total.
- Recommencer et reset journée conservent les médias ; reset profil supprime
  uniquement les médias utilisateur du profil ; reset total supprime tous les
  médias utilisateur mais jamais les assets embarqués.
- Après une remise à zéro totale, l’application revient au choix du profil et
  révoque son accès Health Connect sans supprimer les mesures système.
- Le parcours de démarrage place les autorisations et la première
  synchronisation avant le plan du jour, avec une option sans données santé.

Rapport réel Pixel 8 : `MEDIA_RESET_DEVICE_TEST_REPORT.md`.

Application Android locale issue du livre personnalisé « Projet Renaissance –
Gérard & Sonia ». La V1 Gérard est fonctionnellement validée. L'application est
conçue pour rester utilisable hors connexion en salle de sport.

## État réel

### Terminé — V1 Gérard

- profil Gérard persistant, objectifs et contraintes de santé ;
- accueil avec préparation du jour, prochaine séance et progression réelle ;
- programme local de 12 semaines en quatre phases, trois séances par semaine et
  quatre durées ;
- séance compacte avec prescription, réglage, erreurs, alternative, charge,
  répétitions, RPE, repos automatique, gêne et notes ;
- historique des charges : charge précédente et meilleure charge uniquement
  lorsqu'une valeur réelle existe ;
- coach de séance : format conseillé selon Readiness, charge préremplie depuis
  l'historique et ajustement prudent selon RPE/récupération ;
- bouton confirmé pour recommencer et effacer uniquement la progression de la
  séance courante ;
- chronomètre avec actions ±15 secondes ;
- bibliothèque de 12 exercices avec rendus réalistes hors connexion ;
- suivi local et graphiques calendaires sur 7 jours, 30 jours et 12 semaines ;
- nutrition locale, recettes et protections contre les protéines de lait de
  vache ;
- Health Connect : permissions, import local, diagnostic, déduplication et
  priorités par type de donnée ;
- export et import JSON locaux ;
- Room, DataStore et architecture permettant une future synchronisation.

### Partiel

- le contenu Sonia existe avec protections d'épaule, mais son parcours complet
  n'est pas encore validé sur appareil ni médicalement ;
- la bibliothèque nutritionnelle est fonctionnelle mais reste un corpus initial ;
- les graphiques affichent les données réellement disponibles, sans extrapoler
  les jours manquants ;
- le son et la vibration sont configurables, mais les notifications enrichies ne
  constituent pas encore un lot finalisé.

### Futur

- synchronisation cloud ;
- export du journal en PDF ;
- vidéos externes, uniquement après validation éditoriale des URL ;
- enrichissement des recettes et de la bibliothèque d'exercices ;
- validation complète Sonia et test d'accessibilité TalkBack/grandes polices.

## Préparation du jour : score et confiance

Le score de préparation agrège seulement les facteurs disponibles : sommeil,
fréquence cardiaque/VFC, charge récente, douleur et ressenti. Les poids
disponibles sont renormalisés pour calculer le score.

La confiance est différente du score : elle mesure la couverture, la fraîcheur
et la fiabilité des facteurs. Un score élevé fondé sur peu de données peut donc
avoir une confiance faible.

Pour éviter une fausse précision, l'interface affiche une fourchette lorsque la
confiance est inférieure à 85 %. Sa demi-largeur est :

```text
arrondi supérieur((100 - confiance) × 0,15)
```

La fourchette est bornée entre 0 et 100. Les libellés sont :

- 85 à 100 % : fiabilité élevée ;
- 60 à 84 % : fiabilité moyenne ;
- moins de 60 % : fiabilité faible.

La fourchette décrit l'incertitude d'affichage ; elle ne modifie ni le score
calculé ni les règles de sécurité liées à la douleur. Lorsque le score est élevé
mais la confiance inférieure à 85 %, le conseil demande également de confirmer
l'état réel à l'échauffement.

## Programme et historique réel

Les coches du programme proviennent exclusivement des séries Room non marquées
comme test. Les séries d'un même modèle enregistrées à moins de quatre heures
d'intervalle constituent une séance. Une séance enregistrée occupe un seul
créneau hebdomadaire : elle n'est jamais dupliquée sur toutes les semaines de la
phase.

Un exercice machine sans historique affiche « Aucune charge précédente » et
« Aucune meilleure charge ». Le champ reste vide et la série ne peut pas être
validée tant qu'une charge de calibration n'est pas saisie. Les mouvements sans
charge externe peuvent commencer explicitement à `0 kg`.

Le coach ne déduit jamais une charge machine du sexe ou de l'âge. Il maintient
ou réduit une charge personnelle connue ; sans historique, il demande une
calibration légère. Les médias locaux sont ouverts par défaut avant
chaque exercice. Voir [WORKOUT_COACH_REPORT.md](WORKOUT_COACH_REPORT.md).

## Sources Health Connect

Une priorité est configurable séparément pour le sommeil, les pas et les
séances. Une source est sélectionnable uniquement si elle fournit actuellement
au moins une donnée compatible avec le type concerné.

Une source sans donnée est grisée et affiche « Aucune donnée détectée ». Le dépôt
de données applique la même vérification que l'interface avant d'enregistrer une
priorité. Sur le Pixel 8 de validation, Withings et Google Fit fournissent des
données compatibles ; Basic-Fit n'en fournit aucune et reste indisponible.

## Testé sur appareil

Appareil de référence : Google Pixel 8 (`shiba`).

Le lot de refonte médias a été contrôlé le 17 juillet 2026 :

- 12 visuels principaux et 12 vignettes décodés sur le Pixel 8 ;
- bibliothèque, fiche Mouvement, plein écran, fiche Machine et séance guidée ;
- actions Prendre / Choisir affichées et stockage isolé par profil ;
- vidéo inactive sur les 12 exercices en l’absence d’URL validée ;
- règles de reset média et migrations Room validées ;
- 54 tests unitaires réussis ;
- 19 tests instrumentés réussis ;
- `BUILD SUCCESSFUL` sur 108 tâches Gradle ;
- installation Gérard par mise à jour, sans effacement de ses données ;
- aucune exception Android fatale observée.

Rapport : [MEDIA_INTEGRATION_REPORT.md](MEDIA_INTEGRATION_REPORT.md).

Le dernier lot correctif a été contrôlé le 16 juillet 2026 :

- installation par mise à jour, données d'application conservées ;
- Accueil : fourchette et fiabilité de la préparation ;
- Programme : une séance réelle cochée une seule fois (`1 / 9`) ;
- Séance sans historique : absences explicites et validation désactivée ;
- Santé et synchronisation : 17 permissions, sources détectées et Basic-Fit
  désactivé ;
- 42 tests unitaires réussis ;
- 4 tests instrumentés Room/appareil réussis ;
- aucune exception Android fatale au lancement et pendant les contrôles.

### Variante Sonia Test isolée

Une seconde application peut être installée à côté de Gérard :

- package : `fr.projetrenaissance.soniatest` ;
- nom : « Renaissance Sonia Test » ;
- profil verrouillé sur Sonia ;
- stockage, préférences, permissions et WorkManager séparés.

Elle permet de tester le parcours Sonia sur le même téléphone sans modifier les
données locales de l'application Gérard. Les mesures Health Connect lues sur ce
téléphone restent toutefois des données proxy de Gérard, pas des mesures de
Sonia. Le test proxy complet est documenté dans
[SONIA_DEVICE_TEST_REPORT.md](SONIA_DEVICE_TEST_REPORT.md).

Voir [CORRECTIVE_PASS_DEVICE_TEST_REPORT.md](CORRECTIVE_PASS_DEVICE_TEST_REPORT.md)
et les captures `outputs/corrective-*.png`.

## Prérequis de compilation

- JDK 17 ;
- Android SDK Platform 36 ;
- Android Build Tools 36.0.0.

Chaîne du projet :

- Android Gradle Plugin 8.11.1 ;
- Gradle Wrapper 8.13 ;
- Kotlin 1.9.24 ;
- KSP 1.9.24-1.0.20 ;
- Compose Compiler 1.5.14 ;
- Room 2.6.1 ;
- Health Connect 1.1.0 ;
- `compileSdk 36`, `targetSdk 35`, `minSdk 26`.

## Compiler

Depuis PowerShell à la racine du projet :

```powershell
powershell -ExecutionPolicy Bypass -File .\build-local.ps1
```

Le script vérifie JDK 17, Android 36 et Build Tools 36 avant d'exécuter les tests
unitaires et de produire l'application Gérard, Sonia Test et l'APK
d'instrumentation Sonia.

APK de développement :

```text
app/build/outputs/apk/debug/app-debug.apk
```

APK Sonia Test :

```text
app/build/outputs/apk/soniaTest/app-soniaTest.apk
```

Installation manuelle :

```powershell
.\work\toolchain\sdk\platform-tools\adb.exe install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## Structure

```text
app/src/main/java/fr/projetrenaissance/
├── data/          Room, DataStore, dépôt et Health Connect
├── domain/        règles métier et calculs testables
└── presentation/  Jetpack Compose, navigation et ViewModel
```

Documents complémentaires :

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [DATA_MODEL.md](DATA_MODEL.md)
- [BOOK_ANALYSIS.md](BOOK_ANALYSIS.md)
- [HEALTH_CONNECT_ARCHITECTURE.md](HEALTH_CONNECT_ARCHITECTURE.md)
- [DATA_SOURCE_PRIORITY.md](DATA_SOURCE_PRIORITY.md)
- [SYNC_AND_DEDUPLICATION.md](SYNC_AND_DEDUPLICATION.md)
- [SONIA_DEVICE_TEST_PROTOCOL.md](SONIA_DEVICE_TEST_PROTOCOL.md)
- [SONIA_DEVICE_TEST_REPORT.md](SONIA_DEVICE_TEST_REPORT.md)

## Données, confidentialité et santé

La V1 n'utilise aucun client réseau applicatif. Les données restent dans Room et
DataStore ; Health Connect est l'unique passerelle santé locale. L'application
n'invente ni mesure, ni source, ni vidéo.

L'application ne remplace ni diagnostic, ni avis médical ou paramédical. Pour
Gérard, les recommandations excluent les protéines de lait de vache. Pour Sonia,
les protections existantes ne valent pas validation médicale.
