# Projet Renaissance — V2 (0.2.0)

Base : dépôt `eveetesla-bot/projet-renaissance-android` (main, 4c80130).
L'original n'est pas modifié ; cette V2 est livrée comme sources + patch.

## Corrections de bugs

1. **`adjustTimer` (AppViewModel)** — Les boutons ±15 s modifiaient une
   variable locale sans mettre à jour l'état ni persister l'échéance en
   base. L'ajustement était perdu à la recréation de l'écran. Corrigé :
   mise à jour de `_workout` + `persistWorkoutSession`, garde si aucun
   chronomètre actif.

2. **Son et vibration de fin de repos** — Les réglages « Vibration du
   chronomètre » et « Signal sonore » existaient dans l'UI et DataStore
   mais n'étaient reliés à aucun comportement. Ajout de
   `signalTimerEnd()` déclenché quand le compte à rebours atteint zéro :
   vibration en trois impulsions (`VibrationEffect.createWaveform`,
   compatible API 26+ / API 31+) et tonalité de notification
   (`ToneGenerator`). Respecte les deux préférences ; aucun effet lors
   d'un arrêt manuel (`stopTimer` annule le job avant le signal).

## Fidélité au livre (chapitres 16 à 18)

3. **12 nouvelles fiches d'exercices** dans le seed : extension triceps
   à la corde, tirage vertical prise neutre, développé incliné machine,
   reverse fly machine, curl biceps à la poulie, rowing poitrine
   appuyée, développé épaules machine (OVERHEAD — jamais visible pour
   Sonia), extension lombaire à 45°, pont fessier au sol, step-up bas,
   adducteurs machine, respiration et bassin. Flags de sollicitation de
   l'épaule et validation médicale alignés sur les règles de sécurité.
   (13 → 25 exercices ; les 4 fiches restantes du livre — tractions
   assistées, goblet squat, kickback, marche inclinée, pallof press —
   sont des alternatives citées, à ajouter en V2.2.)

4. **Séances de Gérard conformes au livre** :
   - A : ajout de l'extension triceps (2×10–15, 75 s) → 7 exercices.
   - B : tirage vertical + développé incliné + reverse fly + curl
     (le plan précédent réutilisait rowing/développé de la séance A).
   - C : rowing poitrine appuyée, développé épaules, curl + triceps,
     extension lombaire → 8 exercices (au lieu de 4).

5. **Séances de Sonia conformes au livre (avant validation)** :
   - B : pont fessier au sol (au lieu de hip thrust machine par
     défaut), **adducteurs** (le plan précédent mettait des abducteurs
     par erreur), ajout du step-up bas.
   - C : pont fessier + bloc respiration/bassin (3×5 cycles, RPE 3).
   - A : déjà conforme, inchangée.

6. **Propagation sans réinstallation** — `seedIfNeeded` réécrit
   désormais le contenu éditorial (profils, exercices, plans) à chaque
   lancement (upsert idempotent). Les données utilisateur (journaux,
   mesures, sessions, santé) ne sont pas touchées.

7. **Filtre allergènes durci (Gérard, chapitre 14)** — Ajout de
   lactosérum, petit-lait, lactalbumine, lactoglobuline, protéines de
   lait / laitières, casein, milk protein, beurre, ghee, crème
   laitière. Rappel du livre : « sans lactose » ne garantit rien.

8. **Coach** — `noExternalLoadExercises` étendu aux nouveaux
   mouvements au poids du corps (pont fessier, step-up, respiration).

## Limitations connues (candidates V2.x)

- Les nouveaux exercices n'ont pas encore de visuels dans
  `ExerciseMediaCatalog` (l'UI masque la section proprement).
- Pas de notification de premier plan : le signal de fin de repos ne
  fonctionne que si l'application vit encore (écran séance ouvert ou
  app en arrière-plan récent).
- `GerardNutritionRules.isSafeSuggestion` n'est branché sur aucun flux
  UI : à connecter quand des suggestions dynamiques apparaîtront.
- Les phases 2 à 4 dérivent toujours des séances de phase 1
  (+1 série / RPE 8) au lieu des 9 séances distinctes du livre.
- Mémoire des réglages machine (siège, dossier, pile) non implémentée.
- Le poids Withings (Health Connect) n'alimente pas encore l'écran
  Suivi (`body_metrics`).

## Vérification

`./gradlew test assembleDebug` à lancer sur un poste avec le SDK
Android (non disponible dans l'environnement de préparation de ce
patch). Les tests unitaires existants restent compatibles :
`ExerciseMediaCatalogTest` porte sur le catalogue média (inchangé),
`RulesTest` reste vert avec la liste étendue.
