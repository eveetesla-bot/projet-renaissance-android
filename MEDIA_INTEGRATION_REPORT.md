# Rapport d’intégration médias

Date : 17 juillet 2026.

## Réellement intégré

- 12 masters PNG originaux, 1536 × 1024.
- 12 rendus Android WebP, 1200 × 800.
- 12 vignettes WebP, 600 × 400.
- Ratio 3:2 cohérent dans la bibliothèque, la fiche et la séance guidée.
- Plein écran par toucher sur le visuel.
- Quatre actions : Mouvement, Machine, Vidéo, Livre.
- Vidéo désactivée sans URL validée.
- Photo machine utilisateur isolée par profil : prise, sélection, remplacement, suppression.
- Priorité photo utilisateur, puis rendu local.
- Reset journée et recommencer séance sans effet sur les médias.
- Reset profil limité aux médias utilisateur du profil.
- Reset total limité aux médias utilisateur ; assets APK conservés.
- Variante dead bug bras au sol retenue pour ne pas activer un mouvement au-dessus de la tête pour Sonia.

## Préparé mais non activé

- Modèle `VideoReference` complet.
- Types `LOCAL_VIDEO`, `EXTERNAL_IMAGE`, `EXTERNAL_VIDEO`, `USER_VIDEO`.
- Ouverture future d’une vidéo validée dans un lecteur externe ou sécurisé.
- Remplacement futur par une photo de machine validée et documentée.

## Manquant

- Aucune vidéo externe n’a satisfait l’ensemble des critères pour les douze exercices ; zéro lien actif.
- Les photos utilisateur réelles restent à prendre dans la salle concernée.
- Les rendus ne remplacent pas une validation biomécanique ou médicale.

## Validation

### Compilation et tests

- JDK 17, API 36, Build Tools 36.0.0.
- `BUILD SUCCESSFUL` en 1 min 02 s, 108 tâches.
- 54 tests unitaires : 54 réussis, 0 échec.
- 19 tests instrumentés Pixel 8 : 19 réussis, 0 échec.
- Les tests appareil couvrent les 24 ressources, les migrations Room 1→2→3→4, le redémarrage de séance, les resets et l’isolation des références photo par profil.
- Le lanceur Gradle UTP a rencontré une erreur d’infrastructure locale après téléchargement ; les mêmes APK de test ont été exécutés directement avec `AndroidJUnitRunner`, avec résultat `OK (19 tests)`.

### Contrôle réel Pixel 8

- Appareil : Google Pixel 8 `shiba`, série `3A240DLJH001ZG`.
- Application Gérard installée par mise à jour ; aucune donnée d’application effacée.
- Seul le package `fr.projetrenaissance` reste installé après le contrôle.
- Bibliothèque : cartes 3:2 lisibles, aucun débordement.
- Mouvement : rendu réaliste, légende et consignes visibles.
- Plein écran : ouverture et fermeture validées.
- Machine : rendu générique, boutons Prendre et Choisir, réglages et source visibles.
- Séance guidée : aperçu réaliste et quatre actions visibles ; vidéo grisée.
- Aucun crash fatal `AndroidRuntime` observé.

### Captures

- `outputs/media-library-pixel8.png`
- `outputs/media-movement-pixel8.png`
- `outputs/media-fullscreen-pixel8.png`
- `outputs/media-machine-pixel8.png`
- `outputs/media-workout-pixel8.png`
- `outputs/media-workout-actions-pixel8.png`

### APK livré

- Fichier : `outputs/Projet-Renaissance-0.1.0-media-redesign-debug.apk`
- Taille : 14 378 438 octets.
- SHA-256 : `EE761389DB2F5371A05D14682DF3AC340A54E5AC126F037B54D97E2E89A9C22B`.

## Exercices encore incomplets

Les 12 exercices ont un visuel principal local. Aucun n’a de vidéo activée. Les dix exercices sur équipement utilisent un rendu générique original tant qu’une photo personnelle de la machine réelle n’a pas été ajoutée. `dead_bug` et `reverse_crunch` n’ont volontairement pas de média machine, car ils utilisent seulement un tapis.
