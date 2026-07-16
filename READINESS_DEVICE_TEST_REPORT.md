# Rapport appareil réel — Daily Readiness, suivi et médias

Date : 15 juillet 2026  
Appareil : Google Pixel 8 (`shiba`, série ADB `3A240DLJH001ZG`)  
Application : Projet Renaissance 0.1.0 debug  
APK SHA-256 : `CC6B9DF0695646C3D8131F07D2B25BF0BCD4AB14C2489D1D6652519B8BCFE69A`

## Résultat

Le lot est compilé, testé, installé et parcouru sur le Pixel 8. Aucun crash ou `FATAL EXCEPTION` Projet Renaissance n’a été trouvé dans les journaux contrôlés.

## Préparation quotidienne réelle

- score affiché avant HRV : 78/100, préparation correcte, confiance 48 % ;
- sommeil agrégé réel : 4 h 31, source Withings ;
- charge récente : sous-score 95/100 ;
- pas du jour observés : 830, source Withings ;
- FC au repos : 63 bpm, source Google Fit, explicitement signalée ancienne et exclue du calcul quotidien ;
- poids absent : affichage « Indisponible », aucune valeur de démonstration ;
- après autorisation HRV : 16/16 types Health Connect autorisés ;
- synchronisation HRV : 2 355 enregistrements importés, 0 supprimé ;
- source RMSSD vérifiée dans l’historique : Withings, exemples 41,0 ms et 30,0 ms ;
- score recalculé avec facteur « VFC RMSSD comparée à la moyenne personnelle » ; confiance observée 66 %.

## Sources Health Connect

- Withings : sommeil, activité, fréquence cardiaque et RMSSD détectés ;
- Google Fit : activité et FC au repos détectées ;
- Basic-Fit : application détectée précédemment, aucun enregistrement Health Connect attribuable ;
- total local après HRV : 43 027 enregistrements, dont 13 142 doublons exclus des totaux ;
- la HRV explique exactement l’augmentation de 40 672 à 43 027 enregistrements.

## Graphiques

Contrôlés sur l’écran Suivi :

- poids sans donnée : carte compacte et message explicite ;
- sommeil : courbe réelle des relevés disponibles ;
- FC au repos : courbe réelle ;
- cartes suivantes alimentées uniquement par leurs données Health Connect ou locales ; absence signalée sans série fictive.

## Médias d’exercices

- catalogue : 12 exercices requis, 3 positions vectorielles locales par exercice ;
- fiche testée sur appareil : Développé poitrine machine ;
- image 1 → image 2 : OK ;
- lecture lente et répétition : OK ;
- pause : OK ;
- réglage, erreurs et alternative : visibles ;
- description d’accessibilité : détectée dans l’arbre UI ;
- fonctionnement hors connexion : aucune URL ni ressource distante ;
- le minuteur reste porté par le ViewModel et n’est pas détruit par la navigation vers la démonstration.

## Tests automatisés

- 36 tests unitaires, 0 échec, 0 ignoré ;
- `testDebugUnitTest assembleDebug` : `BUILD SUCCESSFUL` en 2 min 21 s pour la version finale ;
- migration Room 1→2 exécutée directement par AndroidJUnitRunner sur le Pixel 8 : `OK (1 test)`, 1,651 s ;
- le lanceur Gradle `connectedDebugAndroidTest` n’a pas pu télécharger un composant UTP non présent dans le cache, l’accès réseau étant refusé ; le même APK de test compilé a été installé et exécuté directement avec succès.

## Captures

- `outputs/pixel8-readiness.png`
- `outputs/pixel8-tracking.png`
- `outputs/pixel8-exercise-library.png`
- `outputs/pixel8-exercise-demo.png`

## Limites restantes

- les illustrations sont des schémas vectoriels pédagogiques, pas des vidéos biomécaniques ;
- une démonstration a été parcourue entièrement sur appareil ; les 12 entrées et l’absence d’URL inventée sont couvertes par test unitaire ;
- les graphiques affichent les relevés disponibles ; l’agrégation calendaire 7/30 jours avancée pourra encore être enrichie ;
- aucune conclusion médicale n’est produite par le score.
