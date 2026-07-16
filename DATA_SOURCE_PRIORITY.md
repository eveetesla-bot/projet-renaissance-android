# Priorité des sources — Projet Renaissance

## Objectif

La priorité choisit la donnée affichée ou comptée lorsqu'au moins deux sources décrivent le même phénomène. Elle ne modifie ni ne supprime les données originales de Health Connect.

Health Connect sait déjà dédupliquer certaines agrégations d'activité et de sommeil selon l'ordre défini par l'utilisateur dans Health Connect. Projet Renaissance utilise ces agrégations pour les totaux simples quand elles conviennent, et conserve sa propre sélection explicable pour l'historique détaillé.

## Valeurs initiales

Les préférences sont locales à l'installation et modifiables.

| Domaine | Priorité initiale |
|---|---|
| Pas | Withings, puis source Health Connect agrégée/appareil, puis Google Fit, puis autre |
| Distance | Withings, puis Google Fit, puis autre |
| Fréquence cardiaque | Withings/appareil mesuré, puis autre source mesurée |
| Sommeil | Withings, puis autre |
| Poids et composition | Withings, puis Projet Renaissance, puis autre |
| Séances Basic-Fit | Basic-Fit si un record source existe réellement |
| Séances Projet Renaissance | Projet Renaissance pour une séance guidée terminée dans l'application |
| Hydratation | Projet Renaissance, puis autre |

Une marque absente des métadonnées n'est jamais créée artificiellement dans cette liste. L'interface montre uniquement les sources détectées, plus « Automatique ».

## Critères de qualité avant la marque

Pour départager deux records équivalents :

1. record explicitement mesuré par une montre ou un capteur ;
2. source prioritaire choisie pour ce type ;
3. méthode d'enregistrement automatique mesurée plutôt que saisie manuelle ;
4. record le plus récemment modifié ;
5. identifiant Health Connect comme dernier ordre stable.

La priorité de marque ne transforme pas une estimation en mesure. Pour la fréquence cardiaque, un record mesuré gagne toujours sur une valeur calculée ou inconnue.

## Portée

- La configuration est stockée par profil local.
- Un changement de profil ne réutilise pas silencieusement la configuration de l'autre profil.
- Le changement de priorité relance le calcul des indicateurs locaux sans réimporter Health Connect.
- Les données non retenues restent visibles dans le diagnostic et peuvent être affichées avec le filtre « toutes les sources ».

## Interface

Chemin : `Paramètres > Santé et synchronisation > Sources prioritaires`.

Chaque ligne présente un type, la source sélectionnée, les sources détectées et une option « Automatique Health Connect ». Une explication précise que la priorité de Projet Renaissance concerne son affichage local et ne change pas nécessairement l'ordre configuré dans Health Connect.

## Cas particuliers

### Pas

Les pas couvrant des intervalles qui se chevauchent ne sont jamais additionnés source par source. Pour le total journalier, l'agrégation Health Connect sans filtre de source est préférée lorsqu'elle est disponible, car elle applique l'ordre de priorité Health Connect aux données d'activité. Pour une ventilation locale, Projet Renaissance découpe la journée en fenêtres temporelles et ne garde qu'une source gagnante par fenêtre.

### Séances

Deux séances sont candidates au doublon si leur type est compatible, si elles se chevauchent fortement et si leurs débuts sont proches. La séance issue d'une montre mesurée est prioritaire ; ensuite vient la préférence utilisateur. Une séance guidée Renaissance conserve ses séries et notes locales même si une séance externe est retenue pour les calories ou la fréquence cardiaque.

### Basic-Fit

Basic-Fit ne figure dans un choix que si au moins un record porte réellement son package source ou son libellé résolu. « Basic-Fit indisponible » signifie « aucune donnée attribuable détectée dans la période et avec les permissions actuelles », pas « l'application Basic-Fit ne possède aucune donnée ».

## Tests attendus

- Withings et Google Fit couvrent les mêmes pas : une seule source contribue au total local.
- Changement de priorité : le gagnant change, pas le nombre de records bruts.
- Source prioritaire absente : repli vers la meilleure source disponible.
- Basic-Fit absent : aucune donnée n'est étiquetée Basic-Fit.
- Mesure de montre face à une saisie manuelle : la mesure gagne indépendamment de la marque.
