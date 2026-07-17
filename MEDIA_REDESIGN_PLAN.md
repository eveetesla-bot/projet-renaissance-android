# Refonte des médias

Date de référence : 17 juillet 2026.

## Objectif

Remplacer la lecture trop schématique des exercices par une expérience visuelle premium, locale et juridiquement traçable, sans reprendre de média Basic-Fit ni de modèle constructeur identifiable.

## Stratégie retenue

Chaque exercice possède quatre catégories indépendantes :

1. `PRIMARY_VISUAL` : rendu quasi-photoréaliste original, intégré à l’APK, avec vignette optimisée.
2. `MACHINE_VISUAL` : vue générique locale lorsque l’exercice utilise une machine ; une photo personnelle du profil est toujours prioritaire.
3. `VIDEO` : lien externe uniquement après vérification explicite. Sans lien validé, le bouton est désactivé.
4. `BOOK_REFERENCE` : fiche éditoriale locale issue du livre fourni.

Le modèle `ExerciseMediaAsset` porte l’origine, la date de vérification, la disponibilité hors ligne et le statut utilisateur. `VideoReference` est prêt mais aucune URL n’est activée dans ce lot.

## Règles de priorité

- Visuel principal : média embarqué `primary_<exerciseId>`.
- Carte : vignette embarquée `thumb_<exerciseId>`.
- Machine : photo personnelle du profil, puis rendu générique embarqué.
- Vidéo : seulement une `VideoReference` vérifiée ; sinon état indisponible.
- Livre : fiche locale, même hors connexion.

## Photos utilisateur

- Prise de photo via l’application caméra.
- Choix d’une image existante via le sélecteur Android.
- Remplacement et suppression disponibles depuis l’onglet Machine.
- Stockage et préférence séparés par profil et par exercice.
- Reset journée et recommencer séance : aucun effet sur les médias.
- Reset profil : supprime seulement les références et fichiers du profil actif.
- Reset total : supprime tous les médias utilisateur locaux ; les médias embarqués restent dans l’APK.

## Contrôle qualité prévu

- 12 ressources principales et 12 vignettes décodables.
- Ratio constant 3:2.
- Plein écran sans débordement.
- Photo utilisateur prioritaire et persistante.
- Bouton vidéo inactif sans URL.
- Vérification de la séance guidée et des quatre actions.

