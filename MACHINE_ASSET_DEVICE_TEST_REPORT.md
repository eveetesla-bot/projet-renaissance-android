# Rapport Pixel 8 — illustrations machines originales

Date : 16 juillet 2026  
Appareil : Google Pixel 8 (`3A240DLJH001ZG`)  
Application : Projet Renaissance 0.1.0 debug

## Résultat

Les douze illustrations originales sont intégrées et lisibles sur le Pixel 8.
Aucune photo, vidéo, marque, logo ou modèle exact Basic-Fit n’a été utilisé.

## Couverture

| exerciseId | SVG | PNG Android | Bibliothèque | Séance | Plein écran |
|---|---:|---:|---:|---:|---:|
| `bike` | OK | OK | OK | OK | OK |
| `leg_press` | OK | OK | OK | OK | OK |
| `chest_press` | OK | OK | OK | OK | OK |
| `seated_row` | OK | OK | OK | OK | OK |
| `leg_curl` | OK | OK | OK | OK | OK |
| `lateral_raise` | OK | OK | OK | OK | OK |
| `calf_press` | OK | OK | OK | OK | OK |
| `hip_thrust` | OK | OK | OK | OK | OK |
| `leg_extension` | OK | OK | OK | OK | OK |
| `abductors` | OK | OK | OK | OK | OK |
| `dead_bug` | OK | OK | OK | OK | OK |
| `reverse_crunch` | OK | OK | OK | OK | OK |

## Contrôles automatisés

- les 12 ressources sont présentes dans le catalogue ;
- chaque nom logique correspond à un drawable Android ;
- chaque PNG est décodable sur le Pixel 8 ;
- dimensions vérifiées : 1200 × 800 ;
- ratio commun : 3:2 ;
- bouton vidéo toujours désactivé sans URL éditoriale validée ;
- 53 tests unitaires réussis ;
- 18 tests instrumentés réussis sur appareil.

## Contrôles visuels

- cartes de bibliothèque parcourues du premier au douzième exercice ;
- image non recadrée et sans débordement ;
- lisibilité des contours et des zones cuivre sur smartphone ;
- vue machine plein écran validée ;
- double aperçu mouvement + machine validé dans la séance ;
- cohérence bleu nuit, sauge, cuivre, beige et gris ;
- aucun plantage AndroidRuntime observé.

## Remplacement futur par une photo personnelle

`MachineMedia` conserve toujours l’identifiant de l’illustration. Le champ
`userPhotoUri` est prioritaire dans `MachineAssetImage` lorsqu’une photo locale
est fournie et décodable. Si la photo manque ou devient inaccessible,
l’illustration originale réapparaît automatiquement.

## Captures

- `outputs/machine-assets-contact-sheet.png`
- `outputs/machine-library-1-pixel8.png` à
  `outputs/machine-library-6-pixel8.png`
- `outputs/machine-fullscreen-pixel8.png`
- `outputs/machine-workout-preview-pixel8.png`

## APK

`outputs/Projet-Renaissance-0.1.0-machine-assets-debug.apk`  
Taille : 13 002 744 octets  
SHA-256 : `15665D54A25997B77C99F304977030B6ED15373D5F9D553E51C4091209D9C10B`
