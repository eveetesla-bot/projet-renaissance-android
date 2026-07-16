# Rapport — Coach de séance et redémarrage

Date : 16 juillet 2026  
Appareil contrôlé : Google Pixel 8 de Gérard

## Résultat

La séance propose désormais un format, des répétitions ou une durée et une
charge modifiable avant le premier effort. L'illustration locale est ouverte par
défaut.

Un bouton unique « Recommencer la séance » ouvre une confirmation. Après
confirmation, seules les séries du profil et du modèle enregistrées depuis
l'ouverture de la séance courante sont supprimées. L'historique antérieur, les
autres séances et l'autre profil sont conservés.

## Logique du coach

### Format

Le format Complet, 30 minutes, 20 minutes ou 10–15 minutes dépend :

- du score Readiness ;
- de sa confiance ;
- d'une douleur récente déclarée.

### Charge

La charge machine n'est jamais inventée à partir du sexe ou de l'âge. Elle
repart de la dernière charge réelle du même exercice :

- maintien si récupération favorable et RPE maîtrisé ;
- réduction de 5 à 20 % si récupération moindre, douleur ou RPE élevé ;
- arrondi au demi-kilogramme ;
- calibration légère obligatoire en l'absence d'historique machine ;
- 0 kg explicite pour les mouvements sans charge externe.

L'application n'augmente pas automatiquement une charge. Une progression
nécessite une validation réelle du RPE et de la technique.

### Répétitions et durée

- la prescription du livre est analysée ;
- une séance complète utilise le milieu de la plage ;
- un format réduit utilise la borne basse ;
- un exercice minuté affiche « Min. » et réduit progressivement sa durée.

## Démonstration

L'illustration locale du mouvement est affichée par défaut avant la saisie. Elle
ouvre la fiche détaillée avec muscles, trajectoire, bonne exécution, erreur
fréquente, réglage et alternative.

Aucune vidéo ni URL n'est inventée. Une vidéo externe ne sera ajoutée qu'après
validation éditoriale d'une source fiable.

## Test Pixel 8

Exercice contrôlé : presse à cuisses.

- format conseillé : Complet ;
- charge précédente : 80 kg ;
- charge préremplie : 80 kg ;
- meilleure charge : 80 kg ;
- répétitions initiales : 9 ;
- RPE initial : 6 ;
- raccourcis : 75, 80, 82 et 85 kg ;
- illustration : visible avant la série ;
- confirmation de redémarrage : visible ;
- annulation : aucune donnée supprimée, progression Programme restée à 2/9.

## Tests automatisés

- 53 tests unitaires, 0 échec ;
- 7 tests instrumentés, **OK (7 tests)** ;
- test de suppression ciblée sur base en mémoire ;
- compilation complète : **BUILD SUCCESSFUL in 5m 16s**, 108 tâches.

## Limites explicites

Le projet ne stocke pas encore la taille. Le poids Health Connect est utilisé
dans le suivi lorsqu'il existe, mais n'est pas transformé en charge machine :
les résistances diffèrent trop entre équipements.

Il n'existe pas de modèle clinique ou de jeu de données populationnel intégré.
Le coach est un moteur de règles personnelles transparent, pas un dispositif
médical ni une prédiction statistique clinique.

## Livrables

- `outputs/Projet-Renaissance-0.1.0-coach-debug.apk`
- taille : 12 589 884 octets ;
- SHA-256 :
  `B1F3201955CB726735E044B4F4BAF9C3C751192159A5F2DF9DFC75D10C3EB9FA` ;
- `outputs/workout-coach-overview.png`
- `outputs/workout-coach-load.png`
- `outputs/workout-restart-confirmation.png`
