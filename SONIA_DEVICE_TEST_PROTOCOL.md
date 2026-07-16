# Protocole de test réel — téléphone de Sonia

Ce protocole utilise l'application isolée `fr.projetrenaissance.soniatest`. Elle
peut être installée à côté de l'application Gérard sans partager Room,
DataStore, permissions ou tâches WorkManager. Il interdit tout transfert de
base, sauvegarde ou export JSON entre les deux applications.

## 0. Conditions préalables

1. Connecter le téléphone choisi pour le test.
2. Relever dans `adb devices -l` le numéro de série, le modèle et l'état
   `device`.
3. Pour un test réel Sonia, faire confirmer qu'il s'agit de son téléphone et de son compte
   Health Connect.
4. Pour un test proxy sur le téléphone de Gérard, noter explicitement que les
   mesures importées ne sont pas celles de Sonia.
5. Vérifier que Withings et/ou Google Fit ont terminé leur propre
   synchronisation vers Health Connect.
6. Exécuter :

```powershell
powershell -ExecutionPolicy Bypass -File .\verify-sonia-preflight.ps1
```

Le contrôle doit terminer par `Precontrole Sonia reussi`.

## 1. Installation propre

Vérifier d'abord si Sonia Test existe :

```powershell
.\work\toolchain\sdk\platform-tools\adb.exe shell pm path fr.projetrenaissance.soniatest
```

Le résultat attendu est vide. Si l'application existe déjà sur le téléphone de
Sonia, ne pas continuer : demander à Sonia l'autorisation de la désinstaller,
car cette action efface ses données locales de l'application.

Installer ensuite l'APK Sonia Test :

```powershell
.\work\toolchain\sdk\platform-tools\adb.exe install .\outputs\Projet-Renaissance-0.1.0-sonia-test-debug.apk
```

Ne pas utiliser une sauvegarde Android, `adb restore`, un export JSON ou une
copie du dossier `work` de Gérard.

## 2. Premier lancement avant autorisations

1. Lancer « Renaissance Sonia Test ».
2. Vérifier que Sonia est sélectionnée automatiquement et que le changement de
   profil n'est pas proposé.
3. Vérifier le profil :
   - Sonia, 51 ans ;
   - capsulite de l'épaule droite et migraines ;
   - validation médicale désactivée ;
   - mouvements au-dessus de la tête désactivés.
4. Ouvrir Santé et synchronisation.
5. Avant le premier import, relever :
   - aucune source détectée ;
   - sommeil `0/4 nuits` ;
   - FC au repos `0/3 valeurs` ;
   - VFC RMSSD `0/4 valeurs` ;
   - aucun score précis inventé.
6. Capturer l'Accueil, le Profil et les références personnelles vierges.

## 3. Autorisations Health Connect

1. Appuyer sur « Gérer les autorisations ».
2. Vérifier que l'écran appartient bien au magasin Health Connect du téléphone
   de Sonia.
3. Accorder uniquement les lectures nécessaires au test.
4. Ne jamais autoriser un compte, une sauvegarde ou une source appartenant à
   Gérard.
5. Relever le nombre de types accordés et les éventuels refus.

## 4. Première synchronisation

1. Appuyer une seule fois sur « Synchroniser maintenant ».
2. Attendre le message de fin.
3. Relever :
   - nombre total importé ;
   - nombre supprimé par Health Connect ;
   - nombre de types synchronisés ;
   - erreurs éventuelles.
4. Ne pas relancer immédiatement : conserver le premier résultat comme preuve.

## 5. Sources et déduplication

Dans « Diagnostic des sources », relever pour chaque source réellement détectée :

| Source | Paquet | Nombre total | Types disponibles |
|---|---|---:|---|
| Withings | À relever | À relever | À relever |
| Google Fit | À relever | À relever | À relever |
| Basic-Fit | À relever | À relever | À relever |
| Autre | À relever | À relever | À relever |

Vérifier que toute source à zéro est grisée, affiche « Aucune donnée détectée »
et ne peut pas devenir prioritaire.

Dans « Historique local », relever le total et le nombre de doublons écartés.
Comparer notamment les nuits, pas et séances présents chez plusieurs sources.

## 6. Métriques et références personnelles

### Sommeil

- vérifier la dernière nuit, sa durée, sa source et sa fraîcheur ;
- vérifier le nombre de nuits historiques utilisées ;
- avant 4 nuits historiques, la référence doit rester « En construction » ;
- à partir de 4 nuits, relever la référence personnelle affichée.

### Fréquence cardiaque au repos

- vérifier la dernière valeur, sa date et sa source ;
- avant 3 valeurs historiques, elle ne doit pas contribuer au score cardio ;
- à partir de 3 valeurs, relever la moyenne personnelle.

### VFC RMSSD

- vérifier que la donnée est bien de type RMSSD ;
- avant 4 valeurs historiques, elle ne doit pas contribuer au score cardio ;
- à partir de 4 valeurs, relever la moyenne personnelle.

## 7. Score de préparation

Relever :

- score ou fourchette affichée ;
- classification ;
- confiance en pourcentage ;
- niveau de fiabilité ;
- facteurs inclus ;
- facteurs absents ;
- conseil du jour.

Une première journée avec peu d'historique doit produire une confiance réduite.
Un score élevé avec confiance inférieure à 85 % doit être présenté comme une
estimation et demander une confirmation à l'échauffement.

## 8. Restrictions Sonia

1. Vérifier que « Validation médicale renseignée » est désactivée.
2. Ouvrir les séances A, B et C de la première phase.
3. Vérifier l'absence de mouvement au-dessus de la tête.
4. Vérifier que les exercices conditionnels d'épaule ne sont pas proposés sans
   validation.
5. Dans la bibliothèque, vérifier que tout exercice sollicitant l'épaule porte
   un badge et une adaptation.
6. Ne pas activer la validation médicale pour les besoins du test.
7. Ne valider aucune série fictive.

## 9. Réinitialisation de secours

En cas de mauvaise association :

1. ouvrir Santé et synchronisation ;
2. choisir « Réinitialiser le profil local » ;
3. lire l'étendue de la suppression ;
4. confirmer uniquement avec l'accord de Sonia.

La réinitialisation efface localement les mesures, bilans, séries, imports,
états de synchronisation et priorités de Sonia. Elle ne supprime rien dans
Health Connect, Withings ou Google Fit et ne touche jamais au profil Gérard.

## 10. Tests instrumentés sans données fictives

Après les contrôles fonctionnels, installer le paquet de tests :

```powershell
.\work\toolchain\sdk\platform-tools\adb.exe install -r .\outputs\Projet-Renaissance-0.1.0-sonia-test-androidTest.apk
.\work\toolchain\sdk\platform-tools\adb.exe shell am instrument -w fr.projetrenaissance.soniatest.test/androidx.test.runner.AndroidJUnitRunner
.\work\toolchain\sdk\platform-tools\adb.exe shell pm uninstall fr.projetrenaissance.soniatest.test
```

Résultat attendu : `OK (6 tests)`.

Les tests de séparation et de réinitialisation utilisent une base en mémoire.
Les tests visant la base principale sont strictement en lecture seule. Aucun test
ne crée ni ne supprime de donnée personnelle.

## 11. Logs et captures

Avant le lancement :

```powershell
.\work\toolchain\sdk\platform-tools\adb.exe logcat -c
```

Après le parcours :

```powershell
.\work\toolchain\sdk\platform-tools\adb.exe logcat -d AndroidRuntime:E *:S
```

Captures minimales :

- sélection/profil Sonia ;
- Accueil avant et après synchronisation ;
- références personnelles ;
- diagnostic des sources ;
- sommeil ;
- FC au repos ;
- VFC RMSSD ;
- score et détail de confiance ;
- programme et restriction d'épaule ;
- écran de réinitialisation avant confirmation.

## 12. Critères d'arrêt immédiat

Arrêter le test sans poursuivre si :

- le téléphone ou le compte Health Connect n'est pas celui de Sonia ;
- une donnée clairement attribuable à Gérard apparaît ;
- une préférence de source est déjà définie après installation propre ;
- un mouvement au-dessus de la tête est proposé ;
- une source sans donnée peut être sélectionnée ;
- l'application plante ou demande une écriture santé non prévue.

## 13. Fin du test proxy

Après sauvegarde du rapport et des captures :

1. utiliser « Réinitialiser le profil local » et confirmer ;
2. vérifier le retour à zéro des enregistrements et baselines ;
3. désinstaller uniquement Sonia Test :

```powershell
.\work\toolchain\sdk\platform-tools\adb.exe shell pm uninstall fr.projetrenaissance.soniatest
```

Ne jamais désinstaller `fr.projetrenaissance` lors de cette procédure.
