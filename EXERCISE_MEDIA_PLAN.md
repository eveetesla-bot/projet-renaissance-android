# Plan des médias d'exercices

## Objectif

Fournir hors connexion des supports originaux, sobres et pédagogiques. Aucun média protégé, aucune URL inventée et aucune illustration sans rapport avec le mouvement.

## Modèle

```text
ExerciseMedia
├── id
├── exerciseId
├── mediaType
├── localResource
├── externalUrl?
├── thumbnail
├── durationSeconds?
├── source
├── verificationDate
└── accessibilityDescription
```

Types : `STATIC_IMAGE`, `STEP_BY_STEP_IMAGES`, `LOTTIE_ANIMATION`, `LOCAL_VIDEO`, `VERIFIED_EXTERNAL_VIDEO`.

Le premier lot utilise `STEP_BY_STEP_IMAGES` : une planche locale originale en trois panneaux (départ, mouvement, fin), accompagnée de légendes Compose. Ce choix fonctionne hors connexion, n'ajoute aucune dépendance et permet lecture lente, pause et image par image sans moteur vidéo.

## Douze exercices prioritaires

| Exercice | Identifiant | Points visuels |
|---|---|---|
| Vélo | `bike` | selle, genou souple, buste stable |
| Presse à cuisses | `leg_press` | départ fléchi, poussée, genoux non verrouillés |
| Développé poitrine machine | `chest_press` | poignées poitrine, poussée, épaules basses |
| Rowing assis poulie | `seated_row` | prise neutre, tirage, omoplates rapprochées |
| Leg curl assis | `leg_curl` | axe genou, flexion, retour contrôlé |
| Élévations latérales | `lateral_raise` | bras bas, montée confortable, sans haussement |
| Mollets à la presse | `calf_press` | avant-pied stable, extension, retour sans rebond |
| Hip thrust guidé | `hip_thrust` | bassin bas, extension, tibias verticaux |
| Leg extension | `leg_extension` | axe genou, extension, retour lent |
| Abducteurs machine | `abductors` | dos soutenu, ouverture, retour contrôlé |
| Dead bug bras au sol | `dead_bug` | dos neutre, extension opposée, retour |
| Reverse crunch | `reverse_crunch` | bassin stable, enroulement, retour sans élan |

## Direction artistique

- fond clair, bleu nuit, vert sauge et cuivre ;
- personnage adulte neutre et respectueux, vêtements de sport couvrants ;
- machine simplifiée mais mécaniquement reconnaissable ;
- trois panneaux cohérents avec flèche de mouvement discrète ;
- aucun texte raster indispensable : les légendes accessibles restent dans Compose ;
- cadrage constant, contraste élevé, détails non décoratifs limités.

## Interface

- vignette sur la carte exercice ;
- boutons « Démonstration », « Réglages », « Erreurs à éviter » ;
- démonstration accessible en un appui pendant la séance ;
- vue plein écran avec étape précédente/suivante, lecture lente, pause, répétition et consignes ;
- le chronomètre reste dans le ViewModel et continue pendant l'ouverture du média ;
- préchargement limité aux ressources de la séance active.

## Accessibilité

Chaque média possède une description complète : position corporelle, déplacement, appuis, amplitude et précaution. Les flèches ne sont jamais le seul moyen de comprendre la séquence. Les exercices de Sonia affichent les précautions d'épaule avant la démonstration.

## Provenance

Les planches du premier lot seront générées spécifiquement pour Projet Renaissance puis stockées localement. `EXERCISE_MEDIA_SOURCES.md` consignera pour chaque fichier : méthode de création, date, absence d'URL externe, modifications et description. Aucun média externe ne sera ajouté sans licence et vérification éditoriale documentées.

## Tests

- média présent et absent ;
- ressource locale valide ;
- séquence trois étapes ;
- navigation/pause/répétition ;
- média non chargé sans bloquer la séance ;
- URL externe absente/invalide ;
- description d'accessibilité non vide ;
- capture Pixel 8 de la carte, fiche et séquence.
