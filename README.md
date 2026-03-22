# 📱 StackOverflow MVVM — Guide complet

Projet Android réalisé dans le cadre du cours **M2 Données Mobiles**.  
Architecture **MVVM + Clean Architecture + Hilt + Retrofit + Room**.

---

## 📋 Table des matières

1. [Architecture du projet](#1-architecture-du-projet)
2. [Structure des fichiers](#2-structure-des-fichiers)
3. [Les dépendances](#3-les-dépendances)
4. [Étape par étape — explication de chaque fichier](#4-étape-par-étape--explication-de-chaque-fichier)
5. [Le flux de données complet](#5-le-flux-de-données-complet)
6. [Les concepts clés](#6-les-concepts-clés)
7. [Comment adapter à un autre sujet](#7-comment-adapter-à-un-autre-sujet)
8. [Les pièges à éviter](#8-les-pièges-à-éviter)
9. [Checklist exam](#9-checklist-exam)

---

## 1. Architecture du projet

Le projet suit la **Clean Architecture** divisée en 4 couches :

```
┌─────────────────────────────────────────────┐
│  PRESENTATION  (UI, ViewModel, UiState)     │  ← voit uniquement le domaine
├─────────────────────────────────────────────┤
│  DI            (Modules Hilt)               │  ← connecte les couches
├─────────────────────────────────────────────┤
│  DATA          (Retrofit, Room, Mapper)     │  ← implémente le domaine
├─────────────────────────────────────────────┤
│  DOMAIN        (Model, Repository)         │  ← ne dépend de rien
└─────────────────────────────────────────────┘
```

**La règle d'or** : les dépendances ne vont que vers l'intérieur.  
`domain` ne connaît ni Retrofit, ni Room, ni Android.

---

## 2. Structure des fichiers

```
app/src/main/java/fr/mastersid/etudiant/template/
│
├── MyApplication.kt                    ← @HiltAndroidApp
├── MainActivity.kt                     ← @AndroidEntryPoint
│
├── domain/                             ← COUCHE 1 : Kotlin pur, zéro dépendance Android
│   ├── model/
│   │   └── Question.kt                 ← data class métier
│   └── repository/
│       ├── QuestionsRepository.kt      ← interface contrat
│       └── QuestionsResponse.kt        ← sealed interface états (Idle/Pending/Error)
│
├── data/                               ← COUCHE 2 : implémentations concrètes
│   ├── remote/
│   │   ├── api/
│   │   │   └── StackOverflowApiService.kt  ← interface Retrofit @GET
│   │   └── dto/
│   │       └── QuestionDto.kt              ← @JsonClass Moshi
│   ├── local/
│   │   ├── entity/
│   │   │   └── QuestionEntity.kt           ← @Entity Room
│   │   ├── dao/
│   │   │   └── AppDao.kt                   ← @Dao insertAll + Flow
│   │   └── database/
│   │       └── AppDatabase.kt              ← @Database
│   ├── mapper/
│   │   └── QuestionMapper.kt               ← DTO→Domain, Domain→Entity, Entity→Domain
│   └── repository/
│       ├── FakeQuestionsRepository.kt      ← test hors ligne (delay 5s)
│       └── RemoteQuestionsRepository.kt    ← vrai appel réseau + Room (SSOT)
│
├── di/                                 ← COUCHE 3 : injection Hilt
│   ├── AppModule.kt                    ← @Binds interface → implémentation
│   ├── NetworkModule.kt                ← @Provides Moshi + OkHttp + Retrofit
│   └── DatabaseModule.kt              ← @Provides Room + DAO
│
└── presentation/                       ← COUCHE 4 : UI
    ├── items/
    │   ├── QuestionsUiState.kt         ← état unique de l'écran
    │   ├── QuestionsViewModel.kt       ← LiveData + postValue + Dispatchers.IO
    │   └── QuestionsScreen.kt          ← observeAsState + CircularProgressIndicator
    └── theme/
        └── Theme.kt                    ← couleurs Material3
```

---

## 3. Les dépendances

Toutes les versions sont centralisées dans `gradle/libs.versions.toml`.

| Bibliothèque | Rôle | Version |
|---|---|---|
| **Hilt** | Injection de dépendances | 2.51.1 |
| **Retrofit** | Appels réseau HTTP | 2.11.0 |
| **Moshi** | Désérialisation JSON | 1.15.1 |
| **OkHttp** | Client HTTP + logging | 4.12.0 |
| **Jsoup** | Nettoyage HTML | 1.18.1 |
| **Room** | Base de données SQLite | 2.6.1 |
| **LiveData** | Observation de l'état (comme le cours) | 2.8.4 |
| **KSP** | Génération de code à la compilation | 2.0.0-1.0.24 |
| **Compose** | UI déclarative | BOM 2024.08.00 |

> **Pourquoi KSP et pas kapt ?**  
> KSP (Kotlin Symbol Processing) est plus rapide que kapt car il traite directement le code Kotlin sans passer par Java. C'est la recommandation actuelle de Google.

---

## 4. Étape par étape — explication de chaque fichier

### 4.1 `domain/model/Question.kt`

```kotlin
data class Question(
    val id: Int,
    val title: String,
    val answerCount: Int,
    val lastActivityDate: Long = 0L,
    val body: String = ""
)
```

**Pourquoi `data class` ?**  
Génère automatiquement `equals()`, `hashCode()`, `toString()`, `copy()`.  
Tous les champs sont `val` (immuables) — en Compose, on préfère créer un nouvel objet avec `copy()` plutôt que de modifier l'existant.

**Pourquoi dans `domain/` ?**  
`Question` est l'objet métier pur. Il ne dépend d'aucune bibliothèque externe (pas d'annotation `@Entity` ou `@JsonClass`). Si on change demain de Retrofit à GraphQL, `Question` ne change pas.

---

### 4.2 `domain/repository/QuestionsResponse.kt`

```kotlin
sealed interface QuestionsResponse {
    data class Idle(val questions: List<Question>) : QuestionsResponse
    data object Pending : QuestionsResponse
    data class Error(val message: String) : QuestionsResponse
}
```

**Pourquoi `sealed interface` ?**  
Le compilateur connaît tous les cas possibles. Dans un `when`, si tu oublies un cas → erreur de compilation. Impossible d'oublier `Error` ou `Pending`.

**Les 3 états :**
- `Idle` → pas de requête en cours, la liste est disponible (depuis Room)
- `Pending` → requête en cours → afficher `CircularProgressIndicator`
- `Error` → la requête a échoué → afficher la `Snackbar`

---

### 4.3 `domain/repository/QuestionsRepository.kt`

```kotlin
interface QuestionsRepository {
    val questionsResponse: Flow<QuestionsResponse>
    suspend fun updateQuestionsInfo()
}
```

**Pourquoi une interface ?**  
C'est le principe d'**inversion des dépendances** (SOLID).  
Le ViewModel dépend de l'interface, pas de l'implémentation concrète.  
On peut swapper `RemoteQuestionsRepository` par `FakeQuestionsRepository` sans toucher au ViewModel.

---

### 4.4 `data/remote/dto/QuestionDto.kt`

```kotlin
@JsonClass(generateAdapter = true)
data class QuestionDto(
    @Json(name = "question_id") val questionId: Int,
    @Json(name = "title")       val title: String,
    ...
)
```

**Pourquoi des DTOs séparés du modèle domaine ?**  
L'API retourne des champs en `snake_case` (`question_id`, `answer_count`). Le domaine utilise `camelCase` (`questionId`, `answerCount`). On ne mélange pas les annotations Moshi dans le domaine.

**`@JsonClass(generateAdapter = true)`**  
Génère l'adaptateur JSON à la **compilation** via KSP. Plus rapide que la réflexion à l'exécution.

**L'enveloppe `{ "items": [...] }`**  
L'API StackExchange ne retourne pas directement un tableau. Elle enveloppe dans `{ "items": [...] }`. D'où le DTO `QuestionsEnvelopeDto`.

---

### 4.5 `data/remote/api/StackOverflowApiService.kt`

```kotlin
interface StackOverflowApiService {
    @GET("questions?pagesize=20&order=desc&sort=activity&site=stackoverflow&filter=withbody")
    suspend fun getActiveQuestions(): QuestionsEnvelopeDto
}
```

**Comment Retrofit fonctionne ?**  
On écrit une interface avec des annotations HTTP. Retrofit génère à l'exécution une implémentation concrète qui fait les vrais appels HTTP.

**`suspend fun`**  
Rend la fonction compatible avec les coroutines. Retrofit sait qu'il doit exécuter la requête de façon asynchrone sur un thread IO.

---

### 4.6 `data/local/entity/QuestionEntity.kt`

```kotlin
@Entity(tableName = "question_table")
data class QuestionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "answer_count") val answerCount: Int,
    ...
)
```

**Pourquoi une Entity séparée de Question ?**  
Si on met `@Entity` sur `Question`, la couche `domain` dépend de Room → brise la Clean Architecture. Demain si on change Room par autre chose, seule l'Entity change, pas le domaine.

**`@PrimaryKey`**  
Clé primaire de la table. Utilisée par `OnConflictStrategy.REPLACE` pour mettre à jour une question déjà existante.

---

### 4.7 `data/local/dao/AppDao.kt`

```kotlin
@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM question_table ORDER BY last_activity_date DESC")
    fun getQuestionsFlow(): Flow<List<QuestionEntity>>
}
```

**`OnConflictStrategy.REPLACE`**  
Si une question avec le même `id` existe déjà → elle est remplacée. Les autres sont conservées.

**`Flow<List<QuestionEntity>>`**  
Room observe la table. À chaque `INSERT`, Room **réémet automatiquement** la nouvelle liste. L'UI se met à jour sans aucun code supplémentaire.

**Pourquoi `getQuestionsFlow()` n'est pas `suspend` ?**  
Un `Flow` est asynchrone par nature. On n'a pas besoin de `suspend` pour l'obtenir, seulement pour le collecter.

---

### 4.8 `data/local/database/AppDatabase.kt`

```kotlin
@Database(entities = [QuestionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
```

**`version = 1`**  
Si tu modifies la structure de la table, tu **incrémentes** ce numéro et fournis une `Migration`. Sans ça, Room détruit toutes les données.

---

### 4.9 `data/mapper/QuestionMapper.kt`

```kotlin
// DTO → Domain (avec nettoyage HTML via Jsoup)
fun QuestionDto.toDomain(): Question = Question(
    title = Jsoup.parse(title).text()  // "AT&amp;T" → "AT&T"
)

// Domain → Entity
fun Question.toEntity(): QuestionEntity = QuestionEntity(...)

// Entity → Domain
fun QuestionEntity.toDomain(): Question = Question(...)
```

**Pourquoi `@JvmName` sur les listes ?**  
La JVM efface les génériques à la compilation (*type erasure*). `List<QuestionDto>.toDomain()` et `List<QuestionEntity>.toDomain()` ont la même signature JVM. `@JvmName` force des noms différents dans le bytecode.

**Pourquoi Jsoup seulement dans DTO→Domain ?**  
Quand on lit depuis Room, les données ont déjà été nettoyées lors de l'insertion. Appliquer Jsoup une deuxième fois serait inutile.

---

### 4.10 `data/repository/RemoteQuestionsRepository.kt`

```kotlin
override val questionsResponse: Flow<QuestionsResponse> =
    combine(dao.getQuestionsFlow(), _requestState) { entities, state ->
        when (state) {
            is QuestionsResponse.Pending -> QuestionsResponse.Pending
            is QuestionsResponse.Error   -> state
            else -> QuestionsResponse.Idle(entities.toDomain())
        }
    }
```

**Pattern Single Source of Truth (SSOT)**
```
[Retrofit API] ──insertAll()──▶ [Room DB] ──Flow──▶ [ViewModel] ──▶ [UI]
```
L'UI n'observe **jamais** directement le réseau. Elle observe uniquement Room. Le réseau ne fait qu'alimenter Room.

**`combine(flow1, flow2)`**  
Fusionne deux Flows. Émet une nouvelle valeur chaque fois que l'un ou l'autre émet.

**Gestion des erreurs dans le Repository (pas dans le ViewModel)**
```kotlin
catch (e: IOException)   { /* Erreur réseau : mode avion, timeout... */ }
catch (e: HttpException)  { /* Erreur HTTP : 404, 500... */ }
```
Le ViewModel ne connaît pas `IOException`. C'est une responsabilité du Repository.

---

### 4.11 `di/NetworkModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideMoshi(): Moshi = ...

    @Provides @Singleton
    fun provideRetrofit(moshi: Moshi, client: OkHttpClient): Retrofit = ...
}
```

**Chaîne de dépendances Hilt**
```
provideApiService(retrofit)
        ↑
provideRetrofit(moshi, client)
        ↑              ↑
provideMoshi()   provideOkHttpClient()
```
Hilt résout automatiquement l'ordre d'instanciation.

**`@InstallIn(SingletonComponent::class)`**  
Retrofit et Room sont des singletons — une seule instance pour toute l'app.

---

### 4.12 `di/AppModule.kt`

```kotlin
@Module
@InstallIn(ViewModelComponent::class)   // comme le cours
abstract class AppModule {
    @Binds
    abstract fun bindQuestionsRepository(
        impl: RemoteQuestionsRepository
    ): QuestionsRepository
}
```

**`@Binds` vs `@Provides`**
- `@Provides` → pour des objets externes (Retrofit, Room) — on écrit la factory
- `@Binds` → pour lier une interface à une implémentation — plus efficace

**`ViewModelComponent` (comme le cours)**  
Le Repository suit le cycle de vie du ViewModel. À chaque nouveau ViewModel, une nouvelle instance du Repository.

> Pour tester hors ligne : remplacer `RemoteQuestionsRepository` par `FakeQuestionsRepository` — une seule ligne change.

---

### 4.13 `presentation/items/QuestionsUiState.kt`

```kotlin
data class QuestionsUiState(
    val questions      : List<Question> = emptyList(),
    val isUpdating     : Boolean        = false,
    val onlyNotAnswered: Boolean        = false,
    val errorMessage   : String?        = null
) {
    val displayedQuestions: List<Question>
        get() = if (onlyNotAnswered) questions.filter { it.answerCount == 0 } else questions
}
```

**Pourquoi un seul objet d'état ?**  
Avec plusieurs `LiveData` séparés, deux mises à jour simultanées peuvent créer un état incohérent. Avec un seul objet + `copy()`, la mise à jour est **atomique**.

**Propriété calculée `displayedQuestions`**  
Pas stockée en mémoire. Recalculée à chaque accès à partir de `questions` et `onlyNotAnswered`.

---

### 4.14 `presentation/items/QuestionsViewModel.kt`

```kotlin
@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val repository: QuestionsRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(QuestionsUiState())
    val uiState: LiveData<QuestionsUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {   // comme le cours
            repository.questionsResponse.collect { response ->
                _uiState.postValue(...)            // postValue depuis IO
            }
        }
    }
}
```

**`LiveData` (comme le cours)**  
Le cours utilise `LiveData + postValue() + observeAsState()`. C'est l'approche enseignée.  
*(La version moderne utilise `StateFlow + update{} + collectAsStateWithLifecycle()`)*

**`Dispatchers.IO`**  
Exécute la coroutine sur un thread dédié aux opérations I/O. Obligatoire comme le cours.

**`postValue()`**  
Thread-safe — peut être appelé depuis n'importe quel thread (contrairement à `setValue()` qui exige le thread principal).

**`viewModelScope`**  
Scope lié au cycle de vie du ViewModel. Annulé automatiquement quand le ViewModel est détruit → pas de fuite mémoire.

---

### 4.15 `presentation/items/QuestionsScreen.kt`

```kotlin
// Stateful — connecté au ViewModel
@Composable
fun QuestionsScreen(viewModel: QuestionsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.observeAsState(QuestionsUiState())
    ...
}

// Stateless — testable sans ViewModel
@Composable
fun QuestionsContent(uiState: QuestionsUiState, ...) {
    ...
    if (uiState.isUpdating) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}
```

**Pattern Hoisting (séparation stateful/stateless)**  
`QuestionsScreen` est difficile à tester (connecté au ViewModel).  
`QuestionsContent` reçoit juste des données → testable et prévisualisable avec `@Preview`.

**`observeAsState()`**  
Convertit le `LiveData` en `State<QuestionsUiState>` que Compose peut observer.  
Quand le `LiveData` émet → Compose recompose automatiquement.

**`CircularProgressIndicator` centré**  
Comme le cours chapitre 2. Utilise une `Box` pour superposer la liste et l'indicateur.

---

## 5. Le flux de données complet

```
Utilisateur appuie sur FAB
        ↓
updateQuestions() dans ViewModel
        ↓
repository.updateQuestionsInfo()
        ↓
emit(Pending) → isUpdating=true → CircularProgressIndicator visible
        ↓
apiService.getActiveQuestions()  ← coroutine suspendue
        ↓ (réponse reçue)
.items.toDomain()                ← DTO → Domain (Jsoup nettoie HTML)
        ↓
dao.insertAll(questions.toEntity())  ← persist dans Room
        ↓
Room Flow réémet automatiquement
        ↓
combine() → Idle(entities.toDomain())
        ↓
ViewModel reçoit → postValue(uiState)
        ↓
QuestionsScreen observeAsState → recompose
        ↓
Liste affichée à l'écran
```

---

## 6. Les concepts clés

### 6.1 Flow vs LiveData

| | LiveData | StateFlow |
|---|---|---|
| **Cours** | ✅ Utilisé | ❌ |
| **Android-aware** | ✅ | ❌ (besoin de `collectAsStateWithLifecycle`) |
| **Valeur initiale** | Optionnelle | Obligatoire |
| **Thread-safe** | `postValue()` | `update {}` |

### 6.2 Room — Single Source of Truth

```
SANS SSOT :              AVEC SSOT (notre projet) :
API → UI directement     API → Room → UI
(pas de cache)           (cache + offline)
```

### 6.3 Hilt — les annotations essentielles

| Annotation | Où | Rôle |
|---|---|---|
| `@HiltAndroidApp` | Application | Initialise le graphe Hilt |
| `@AndroidEntryPoint` | Activity | Connecte l'Activity à Hilt |
| `@HiltViewModel` | ViewModel | Permet l'injection dans le VM |
| `@Inject` | Constructeur | Déclare les dépendances |
| `@Module` | Classe | Contient les instructions de création |
| `@InstallIn` | Module | Définit la durée de vie |
| `@Provides` | Fonction | Crée un objet externe |
| `@Binds` | Fonction abstraite | Lie interface → implémentation |
| `@Singleton` | Fonction | Une seule instance pour toute l'app |

---

## 7. Comment adapter à un autre sujet

Remplace ces éléments selon ton sujet d'exam :

| Crochet | StackOverflow | Films TMDB | Météo |
|---|---|---|---|
| Objet métier | `Question` | `Movie` | `Weather` |
| Champ 1 | `title` | `title` | `cityName` |
| Champ 2 | `answerCount` | `voteAverage` | `temperature` |
| Tri | `lastActivityDate` | `releaseDate` | `timestamp` |
| Enveloppe JSON | `"items"` | `"results"` | `"list"` |
| Base URL | `api.stackexchange.com/2.3/` | `api.themoviedb.org/3/` | `api.openweathermap.org/data/2.5/` |

**Ordre de construction toujours pareil :**
```
domain/ → data/ → di/ → presentation/
```

---

## 8. Les pièges à éviter

### ❌ `@Singleton` sur le Repository
```kotlin
// FAUX — incompatible avec ViewModelComponent
@Singleton
class RemoteQuestionsRepository ...

// CORRECT — pas d'annotation @Singleton
class RemoteQuestionsRepository ...
```

### ❌ `@JvmName` oublié sur les listes
```kotlin
// FAUX — clash JVM
fun List<QuestionDto>.toDomain(): List<Question>
fun List<QuestionEntity>.toDomain(): List<Question>

// CORRECT
@JvmName("dtoListToDomain")
fun List<QuestionDto>.toDomain(): List<Question>
@JvmName("entityListToDomain")
fun List<QuestionEntity>.toDomain(): List<Question>
```

### ❌ try/catch dans le ViewModel
```kotlin
// FAUX — le ViewModel ne doit pas connaître IOException
fun updateQuestions() {
    viewModelScope.launch {
        try { repository.updateQuestionsInfo() }
        catch (e: IOException) { ... }  // ← mauvais endroit
    }
}

// CORRECT — les erreurs sont gérées dans le Repository
fun updateQuestions() {
    viewModelScope.launch(Dispatchers.IO) {
        repository.updateQuestionsInfo()  // ne lève plus d'exception
    }
}
```

### ❌ Domain qui dépend de Room ou Retrofit
```kotlin
// FAUX — annotation Room dans le domaine
@Entity
data class Question(...)

// CORRECT — Kotlin pur dans le domaine
data class Question(...)
```

### ❌ `kapt` au lieu de `ksp`
```kotlin
// FAUX
kapt(libs.hilt.compiler)

// CORRECT (comme le cours)
ksp(libs.hilt.compiler)
```

---

## 9. Checklist exam

Suis ces étapes dans l'ordre. Le projet compile à chaque étape.

- [ ] **1.** `domain/model/[Objet].kt` — data class métier
- [ ] **2.** `domain/repository/[Objet]Response.kt` — sealed interface Idle/Pending/Error
- [ ] **3.** `domain/repository/[Objet]Repository.kt` — interface avec Flow + suspend fun
- [ ] **4.** `data/remote/dto/[Objet]Dto.kt` — @JsonClass + @Json(name = ...)
- [ ] **5.** `data/remote/dto/[Objet]sEnvelopeDto.kt` — wrapper { items: [...] }
- [ ] **6.** `data/remote/api/[App]ApiService.kt` — @GET suspend fun
- [ ] **7.** `data/local/entity/[Objet]Entity.kt` — @Entity @PrimaryKey @ColumnInfo
- [ ] **8.** `data/local/dao/[App]Dao.kt` — @Insert REPLACE + Flow réactif
- [ ] **9.** `data/local/database/[App]Database.kt` — @Database abstract class
- [ ] **10.** `data/mapper/[Objet]Mapper.kt` — 3 directions + @JvmName
- [ ] **11.** `data/repository/Remote[Objet]Repository.kt` — combine() + insertAll + try/catch
- [ ] **12.** `di/NetworkModule.kt` — Moshi + Retrofit + ApiService
- [ ] **13.** `di/DatabaseModule.kt` — Room.databaseBuilder + Dao
- [ ] **14.** `di/AppModule.kt` — @Binds + @InstallIn(ViewModelComponent)
- [ ] **15.** `presentation/[objet]s/[Objet]sUiState.kt` — data class état unique
- [ ] **16.** `presentation/[objet]s/[Objet]sViewModel.kt` — LiveData + postValue + Dispatchers.IO
- [ ] **17.** `presentation/[objet]s/[Objet]sScreen.kt` — observeAsState + CircularProgressIndicator
- [ ] **18.** `presentation/theme/Theme.kt` — MaterialTheme couleurs
- [ ] **19.** `[App]Application.kt` — @HiltAndroidApp
- [ ] **20.** `MainActivity.kt` — @AndroidEntryPoint + setContent { [Objet]sScreen() }
- [ ] **21.** `AndroidManifest.xml` — INTERNET permission + android:name=.[App]Application

---

## 🔗 Ressources

- [Documentation Hilt](https://dagger.dev/hilt/)
- [Documentation Room](https://developer.android.com/training/data-storage/room)
- [Documentation Retrofit](https://square.github.io/retrofit/)
- [Documentation Compose](https://developer.android.com/jetpack/compose)
- [API StackExchange](https://api.stackexchange.com/docs)

---

*Projet réalisé dans le cadre du cours M2 Données Mobiles — MasterSID*
