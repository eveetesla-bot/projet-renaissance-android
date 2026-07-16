# Notes de refonte par écran

## Accueil

- Remplacer l’ouverture plate par `EditorialPageHeader` avec profil, salutation et phrase de coaching.
- Placer la préparation comme héro principal.
- Regrouper santé du jour en lignes mesurées et badges de source.
- Donner à la séance suivante une surface bleu nuit avec bouton cuivré.
- Transformer les raccourcis en tuiles compactes illustrées par des repères typographiques.

## Préparation du jour

- Grande jauge circulaire 0–100.
- Classification et confiance immédiatement lisibles.
- Facteurs en cartes compactes avec score.
- Conseil du jour dans `CoachTipCard`.
- Détail extensible ou page dédiée ultérieurement ; le premier lot garde les données existantes dans la carte.

## Suivi

- En-tête éditorial et choix 7/30 jours.
- Carte récapitulative du poids sans espace vide si absent.
- `PremiumChartCard` : titre, période, dernière valeur, moyenne, tendance, source et commentaire.
- Grille et moyenne discrètes ; point final accentué.

## Bibliothèque

- Carte large avec illustration intégrée, badges profil/sécurité, nom et muscles.
- Action « Voir la fiche » distincte de l’ouverture des informations secondaires.
- Filtres conservés mais allégés visuellement.

## Détail exercice

- En-tête avec groupe musculaire et badge de sécurité.
- Démonstration mise en scène sur papier chaud.
- Commandes réunies dans une barre claire.
- Sections : réglage, exécution, respiration, erreurs, alternative.
- Un `CoachTipCard` reformulé depuis les consignes existantes.

## Séance en cours

- Barre de progression de séance.
- Focus éditorial au-dessus du mouvement.
- Vignette démonstration immédiatement accessible.
- Prescription sombre compacte ; saisie dans une carte claire.
- Chronomètre traité comme un bandeau de repos.

## Composants à refactoriser

- `ScreenTitle` → `EditorialPageHeader`
- `PremiumCard` → composant public configurable du design system
- `ReadinessCard` → jauge + facteurs + conseil
- `RealMetricChart` et `MetricChart` → `PremiumChartCard`
- `ExerciseCard` → `ExercisePreviewCard`
- `DetailLine` → ligne éditoriale avec source/valeur
- `PrescriptionCard`, `CompactTimer`, `QuickAction` → variantes premium cohérentes
