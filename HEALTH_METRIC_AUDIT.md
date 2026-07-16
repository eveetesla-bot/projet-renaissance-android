# Audit des mesures affichées

## Convention d'affichage

Toute mesure utilise `MeasuredValueCard` avec valeur, unité, source, date, statut `MEASURED | MANUAL | ESTIMATED`, confiance, fraîcheur et synchronisation. Une absence devient « Donnée indisponible ». Une estimation est explicitement nommée et n'est jamais présentée comme mesurée.

## Audit actuel

| Mesure | État constaté | Correction requise |
|---|---|---|
| Énergie/sommeil 3/5 | curseurs initialisés à 3 et affichés comme valeurs | aucun défaut considéré saisi ; afficher dernière réponse manuelle ou indisponible |
| Sommeil 4,5 h | dernier `SleepSessionRecord` | agréger la dernière nuit complète et séparer les siestes |
| Pas | dernier intervalle, pas total du jour | agréger le jour avec déduplication/source |
| FC repos | dernière valeur réelle | ajouter source, fraîcheur, moyenne 14 jours et tendance |
| Poids | indisponible si absent | conserver ; retirer les mesures de démonstration historiques |
| Courbe de poids | données `DemoData.metrics` fictives | ne plus semer ces données, supprimer les identifiants de démonstration connus |
| Progression semaine | texte fixe `0 / 3` | calculer depuis les séances locales terminées ; sinon « aucune séance enregistrée » |
| Séance suivante | premier modèle du programme | calculer depuis semaine/séances terminées ; tant que non implémenté, étiqueter « programme proposé » |
| Volume entraînement | absent | calculer uniquement depuis séries terminées (`charge × répétitions`) et afficher unité kg·rep |
| Calories | diagnostic réel Health Connect | ajouter agrégation par période, source et statut mesuré/estimé |
| Récupération | absente | utiliser `DailyReadinessEngine`, jamais une valeur fixe |
| Objectifs | contenu éditorial | conserver comme objectif, sans le confondre avec une mesure |
| Graphiques | courbe démo/partielle | alimenter uniquement Room/Health Connect et montrer les jours manquants |

## Mesures réelles disponibles sur le Pixel 8 de Gérard

Sources confirmées lors du test : Withings et Google Fit. Basic-Fit installé mais sans record attribuable. Types actuellement présents : pas, distance, calories actives/totales, fréquence cardiaque, FC au repos, sommeil, vitesse, étages et séances. Poids, composition, hydratation et VO₂ max sont autorisés mais absents de la fenêtre testée.

## Composant `MeasuredValueCard`

```text
MeasuredValue
├── label
├── value: Double/String?       null = indisponible
├── unit
├── sourceLabel / sourcePackage
├── measuredAt
├── status
├── confidence
├── freshness
├── syncState
└── explanation
```

Fraîcheur configurable par type : pas/calories 6 h, cardio repos/sommeil 36 h, poids 30 jours, déclaration manuelle 36 h. L'interface n'utilise pas la couleur seule pour communiquer qualité ou ancienneté.

## Graphiques à produire

- sommeil 7/30 jours : durée nocturne agrégée, nuits absentes visibles ;
- FC repos : valeur quotidienne, moyenne 14 jours ;
- poids : aucune courbe si moins de deux valeurs réelles ;
- pas : total quotidien dédupliqué ;
- séances : durée/nombre, source ;
- volume : local uniquement ;
- préparation : score et confiance ;
- douleur/énergie : saisies manuelles datées.

Chaque graphique indique période, unité, moyenne, tendance, sources et données manquantes. Aucun point de démonstration n'est injecté.

## Plan de nettoyage

1. arrêter le semis de `g1`, `g2`, `g3`, `s1`, `s2`, `s3` ;
2. supprimer uniquement ces identifiants connus, sans toucher aux mesures utilisateur ;
3. rendre les champs manuels nullable/non saisis dans l'UI ;
4. introduire agrégateurs quotidiens testables ;
5. remplacer progressivement les cartes existantes par `MeasuredValueCard` ;
6. vérifier chaque écran sur données absentes et données anciennes.
