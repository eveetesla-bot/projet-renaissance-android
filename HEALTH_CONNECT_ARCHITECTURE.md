# Architecture Health Connect — Projet Renaissance

## Décisions fermes

- Health Connect est l'unique passerelle Android pour les données santé externes. Aucune ancienne API Google Fit n'est appelée.
- Chaque installation ne lit que le magasin Health Connect du téléphone courant. Les données importées sont rattachées au profil local actif (`gerard` ou `sonia`) au moment de l'import.
- Aucun rapprochement automatique entre les magasins Health Connect de Gérard et Sonia n'est possible ou tenté.
- Toutes les données importées restent dans Room. Une future synchronisation entre installations sera une fonction distincte, chiffrée et soumise à un consentement séparé.
- La disponibilité, le refus et la révocation des permissions sont des états normaux de l'application, jamais des erreurs bloquantes.

## Compatibilité

- L'application conserve `minSdk 26`.
- Health Connect est utilisable à partir d'Android 9 (API 28) avec Google Play. Sur API 26–27, l'application reste utilisable sans intégration santé.
- Android 14 et ultérieur fournissent Health Connect comme composant système. Android 9 à 13 utilisent l'application Health Connect distribuée par Google Play.
- Dépendance retenue pour le premier lot : `androidx.health.connect:connect-client:1.1.0`, version stable.

## Composants

```text
Compose UI
├── Tableau de bord santé
├── Historique > Données santé
└── Paramètres > Santé et synchronisation
    ├── Permissions
    ├── Sources prioritaires
    └── Diagnostic
          │
          ▼
HealthConnectViewModel
          │
          ▼
HealthDataRepository (contrat domaine)
├── LocalHealthDataSource (Room)
├── HealthConnectDataSource (SDK Jetpack)
└── SourcePriorityStore (DataStore)
          ▲
          │
HealthConnectSyncWorker (WorkManager)
```

`HealthConnectDataSource` est isolé derrière une interface pour permettre des tests unitaires sans appareil. Le dépôt applique l'association au profil, l'attribution, la déduplication et les transactions Room. Le Worker orchestre uniquement une synchronisation déjà autorisée.

## Types lus

Les permissions sont proposées par familles et uniquement lorsqu'une fonction visible les utilise.

| Famille visible | Types Health Connect |
|---|---|
| Activité | `StepsRecord`, `DistanceRecord`, `FloorsClimbedRecord`, `SpeedRecord` |
| Énergie | `ActiveCaloriesBurnedRecord`, `TotalCaloriesBurnedRecord` |
| Cardio | `HeartRateRecord`, `RestingHeartRateRecord`, `Vo2MaxRecord` |
| Corps | `WeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord` |
| Sommeil | `SleepSessionRecord` |
| Entraînement | `ExerciseSessionRecord` |
| Hydratation | `HydrationRecord` |

L'écran de consentement explique chaque famille. Une permission non accordée ne bloque pas les autres familles. Le diagnostic distingue « non demandé », « refusé », « accordé mais aucune donnée » et « donnée disponible ».

## Modèle local

### `HealthRecordEntity`

Table générique destinée aux lectures, diagnostics et graphiques :

- `healthConnectId` : identifiant Health Connect, clé de rapprochement ;
- `profileId` : profil local actif lors de l'import ;
- `recordType` : type normalisé ;
- `startTime` / `endTime` : epoch millisecondes ;
- `value` et `secondaryValue` : valeurs numériques normalisées ;
- `payloadJson` : détails spécifiques conservés sans bloquer l'évolution du schéma ;
- `sourcePackage` : `metadata.dataOrigin.packageName` ;
- `sourceLabel` : libellé résolu par `PackageManager`, avec repli sûr ;
- `sourceCategory` : `WITHINGS`, `GOOGLE_FIT`, `BASIC_FIT`, `RENAISSANCE`, `DEVICE`, `OTHER` ;
- `deviceManufacturer`, `deviceModel`, `deviceType` si présents ;
- `recordingMethod` et `lastModifiedAt` ;
- `importedAt` ;
- `dedupeKey`, `isPreferred`, `isDeleted`.

La contrainte unique principale porte sur `(profileId, healthConnectId)`. L'identifiant n'est jamais utilisé sans le profil afin de préserver l'isolation logique des deux installations.

### État de synchronisation

`HealthSyncStateEntity` stocke par profil et type : jeton de changements, dernier succès, dernière tentative, dernière erreur et état de permission. Les jetons sont séparés par type, conformément à la recommandation Android : la révocation d'une permission ne doit pas invalider toute la synchronisation.

## Attribution des sources

L'origine factuelle est toujours `Metadata.dataOrigin.packageName`. Le nom affiché est résolu localement avec `PackageManager`, sans demander `QUERY_ALL_PACKAGES`.

- « Withings » n'est affiché que si le package ou le libellé résolu correspond à l'application réellement présente.
- « Google Fit » suit la même règle.
- « Basic-Fit » n'est affiché que si les métadonnées du record l'indiquent.
- Le package `fr.projetrenaissance` est affiché comme « Projet Renaissance ».
- Les sources synthétiques de pas du téléphone ne sont jamais codées en dur ; elles sont affichées comme appareil/téléphone à partir des métadonnées disponibles.
- Toute origine non reconnue reste « Autre source » avec son nom réel si disponible.

## Permissions et confidentialité

- La demande de permissions est déclenchée par une action explicite de l'utilisateur.
- Les permissions d'écriture sont séparées des permissions de lecture et ne sont demandées qu'après activation de « Écrire mes données Renaissance ».
- La lecture en arrière-plan n'est demandée que si la synchronisation périodique est activée.
- Le bouton « Gérer les autorisations » ouvre l'écran Health Connect approprié.
- « Supprimer les données importées » efface uniquement Room et les jetons locaux ; il ne supprime pas les données des applications sources.
- « Révoquer l'accès » arrête le Worker, efface les jetons puis dirige l'utilisateur vers la gestion des permissions.
- Aucun client réseau ni cloud n'est introduit.

## Écriture depuis Projet Renaissance

Après consentement explicite seulement :

- séance terminée → `ExerciseSessionRecord` avec `clientRecordId` stable ;
- poids saisi → `WeightRecord` avec identifiant client stable ;
- calories estimées → écrites uniquement si l'interface les marque comme estimées ;
- fréquence cardiaque → jamais fabriquée et jamais réécrite comme mesure Renaissance si elle provient d'une autre source.

Les identifiants clients et versions empêchent une seconde insertion de la même donnée. Une séance externe proche ne sera pas supprimée : elle sera masquée du total si la règle de déduplication la considère équivalente.

## Écrans

```text
Accueil
└── Cartes santé : pas, sommeil, FC repos, dernière séance, poids, récupération
    └── attribution discrète de la source

Historique
└── Données santé
    ├── période
    ├── type
    ├── source
    └── profil local

Paramètres
└── Santé et synchronisation
    ├── État et consentement
    ├── Synchroniser maintenant
    ├── Synchronisation périodique
    ├── Sources prioritaires
    ├── Diagnostic
    ├── Supprimer les données importées
    └── Gérer/révoquer les autorisations
```

## Limites de Basic-Fit

Projet Renaissance ne peut pas obliger Basic-Fit à écrire dans Health Connect et ne peut pas déduire son comportement depuis une documentation marketing. L'intégration voit uniquement les records réellement présents dans Health Connect et leurs métadonnées.

Par conséquent :

- l'absence de données Basic-Fit est un résultat valide ;
- une séance n'est attribuée à Basic-Fit que lorsque le package source le prouve ;
- des données vues dans l'application Basic-Fit peuvent ne pas être publiées dans Health Connect ;
- le type, la granularité et le délai de publication dépendent de la version de Basic-Fit, de ses réglages, du téléphone et des permissions ;
- le diagnostic sur le téléphone est la seule preuve opérationnelle des types effectivement partagés.

## Validation sur appareil

La compilation et les tests unitaires valident le code, mais ne prouvent pas les données réellement partagées par Withings ou Basic-Fit. Le rapport final sur le téléphone doit être produit après : installation, permissions accordées, injection éventuelle via Health Connect Toolbox, synchronisation manuelle et lecture du diagnostic.

## Références officielles

- [Démarrer avec Health Connect](https://developer.android.com/health-and-fitness/health-connect/get-started)
- [Synchroniser les données](https://developer.android.com/health-and-fitness/health-connect/sync-data)
- [Lire les données](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Agrégation et doublons](https://developer.android.com/health-and-fitness/health-connect/aggregate-data)
- [Attribution des données](https://developer.android.com/health-and-fitness/health-connect/ui/data?hl=fr)
- [Cas de test et Health Connect Toolbox](https://developer.android.com/health-and-fitness/health-connect/test/test-cases)
