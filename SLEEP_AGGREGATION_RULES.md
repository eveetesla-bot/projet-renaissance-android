# Agrégation du sommeil — Règles

## Objectif

Construire la dernière nuit principale à partir de `SleepSessionRecord` réels sans confondre dernier record, sieste, fragment ou doublon inter-source.

## Entrées conservées

Chaque session possède : identifiant Health Connect, profil, début, fin, source/package, méthode de mesure, appareil, fraîcheur et statut de priorité. Une durée négative, nulle ou supérieure à 24 h est rejetée comme invalide.

## Algorithme

1. Convertir les instants dans le fuseau local courant du profil/appareil.
2. Dédupliquer les sessions inter-sources : recouvrement ≥70 % ou débuts/fins à ±30 min. Appliquer la source prioritaire, puis appareil mesuré, puis modification la plus récente.
3. Grouper par source les fragments séparés de 90 minutes maximum.
4. Fusionner les intervalles qui se chevauchent ; ne jamais additionner deux minutes couvertes deux fois.
5. Former des épisodes candidats sur une fenêtre allant de 18 h la veille à 12 h le jour du réveil.
6. Classer comme nuit principale l'épisode ayant la plus grande durée nocturne et dont le centre tombe entre 22 h et 8 h. Une durée totale ≥3 h est complète ; en dessous, l'épisode est partiel et signalé.
7. Choisir la nuit complète la plus récente dont le réveil est antérieur au calcul. Ne pas prendre mécaniquement le record le plus récent.
8. Les épisodes diurnes de 20 min à moins de 3 h, principalement entre 9 h et 20 h, sont des siestes et sont affichés séparément.

Une nuit traversant minuit conserve un seul épisode. Les fragments 22:40–01:30 et 02:05–06:40 sont fusionnés ; la pause de 35 minutes est conservée comme éveil et n'est pas comptée dans la durée dormie.

## Durées

- `elapsedWindow` : réveil − endormissement ;
- `sleepDuration` : union des segments, hors trous ;
- `awakeGapDuration` : trous entre fragments ;
- `napDuration` : union des épisodes classés siestes.

Le tableau de bord utilise `sleepDuration`, pas `elapsedWindow`.

## Moyenne et régularité

- moyenne personnelle : 14 nuits valides, minimum 4 ;
- une seule nuit par date de réveil ;
- médiane utilisée pour limiter l'effet d'une nuit extrême ;
- régularité : écart circulaire des heures d'endormissement et de réveil par rapport à la médiane ;
- une donnée vieille de plus de 36 h est affichée « ancienne » et exclue du score quotidien ;
- une synchronisation partielle est signalée si un segment se termine près du présent ou si les changements Health Connect ne sont pas terminés.

## Sources contradictoires

Withings et Google Fit ne sont jamais additionnés pour la même nuit. La source prioritaire gagne sur la zone en conflit. Une source secondaire peut compléter une portion non couverte uniquement si cette politique est explicitement activée ; le premier lot conserve une seule source gagnante par épisode pour rester explicable.

## Sortie

```text
AggregatedSleepNight
├── bedtime / wakeTime
├── sleepDurationMinutes
├── elapsedWindowMinutes
├── awakeGapMinutes
├── sourceLabel / sourcePackage
├── segmentIds[]
├── quality: COMPLETE | PARTIAL | STALE
├── confidence
└── naps[]
```

Sans nuit valide, la sortie est absente et l'interface affiche « Sommeil indisponible ».

## Tests

Nuit complète, nuit fractionnée, sieste, doublon Withings/Google Fit, sources contradictoires, absence, synchronisation partielle, donnée ancienne, passage de minuit, fragments chevauchants et changement de priorité.
