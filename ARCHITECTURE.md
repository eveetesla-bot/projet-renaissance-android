# Architecture — Projet Renaissance

## Évolutions du lot médias et réinitialisation

- Le catalogue média distingue illustration guidée, photo machine, vidéo
  externe vérifiée et fiche livre.
- Room version 4 persiste l’identité et l’état minimal de la séance ouverte.
- Les trois niveaux de remise à zéro passent par le ViewModel et des
  transactions du dépôt.
- DataStore conserve l’état d’onboarding et l’heure de la dernière
  synchronisation réussie.
- Health Connect reste la source système ; ses mesures ne sont jamais
  supprimées par l’application.

## Objectifs

Application Android locale, utilisable rapidement en salle, avec de grandes cibles tactiles, un parcours de séance résilient aux interruptions et une séparation nette entre contenu éditorial, historique utilisateur et préférences.

## Choix structurants

- Kotlin, Jetpack Compose et Material 3.
- Une application mono-module pour la V1 afin de limiter la complexité de compilation, organisée en couches Clean Architecture.
- MVVM avec flux unidirectionnel : `UiState` immutable, événements utilisateur, ViewModel, cas d'usage, dépôts.
- Room comme source de vérité locale pour profils, programme, séances et mesures.
- DataStore Preferences pour le profil actif, le son, la vibration, le repos par défaut et le filtre médical Sonia.
- Navigation Compose avec routes typées centralisées.
- Coroutines et `Flow` pour la réactivité hors connexion.
- Aucun WorkManager en V1 : le chronomètre repose sur une échéance absolue et peut être recalculé après recréation. WorkManager ne garantirait pas un compte à rebours précis.
- Aucun client HTTP. Les liens vidéo ne sont affichés que lorsqu'une URL éditoriale vérifiée existe.

## Couches

```text
UI Compose
  -> ViewModels / UiState
    -> cas d'usage (sélection du profil, prochaine séance, terminer une série)
      -> interfaces de dépôts
        -> Room / DataStore / import-export JSON
```

### Présentation

`presentation/` contient le thème, la navigation, les écrans, composants et ViewModels. Les écrans ne connaissent ni DAO ni entités Room.

### Domaine

`domain/model/` contient les modèles métier sans annotation Android. `domain/repository/` définit les contrats. Les règles non négociables y vivent : allergènes de Gérard, visibilité médicale de Sonia et validation d'une série.

### Données

`data/local/` contient Room, entités, DAO et données initiales. `data/preferences/` contient DataStore. `data/repository/` mappe entités et domaine. `data/transfer/` porte l'export/import versionné.

## Arborescence des écrans

```text
Sélection du profil (premier lancement / changement)
└── Accueil
    ├── État de forme du jour
    ├── Séance suivante
    │   ├── Choix de durée : complète / 30 / 20 / minimale
    │   └── Séance en cours
    │       ├── Détail exercice
    │       ├── Série terminée -> Chronomètre de repos
    │       ├── Douleur ou gêne -> saisie + adaptation
    │       ├── Notes
    │       └── Résumé de séance
    ├── Programme
    │   ├── Phase (Ancrer / Construire / Progresser / Consolider)
    │   ├── Semaine 1…12
    │   └── Séance A / B / C
    ├── Suivi
    │   ├── Tableau de bord
    │   ├── Poids et mensurations
    │   ├── Charges, répétitions et RPE
    │   └── Énergie, sommeil, douleur et humeur
    ├── Nutrition
    │   ├── Repères du profil
    │   ├── Recettes
    │   ├── Liste de courses
    │   └── Suivi protéines
    ├── Bibliothèque
    │   ├── Recherche et filtres
    │   └── Fiche exercice
    ├── Chronomètre autonome
    └── Profil et préférences
        ├── Objectifs et santé
        ├── Historique
        ├── Réglages du chronomètre
        ├── Sécurité Sonia
        └── Export / import JSON
```

## Navigation V1

La barre principale expose Accueil, Programme, Suivi, Nutrition et Profil. Bibliothèque et chronomètre sont accessibles depuis l'accueil et les séances. La séance en cours est une destination plein écran afin d'éviter les erreurs tactiles.

## Sécurité

- Les exercices ont un niveau de sollicitation de l'épaule et un statut de validation médicale.
- Pour Sonia, la requête de programme exclut par défaut `OVERHEAD` et tout exercice `REQUIRES_CLEARANCE` tant que la préférence de validation n'est pas activée.
- Le badge visuel complète la règle métier ; il ne la remplace jamais.
- Une douleur enregistre l'événement, met la série en pause et propose l'alternative autorisée.
- Les textes médicaux sont informatifs et rappellent que l'application ne remplace pas un professionnel.
- Les suggestions nutritionnelles de Gérard passent par un filtre d'allergènes testé automatiquement.

## Hors connexion et future synchronisation

Room est la source de vérité. Chaque donnée mutable possède un UUID, `createdAt`, `updatedAt` et, lorsque nécessaire, `deletedAt`. Une future synchronisation pourra envoyer un journal de changements sans modifier l'UI ni le domaine. Les exports JSON contiennent `schemaVersion`, `exportedAt` et les collections, puis sont validés avant transaction d'import.

## Chronomètre

Le ViewModel conserve `endsAtEpochMillis`, pas seulement un compteur. L'affichage calcule le reste depuis l'horloge. Ajouter/retirer 15 secondes modifie l'échéance. Son et vibration sont optionnels. Une notification de premier plan n'est pas retenue pour la V1 initiale ; elle sera ajoutée si le compte à rebours doit continuer avec l'application totalement fermée.

## Graphiques

La V1 dessine les courbes simples avec `Canvas` Compose afin d'éviter une dépendance. Une bibliothèque dédiée ne sera introduite que si les besoins d'accessibilité, d'axes ou d'interactions dépassent cette implémentation.

## Tests

- Tests unitaires JVM des règles d'allergènes, du filtre Sonia, des variantes de durée et du calcul du chronomètre.
- Tests DAO instrumentés à ajouter lorsque l'environnement d'émulation est disponible.
- Test de compilation `assembleDebug` obligatoire avant de déclarer l'incrément compilable.
