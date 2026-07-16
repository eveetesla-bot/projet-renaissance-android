# Rapport Pixel 8 — médias, redémarrage et réinitialisations

Date : 16 juillet 2026  
Appareil : Pixel 8 (`3A240DLJH001ZG`)  
Application principale : `fr.projetrenaissance`

## Résultat global

Le code, la migration Room 3 → 4, les APK et les tests sont validés. L’APK
principal est installé par mise à jour sur le Pixel 8 et les données de Gérard
n’ont pas été réinitialisées.

La couverture de photos réelles de machines reste incomplète : aucune des douze
photos ne possède encore une source locale ou autorisée validée. L’application
affiche donc honnêtement « Photo machine à valider ».

## Compilation et tests

- JDK 17.0.19 ;
- compileSdk 36, targetSdk 35 ;
- Android Gradle Plugin 8.11.1 ;
- Gradle 8.13 ;
- `BUILD SUCCESSFUL` ;
- 53 tests unitaires, 0 échec ;
- 17 tests instrumentés sur Pixel 8, 0 échec ;
- migrations Room 1→2, 2→3 et 3→4 validées.

Les tests instrumentés couvrent :

- séance vide ;
- séance avec une série ;
- séance avec plusieurs séries et RPE ;
- repos et note en cours ;
- fermeture/réouverture de la séance ;
- conservation de l’historique et de l’autre profil ;
- remise à zéro de la journée ;
- remise à zéro du profil ;
- remise à zéro totale et restauration du contenu éditorial.

## Test réel du redémarrage

Le test destructif a été réalisé dans `fr.projetrenaissance.soniatest`, isolé
des données de Gérard :

1. série de vélo enregistrée ;
2. plusieurs séries d’un mouvement suivant enregistrées ;
3. exercice courant et note persistés après navigation ;
4. confirmation « Recommencer cette séance » ouverte ;
5. transaction confirmée ;
6. retour au mouvement 1, repos arrêté, note et douleur effacés ;
7. message de réussite affiché ;
8. progression du programme revenue de 1/9 à 0/9 ;
9. historique antérieur conservé par le test instrumenté dédié.

Un défaut d’affichage découvert pendant le test (`série 3/2`) a été corrigé :
quand toutes les séries sont faites, l’écran affiche maintenant « Exercice
terminé » et désactive le bouton d’enregistrement.

## Réinitialisations

### Journée

Validée sur appareil. Le message confirme que l’historique antérieur et les
données santé sont conservés.

### Profil local

Validée dans l’application de test. Le parcours de démarrage Sonia réapparaît et
les tests prouvent que les données Gérard restent intactes.

### Totale

Validée dans l’application de test. Un premier essai a révélé un écran de
chargement propre à la variante verrouillée Sonia ; le profil automatique a été
réappliqué après la transaction. Le second essai revient correctement à
« Bienvenue Sonia ». Dans l’application principale non verrouillée, le flux
revient au choix du profil.

Les mesures du magasin Health Connect ne sont pas supprimées. Seul l’accès de
l’application est révoqué, afin que les autorisations soient demandées de
nouveau.

## Santé au démarrage

Le parcours Gérard réel a été testé :

- introduction ;
- écran Health Connect ;
- synchronisation réelle ;
- création du plan ;
- retour à l’accueil ;
- préparation recalculée et sources présentes.

L’intention système « Gérer / révoquer les autorisations » ouvre correctement
la page Health Connect de Projet Renaissance sur le Pixel 8.

## Médias

### Terminé

- 12 fiches structurées ;
- départ, intermédiaire et finale ;
- trajectoire, pivot, appuis, amplitude, respiration, zone d’effort ;
- silhouette et machine dessinées plus lisiblement ;
- vue plein écran ;
- actions mouvement, machine, vidéo et livre ;
- URL vidéo absente et bouton désactivé quand aucune source n’est validée ;
- plan de couverture dans `MACHINE_PHOTO_PLAN.md`.

### Restant

- 0 photo réelle validée sur 12 ;
- 12 photos à prendre ou à obtenir avec droits documentés ;
- 0 URL vidéo validée ;
- ouverture directe du document Word embarqué non implémentée.

## Captures

- `outputs/guided-movement-v2b-pixel8.png`
- `outputs/machine-to-validate-pixel8.png`
- `outputs/media-actions-pixel8.png`
- `outputs/restart-confirmation-v2-pixel8.png`
- `outputs/restart-success-pixel8.png`
- `outputs/reset-center-pixel8.png`
- `outputs/profile-reset-onboarding-pixel8.png`
- `outputs/reset-total-confirmation-pixel8.png`
- `outputs/reset-total-fixed-pixel8.png`
- `outputs/manage-health-permissions-pixel8.png`

## État du téléphone après test

Les paquets de test ont été désinstallés. Seul `fr.projetrenaissance` reste
installé et lancé. Aucun `AndroidRuntime` fatal n’a été observé.

APK livré : `outputs/Projet-Renaissance-0.1.0-media-reset-debug.apk`  
Taille : 12 630 890 octets  
SHA-256 : `D89665A78B55293DCA9ADDC3196079BA3FE0080D2C8CEE596CB346A73443D2FA`
