# Projet Renaissance — V3 (0.3.0)

Branche `v2` du dépôt `eveetesla-bot/projet-renaissance-android`,
au-dessus de la V2 (0.2.0). Sept évolutions majeures.

## Coach adaptatif intelligent

- Première charge estimée à partir des **mesures réelles Withings**
  (masse maigre en priorité, sinon poids + masse grasse, sinon poids
  mesuré), du sexe et de l'âge. Le poids cible du profil ne sert que de
  repli signalé et minoré ; sans aucune donnée, aucune charge n'est
  inventée.
- **Index de performance personnel** : les séries réellement validées
  situent l'utilisateur par rapport aux références statistiques, et ce
  recalage se transfère prudemment aux exercices jamais pratiqués.
- Adaptation quotidienne (sommeil, FC repos, VFC, douleur) : charges et
  répétitions modulées avant chaque exercice.
- Progression selon l'objectif du programme : montée de charge pour la
  masse sèche (Gérard), progression par répétitions pour tonicité et
  mobilité (Sonia). Tout reste modifiable, rien n'est imposé.

## Alignement sur le livre (édition premium)

- Programme refondu en **trois phases de quatre semaines** (1–4 Ancrer,
  5–8 Construire, 9–12 Intensifier).
- Gérard : 9 séances distinctes reprenant les tableaux exacts du livre
  (séries, répétitions, repos, tempo, RPE) — plus aucune dérivation
  mécanique des phases.
- Sonia : séances identiques toutes phases (progression par
  répétitions) ; rowing poitrine appuyée et chest press prise neutre
  (RPE 4–6) intégrés et proposés uniquement après validation médicale.
- Migration propre des installations existantes (gabarits réécrits,
  journaux d'entraînement conservés).

## Refonte de l'interface

- Identité « salle de sport » : anthracite + orange énergie + sarcelle
  santé, typo sans-serif grasse, cartes plates à filet fin, en-têtes
  héro, icônes d'onglets sportives.
- Accueil réorganisé : carte héro « Séance du jour » avec bouton
  DÉMARRER, tuiles de stats, raccourcis.
- Contraste garanti par construction (couleur de texte liée au fond) ;
  barres système en icônes sombres (heure lisible sur fond clair).

## Cycle de séance explicite

- Pause claire (reprise possible toute la journée), TERMINER LA SÉANCE
  au dernier exercice ou fin anticipée, écran récapitulatif, indication
  « séance enregistrée aujourd'hui » sur l'accueil.

## Illustrations personnalisées

- 32 cartes WebP du pack éditorial (3:2, plein format + miniature),
  variante par profil (tatouage de Gérard, épaule neutre de Sonia).
- Les 12 exercices ajoutés en V2 ont enfin leur visuel et apparaissent
  dans la bibliothèque.
- Vue machine sans doublon : emplacement pour photographier l'appareil
  réel de sa salle.

## Logo

- Icône adaptative vectorielle (soleil levant + barre d'haltère sur
  anthracite), variante monochrome Android 13+.

## Vérification

`./gradlew test assembleDebug` : 62 tests unitaires verts (dont 19 pour
le coach adaptatif). Validé sur Pixel 8 (installation, lancement, seed
des nouveaux gabarits contrôlé en base).
