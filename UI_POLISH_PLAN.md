# Plan de finition visuelle

## Intention

Faire de Projet Renaissance le prolongement mobile du livre : une interface calme, éditoriale et chaleureuse, utilisable rapidement en salle. La finition ne modifie ni les calculs, ni la séparation Gérard/Sonia, ni les règles de sécurité.

## Audit de départ

- La palette correspond déjà au livre, mais elle est peu déclinée : surfaces majoritairement blanches et cartes uniformes.
- La hiérarchie repose surtout sur des textes en capitales ; les écrans manquent de respirations, de surtitres et de rythme éditorial.
- `PremiumCard`, `ScreenTitle`, `DetailLine` et `MetricChart` sont trop génériques et restent privés dans un fichier de près de 1 000 lignes.
- La préparation quotidienne présente les bonnes données, mais sous forme d’une liste textuelle sans jauge ni lecture immédiate.
- Les graphiques montrent les séries réelles, mais sans moyenne, tendance, période ni contextualisation.
- Les illustrations d’exercices sont fonctionnelles ; leur cadre, leurs commandes et leurs informations doivent devenir une vraie fiche pédagogique.
- La séance en cours privilégie la fonction mais ne montre pas assez la progression ni le focus du mouvement.

## Lots

### Lot A — fondations

- consolider couleurs, typographies, rayons, ombres et espacements ;
- créer `EditorialPageHeader`, `RenaissanceInsightCard`, `PremiumMetricCard`, `PremiumChartCard`, `CoachTipCard`, `SourceBadge`, `ConfidenceBadge` et `TrendChip` ;
- conserver Material 3 et les dépendances existantes.

### Lot B — accueil et préparation

- header éditorial avec repère de profil ;
- jauge de préparation et facteurs sous forme de badges ;
- conseil du jour isolé ;
- santé, séance suivante et semaine mieux hiérarchisées.

### Lot C — suivi

- cartes métriques avec unité, moyenne et tendance ;
- graphiques avec ligne moyenne, grille légère et dernier point mis en avant ;
- sélecteur 7/30 jours purement visuel sur les données disponibles ;
- messages honnêtes lorsque les données sont insuffisantes.

### Lot D — exercices et séance

- cartes d’aperçu illustrées ;
- fiche exercice structurée en sections éditoriales ;
- prudence épaule immédiatement visible pour Sonia ;
- séance immersive : progression, focus, prescription, saisie et repos.

## Critères de validation

- aucune valeur inventée ;
- zones tactiles d’au moins 48 dp ;
- contraste lisible sur fond ivoire ;
- un seul encadré éditorial par écran important ;
- compilation après chaque lot ;
- test et captures sur Pixel 8 avant livraison.
