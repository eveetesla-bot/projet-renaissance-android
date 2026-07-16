# Stratégie de réinitialisation

## Niveau 1 — Journée

La transaction Room supprime, pour le profil actif et le jour civil local :

- séries et charges du jour ;
- bilan et réponses du jour ;
- mesure corporelle du jour ;
- séance ouverte pendant la journée ;
- état local du plan du jour.

Elle conserve l’historique antérieur, le profil, les préférences, le programme
éditorial et toutes les données Health Connect déjà synchronisées.

## Niveau 2 — Profil local

La transaction Room supprime les mesures, bilans, séries, séances ouvertes,
données santé importées et états de synchronisation du profil actif. DataStore
oublie ses priorités de sources, ses drapeaux de connexion santé, son onboarding
et, pour Sonia, la validation médicale locale.

Les données système Health Connect et l’autre profil ne sont pas touchés. Le
parcours santé est réaffiché pour le profil réinitialisé.

## Niveau 3 — Total

L’application :

1. attend la fin d’une éventuelle écriture de série ;
2. révoque les autorisations Health Connect de l’application sans supprimer les
   mesures du téléphone ;
3. efface les tables Room ;
4. efface DataStore ;
5. annule la synchronisation périodique ;
6. réinstalle uniquement le contenu éditorial embarqué ;
7. revient au choix du profil, comme lors d’une première installation.

## Limites et garanties

- Health Connect, Withings et Google Fit ne sont jamais vidés par ces actions.
- Le redémarrage d’une séance utilise son identifiant persistant, pas une heure
  approximative.
- La suppression des séries et la remise à zéro de la séance sont regroupées
  dans une transaction Room.
- Si cette transaction échoue, l’état de séance affiché n’est pas remis à zéro.
