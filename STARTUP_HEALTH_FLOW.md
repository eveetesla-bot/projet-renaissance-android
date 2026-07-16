# Chargement santé au démarrage

## Premier lancement ou après réinitialisation totale

1. Choix de Gérard ou Sonia.
2. Introduction au fonctionnement local et personnel.
3. Demande des autorisations Health Connect, avec possibilité de continuer sans.
4. Première synchronisation après autorisation.
5. Calcul de la préparation avec les données réellement disponibles.
6. Création dérivée du premier plan du jour puis affichage de l’accueil.

Les premières journées sans références personnelles conservent une confiance
réduite. Aucune moyenne de Gérard n’est utilisée pour Sonia, et inversement.

## Lancement quotidien

Si l’onboarding est terminé et que la synchronisation santé est activée :

- l’application vérifie si une permission compatible reste accordée ;
- une permission absente désactive calmement la synchronisation locale ;
- une synchronisation est lancée si la dernière réussite date de six heures ou
  plus ;
- les références, la préparation et le plan dérivé sont recalculés par les flux
  locaux avant l’affichage de leurs nouvelles valeurs.

## Gestion des autorisations

L’écran Santé propose deux actions distinctes :

- autoriser les données demandées ;
- gérer ou révoquer l’accès dans l’écran système Health Connect.

Continuer sans données santé reste toujours possible depuis l’onboarding.
