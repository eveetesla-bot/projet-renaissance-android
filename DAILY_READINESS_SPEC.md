# Daily Readiness — Spécification

## Objet et limites

`DailyReadinessEngine` calcule localement un indicateur de préparation à l'entraînement. Il ne produit ni diagnostic, ni aptitude médicale, ni décision automatique. Le résultat aide l'utilisateur à choisir ; les règles de sécurité, la douleur et les restrictions médicales restent prioritaires.

Le moteur n'invente aucune valeur. Une composante sans donnée exploitable est absente du calcul et son poids est redistribué proportionnellement entre les composantes disponibles. Sans composante disponible, le score est `null` et l'interface affiche « Données insuffisantes ».

## Sortie

```text
DailyReadinessResult
├── score: Int?                  0..100 ou null
├── classification
├── recommendation
├── confidence: Int              0..100
├── calculatedAt
├── factors[]
└── missingInputs[]
```

Classifications :

| Score | Classe affichée |
|---:|---|
| 80–100 | Bonne préparation |
| 60–79 | Préparation correcte |
| 40–59 | Récupération partielle |
| 0–39 | Récupération faible |
| absent | Données insuffisantes |

## Pondération configurable

```kotlin
ReadinessWeights(
    sleep = 0.35,
    cardiovascular = 0.20,
    recentLoad = 0.20,
    comfort = 0.15,
    subjective = 0.10,
)
```

La somme doit valoir 1. Chaque composante produit un sous-score 0–100, une qualité, une fraîcheur, une source et une explication. Le score final est la moyenne pondérée des seules composantes disponibles.

## Composante sommeil — 35 %

Entrées : dernière nuit agrégée, moyenne personnelle glissante sur 14 jours, heures d'endormissement/réveil et régularité sur 14 jours.

- durée / moyenne personnelle ≥ 100 % : 95 à 100 ;
- 85–99 % : interpolation 75 à 95 ;
- 70–84 % : interpolation 50 à 75 ;
- 50–69 % : interpolation 20 à 50 ;
- < 50 % : 0 à 20.

La régularité ajuste au maximum ±10 points : écart médian de coucher et lever ≤30 min = +5/+5 ; 30–60 min = 0 ; 60–120 min = −5/−5 ; au-delà = −10/−10, avec bornage final 0–100. Une nuit partielle ne reçoit pas de bonus de régularité et réduit la confiance.

La moyenne personnelle exige au moins 4 nuits valides. Sinon, la durée actuelle est décrite mais aucun écart à une moyenne supposée n'est inventé ; le sous-score peut utiliser des repères de durée configurables pour adulte, explicitement étiquetés « repère général » et avec confiance réduite.

## Récupération cardiovasculaire — 20 %

Entrées optionnelles : fréquence cardiaque au repos du jour, moyenne personnelle 14/28 jours, fréquence cardiaque moyenne récente et RMSSD Health Connect si disponible.

Pour la FC au repos, l'écart relatif à la moyenne personnelle est évalué :

- ≤3 % : 100 ;
- 3–8 % au-dessus : interpolation 100→70 ;
- 8–15 % au-dessus : interpolation 70→30 ;
- >15 % au-dessus : 10 ;
- une valeur légèrement inférieure à la moyenne n'est pas automatiquement considérée meilleure et reste bornée à 100.

Une seule mesure inhabituelle ne peut retirer plus de 14 points au score final. La RMSSD n'est utilisée qu'avec une moyenne personnelle suffisante et une méthode/source cohérente. Les données cardio vieilles de plus de 36 heures sont signalées comme anciennes et exclues du score quotidien.

## Charge récente — 20 %

Fenêtres : 24 h et 72 h. Entrées réelles uniquement : durée des séances Health Connect, séances Projet Renaissance, charge interne `durée × RPE` lorsqu'elle existe, pas et activité.

- aucune activité détectée n'est pas interprétée comme repos si les permissions/données sont incomplètes ;
- charge habituelle : 85–100 ;
- charge 120–150 % de la moyenne personnelle : 60–85 ;
- charge >150 % : 30–60 ;
- séance lourde la veille : réduction plafonnée à 12 points sur le score final ;
- activité très faible ne pénalise pas automatiquement la préparation.

La référence personnelle exige au moins 7 jours. Sans référence, la composante est descriptive ou de confiance réduite.

## Douleur et confort — 15 %

Dernière déclaration locale datant de moins de 36 h :

| Douleur /10 | Sous-score |
|---:|---:|
| 0 | 100 |
| 1–3 | 80 |
| 4–6 | 50 |
| 7–8 | 20 |
| 9–10 | 0 |

Pour Sonia, une douleur d'épaule reste un signal de sécurité distinct et ne peut jamais réactiver un exercice interdit. Une migraine déclarée est affichée comme facteur, sans interprétation médicale.

## Ressenti manuel — 10 %

Énergie, fatigue et humeur sont optionnelles. Les valeurs 1–5 deviennent respectivement 0, 25, 50, 75, 100. La fatigue est inversée. La moyenne porte uniquement sur les réponses réellement saisies. Les curseurs ne possèdent aucune valeur par défaut considérée comme saisie.

## Fraîcheur et confiance

La confiance combine couverture des poids et qualité/fraîcheur :

```text
confidence = somme(poids disponibles × qualité) / somme(poids configurés)
```

Qualité indicative : record mesuré et récent 1,0 ; saisie manuelle récente 0,9 ; agrégat multi-segments 0,85 ; donnée partielle 0,6 ; donnée ancienne exclue 0.

Chaque facteur expose : valeur actuelle, moyenne personnelle éventuelle, période, tendance, source, horodatage, qualité, fraîcheur et contribution au score. L'impact est calculé par rapport à un niveau neutre configurable de 70 : `poids effectif × (sousScore − 70) / 100 × 100` points.

## Conseil de séance

| Situation | Conseil non médical |
|---|---|
| score ≥80 | Séance normale |
| 60–79 | Séance normale, auto-évaluation à l'échauffement |
| 40–59 | Séance courte ou charge réduite de 5 à 10 % |
| <40 | Mobilité, marche ou repos et réévaluation |
| douleur ≥7 | Réduire/adapter et demander conseil si nécessaire, indépendamment du score |

Le moteur ne supprime, ne reporte et ne démarre aucune séance. L'utilisateur décide toujours.

## Tests obligatoires

Score complet, données partielles, aucune donnée, sommeil très faible, FC inhabituelle, charge forte, douleur, donnée ancienne, source prioritaire, doublon, valeur manuelle et stabilité des seuils 39/40/59/60/79/80.
