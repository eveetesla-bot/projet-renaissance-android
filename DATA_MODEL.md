# Modèle de données — Projet Renaissance

## Version locale 4

La migration Room 3 → 4 ajoute `sessionId` à chaque série et la table
`workout_sessions`. Celle-ci conserve l’identifiant persistant, le profil, le
modèle et le format de séance, l’heure de début, l’exercice courant, la note, la
douleur, la fin du repos et le statut.

Le redémarrage supprime les `set_logs` du même `sessionId` et remet la ligne de
séance à zéro dans une transaction unique. Une fermeture/réouverture retrouve
donc la même session au lieu de créer une fenêtre temporelle approximative.

## Conventions

- Identifiants : chaînes UUID, sauf contenu éditorial initial dont les identifiants sont stables et lisibles.
- Dates : `Instant` sérialisé en epoch millisecondes ; jours civils en ISO-8601.
- Toutes les unités sont explicites (`weightKg`, `durationSeconds`, `circumferenceCm`).
- Les données éditoriales et les données saisies sont séparées.

## Agrégats

### Profil

`ProfileEntity`

| Champ | Type | Rôle |
|---|---|---|
| `id` | String | `gerard` ou `sonia` |
| `displayName` | String | Nom affiché |
| `birthYear` | Int | Repère d'âge, modifiable |
| `goalSummary` | String | Objectif éditorial |
| `targetWeightMinKg` / `MaxKg` | Double? | Objectif de Gérard |
| `healthNotes` | String | Contraintes visibles |
| `dairyProteinAllergy` | Boolean | Règle forte Gérard |
| `rightShoulderCapsulitis` | Boolean | Règle forte Sonia |
| `migraineProne` | Boolean | Adaptation Sonia |

`UserPreferences` (DataStore) : `activeProfileId`, repos par défaut, son, vibration, `soniaMedicalClearance`, masquage des exercices non validés, thème.

### Programme éditorial

`ProgramWeekEntity` : numéro 1–12, phase, résumé, objectif de charge.

Phases : `ANCHOR` (1–3), `BUILD` (4–6), `PROGRESS` (7–9), `CONSOLIDATE` (10–12).

`WorkoutTemplateEntity` : profil, semaine de début/fin, code A/B/C, titre, intention, ordre.

`WorkoutExerciseEntity` : jointure ordonnée entre séance et exercice, séries, répétitions min/max, durée éventuelle, tempo, RPE cible, repos, alternative et notes de phase.

`DurationVariantEntity` : séance, variante `FULL`, `MIN_30`, `MIN_20`, `MINIMAL`, liste/limite d'exercices et ajustement de séries. La variante minimale dure 10 à 15 minutes.

### Bibliothèque

`ExerciseEntity`

| Groupe | Champs principaux |
|---|---|
| Identité | `id`, `name`, `description`, `imageAsset` |
| Ciblage | muscles, machine, niveau, profils |
| Technique | réglage, exécution, respiration, tempo suggéré |
| Sécurité | erreurs, alternative, sollicitation épaule, statut médical |
| Média | `verifiedVideoUrl` nullable, `videoVerifiedAt` nullable |

Enums : `ShoulderLoad = NONE, LOW, MODERATE, OVERHEAD` et `MedicalStatus = ALLOWED, REQUIRES_CLEARANCE, PRESCRIBED_ONLY`.

### Séance réalisée

`WorkoutSessionEntity` : profil, modèle source, variante, début/fin, état, énergie, sommeil, humeur, douleur maximale et notes.

`ExerciseLogEntity` : séance, exercice, ordre, réglage de machine, notes et état douleur.

`SetLogEntity` : exercice réalisé, numéro de série, répétitions, charge kg, RPE, tempo, début/fin, terminé.

`PainEventEntity` : séance/exercice/série, zone, intensité 0–10, type de gêne, action choisie, commentaire et date.

### Mesures et bien-être

`BodyMetricEntity` : profil, date, poids, taille, poitrine, taille abdominale, hanches, cuisse, bras et commentaire.

`DailyCheckInEntity` : profil, date, énergie 1–5, sommeil 1–5, heures de sommeil, humeur 1–5, douleur 0–10, migraine, épaule droite 0–10, hydratation et notes.

### Nutrition

`NutritionGuideEntity` : profil ou commun, catégorie, titre, contenu, ordre et avertissement.

`RecipeEntity` + `RecipeIngredientEntity` : profils compatibles, allergènes, ingrédients, étapes et protéines estimées.

`ShoppingListEntity` + `ShoppingItemEntity` : période, quantité, unité, rayon et état coché.

`ProteinLogEntity` : profil, date, repas, grammes estimés et source.

## Relations

```text
Profile 1─N WorkoutTemplate 1─N WorkoutExercise N─1 Exercise
Profile 1─N WorkoutSession 1─N ExerciseLog 1─N SetLog
WorkoutSession 1─N PainEvent
Profile 1─N BodyMetric
Profile 1─N DailyCheckIn
Profile 1─N ProteinLog
ShoppingList 1─N ShoppingItem
Recipe 1─N RecipeIngredient
```

## Index et contraintes

- Index uniques sur `(profileId, date)` pour `DailyCheckIn` et sur `(templateId, exerciseId, position)` pour le programme.
- Index temporels sur les historiques et mesures.
- Suppression en cascade des journaux enfants d'une séance ; jamais de cascade depuis le contenu éditorial vers l'historique utilisateur.
- Une URL vidéo vide ou non vérifiée est stockée à `null`.
- Une recette incompatible avec l'allergie de Gérard n'est jamais renvoyée par le dépôt de nutrition Gérard.

## Export JSON

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-07-15T12:00:00Z",
  "profiles": [],
  "sessions": [],
  "bodyMetrics": [],
  "dailyCheckIns": [],
  "proteinLogs": [],
  "preferences": {}
}
```

Le contenu éditorial initial n'est pas dupliqué dans l'export : les journaux référencent ses identifiants stables. L'import valide la version, les profils, les plages numériques et les références avant une transaction atomique.
