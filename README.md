# Android Application Template

**English** | [简体中文](README.zh-CN.md)

This project is an opinionated Android application template designed to provide a robust and
scalable foundation for modern application development. The template enforces a strict multi-module,
unidirectional architecture, and centralises build configuration in Gradle convention plugins so
that adding a new module stays a matter of a few lines rather than a copy-pasted block of
boilerplate.

## 1. Tech Stack & Core Principles

* **Language**: [Kotlin](https://kotlinlang.org/) with Kotlin DSL for Gradle. Kotlin compilation
  comes from **AGP 9's built-in Kotlin support**, so modules apply only the Android plugin and no
  `org.jetbrains.kotlin.android` plugin; `jvmTarget` follows `compileOptions` (Java 21). Pure JVM
  modules (`:domain:*`) use the standalone `org.jetbrains.kotlin.jvm` plugin instead.
* **Build System**: Gradle 9.8.0 + AGP 9.4.1, using Version Catalogs (`gradle/libs.versions.toml`)
  for dependency versions and `build-logic` convention plugins for shared configuration.
* **Toolchain**: JDK 21, `compileSdk` / `targetSdk` 37, `minSdk` 28 — all declared once in
  `AppConfig`.
* **UI**: Built entirely with [Jetpack Compose](https://developer.android.com/jetpack/compose)
  (Material 3, versions pinned by the Compose BOM). A single `MainActivity`, no XML layouts and no
  ViewBinding.
* **Navigation**: [Navigation-Compose](https://developer.android.com/jetpack/compose/navigation)
  with **type-safe routes** — destinations are `@Serializable` objects, not string routes.
* **Architectural Pattern**: Clean Architecture (domain / data / presentation) cut into vertical
  business slices, combined with MVI for the presentation layer.
* **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
  (via KSP) manages the dependency graph. Domain use cases use `javax.inject.Inject` — see
  [2.4 Implicit Contracts](#24-implicit-contracts).
* **Asynchronicity**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
  and [Flow](https://developer.android.com/kotlin/flow) for asynchronous work and reactive data
  streams.
* **Networking**: [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)
  with a logging interceptor and the kotlinx.serialization converter.
* **Data Persistence**:
    * [Room](https://developer.android.com/training/data-storage/room): structured local data
      (Kotlin codegen via KSP).
    * [DataStore](https://developer.android.com/topic/libraries/architecture/datastore): key-value
      storage.
    * [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview):
      paged data sources.
* **Testing**: JUnit 4, `kotlinx-coroutines-test`, AndroidX Test / Espresso, Compose UI tests
  (`ui-test-junit4`, wired up by the Compose convention plugin) and LeakCanary (debug builds only).
* **Gradle performance**: parallel builds, build cache, configuration cache (with `problems=fail`),
  and isolated projects are all enabled.

## 2. Architectural Design

### 2.1 Design Philosophy

The architecture rests on four ideas:

1. **Unidirectional dependencies.** Dependencies point one way only:
   `:app` → `:feature:*` → `:domain:*`, with `:data:*` implementing `:domain:*` and `:core:*`
   sitting underneath. Nothing lower in that chain may reference something higher.
2. **Dependency inversion at the domain boundary.** `:domain:*` is **pure Kotlin**. It declares
   entities, repository interfaces and use cases, and knows nothing about Room, Retrofit, Android,
   Hilt or Compose. `:data:*` implements those interfaces. `:feature:*` only ever sees the
   contracts.
3. **A feature is a vertical slice, cut into three layers.** Each business capability owns a
   `:domain:<name>`, a `:data:<name>` and a `:feature:<name>`. The slice is the unit of change; the
   layer is the unit of isolation.
4. **Shared code is split by kind.** Reusable *presentation* code lives in `:shared:*`; reusable
   *infrastructure* lives in `:core:*`. Neither knows anything about a specific feature, with one
   deliberate exception: `:core:database` owns the entire Room schema and therefore knows every
   table.

The benefit is that changes stay local and the compiler enforces the boundaries instead of code
review: swapping a data source means editing one `:data:*` module, deleting a capability means
deleting three modules, and a feature module physically cannot reach a database implementation
because it does not depend on one.

### 2.2 Module Map

```
:app                          # Composition root
├── :feature                  # Presentation (UI + MVI ViewModel + navigation)
│   └── :main
├── :domain                   # Business contracts (pure Kotlin)
│   └── :main
├── :data                     # Implementations of the domain contracts
│   └── :main
├── :shared                   # Reusable presentation code
│   ├── :designsystem
│   └── :ui
└── :core                     # Infrastructure capabilities
    ├── :common
    ├── :network
    └── :database
```

#### `:app` — Composition Root

* **Responsibility**: Assembles the application. It is the only module that knows about the whole
  graph.
* **Contents**: `TemplateApplication` (`@HiltAndroidApp`), the single `MainActivity`
  (`@AndroidEntryPoint`, `setContent { AppTheme { TemplateApp() } }`), the app-level navigation
  (`TemplateAppState` + `TemplateNavHost`), resources and the launcher icon.
* **Dependencies**: **every module in the graph** — `:feature:*`, `:data:*`, `:domain:*`, `:core:*`
  and `:shared:*` — declared explicitly one line per module. Do not build the list with constants
  or a `forEach` loop, and do not introduce an aggregator module — explicit lines stay indexable,
  clickable and diff-friendly.

  Depending on the whole graph is what lets every other module use plain `implementation`. Hilt
  generates its component inside `:app`, and that generated code references types from all layers:
  repository interfaces from `:domain:*`, DAOs and the database module from `:core:database`,
  ViewModels from `:feature:*`, base classes from `:shared:ui`. With `:app` holding the full
  compile classpath, no lower module needs to re-export anything through `api`.
* **Development Rules**: Keep it thin. It wires features together, owns the navigation graph and
  provides the Hilt binding entry point; business logic belongs elsewhere.

#### `:feature:*` — Presentation

* **Responsibility**: A single screen group of the product: UI, state handling and orchestration.
* **Contents**: Composable screens, MVI `ViewModel`s, state / side-effect types, and the feature's
  navigation API.
* **Development Rules**: Applies `template.android.feature` (which brings Hilt, Compose,
  `:core:common` and `:shared:ui`) plus `template.serialization` for the type-safe route object.
  Add `implementation(project(":domain:<name>"))` for the capabilities it consumes.
  **A feature must never depend on `:data:*`**, and never on another `:feature:*`. Cross-business
  orchestration happens in the ViewModel: inject two use cases and combine them there, rather than
  making one domain module call into another.

#### `:domain:*` — Business Contracts

* **Responsibility**: The business rules of one capability, expressed without any framework.
* **Contents**: Domain models, repository **interfaces**, and use cases. Use cases take
  `@Inject constructor` and are invoked as operators.
* **Development Rules**: Applies `template.kotlin.library` — a **pure JVM module**, no Android, no
  Hilt, no Room, no Compose. Its entire dependency set is:
  ```kotlin
  dependencies {
      api(libs.kotlinx.coroutines.core)   // use cases are suspend / return Flow
      implementation(libs.javax.inject)   // @Inject constructor only
  }
  ```
  Domain models are **not** Room entities: `:data:*` maps between them. A `:domain:*` module must
  not depend on `:data:*`, `:core:*`, `:feature:*`, `:shared:*`, or another `:domain:*`. Rules that
  genuinely span capabilities stay as orchestration in the ViewModel — there is no shared domain
  module to fall back on.

#### `:data:*` — Implementations

* **Responsibility**: Implements the repository interfaces of exactly one domain capability.
* **Contents**: Repository implementations, entity ↔ domain mappers, Retrofit service
  definitions, and the Hilt module that binds the interfaces.
* **Development Rules**: Applies `template.android.library` + `template.hilt` — **not**
  `template.room`, because the Room schema does not live here. Depends on `:domain:<name>` and on
  `:core:database` / `:core:network` with plain `implementation`: this is the only layer allowed to
  touch the infrastructure, and nothing has to be re-exported because `:app` already depends on
  every module. **A `:data:*` module must not depend on `:feature:*`, `:shared:*`, or another
  `:data:*`.**

  Implementation classes must be `public`: Hilt generates the component in `:app` and needs to
  reference them. Isolation comes from the dependency graph (`:feature:*` never depends on
  `:data:*`), not from `internal`. Classes that never participate in DI (mappers) can and should be
  `internal`.

#### `:core:database` — Persistence Infrastructure

* **Responsibility**: Owns the entire Room schema: the `@Database` class, every `@Entity`, every
  `@Dao`, migrations, converters, and the Hilt module that provides the database and its DAOs.
* **Contents**: `RoomDatabase` subclass, all entities and DAOs, `schemas/` output,
  `room.generateKotlin`, DI module.
* **Development Rules**: Applies `template.android.library` + `template.hilt` + `template.room`.
  Only `:data:*` modules depend on it.

  This module is the **single exception** to "infrastructure knows nothing about the business": it
  necessarily knows every table, because Room requires the `@Database` class to reference all
  entities and DAOs within the same compilation unit.

  Putting an `@Entity` or `@Dao` in a `:data:*` module instead creates a cycle: `:core:database`
  would have to depend on `:data:*` to register the table, while `:data:*` depends on
  `:core:database` to use it. KSP cannot break the cycle either — it only generates `*_Impl`
  classes for `@Dao` sources in its own compilation unit, so a DAO compiled in a module without
  `template.room` gets no implementation at all. `:data:*` therefore injects DAOs and maps entities
  to domain models; it never declares them.

#### `:core:network` — Network Infrastructure

* **Responsibility**: Everything needed to talk to a backend.
* **Contents**: Retrofit, the kotlinx.serialization converter, OkHttp and the logging interceptor.
  API service definitions belong here.
* **Development Rules**: Depends on `:core:common`. Applies `template.hilt` so services can be
  injected. Only `:data:*` modules depend on it.

#### `:core:common` — Foundation

* **Responsibility**: The baseline every module can rely on.
* **Contents**: Coroutines and kotlinx.serialization (exposed via `api`), DataStore Preferences, DI
  qualifiers such as `@IODispatcher`, and the `CoroutineModule` that provides them.
* **Development Rules**: Must never depend on `:domain:*`, `:data:*`, `:feature:*` or `:shared:*`.
  This is the easiest boundary to break by accident ("the model is right there"), and breaking it
  drags the domain back down into infrastructure.

#### `:shared:designsystem` — Visual Language

* **Responsibility**: Defines the basic visual specifications of the app.
* **Contents**: The Material 3 theme — `AppTheme` (light / dark schemes, dynamic color on
  Android 12+), colors and typography.
* **Development Rules**: Exposes the Compose BOM, `ui`, `material3` and the icon packs as `api`, so
  every consumer resolves Compose artifacts through this module. It deliberately does **not** apply
  Hilt, so it stays free of a DI framework and a KSP processor.

#### `:shared:ui` — UI Toolkit

* **Responsibility**: Reusable UI building blocks shared by every feature.
* **Contents**:
    * MVI runtime: `MVIContainer`, `IntentContext`, `BaseMVIViewModel`, plus
      `BaseAndroidMVIViewModel` — an `AndroidViewModel` variant kept for compatibility; prefer
      injecting `@ApplicationContext`.
    * Compose collect helpers: `MVIContainer.collectState()` (state as lifecycle-aware `State`)
      and `MVIContainer.collectSideEffect { ... }` (one-shot effects collected on
      `flowWithLifecycle`).
* **Development Rules**: Depends only on `:shared:designsystem` (plus activity-compose /
  lifecycle-runtime-compose, exposed via `api`). Applies `template.compose` but not Hilt — this
  module defines contracts, it does not inject anything.

### 2.3 Dependency Rules

| From                   | May depend on                                       | Must not depend on                                              |
|------------------------|-----------------------------------------------------|-----------------------------------------------------------------|
| `:app`                 | every module (`:feature:*`, `:data:*`, `:domain:*`, `:core:*`, `:shared:*`) | — (composition root)                                            |
| `:feature:*`           | `:domain:*`, `:shared:*`, `:core:common`            | `:data:*`, other `:feature:*`, `:core:database`, `:core:network` |
| `:domain:*`            | coroutines + `javax.inject` only                    | `:data:*`, `:core:*`, `:feature:*`, `:shared:*`, other `:domain:*` |
| `:data:*`              | `:domain:*` (its own), `:core:*`                    | `:feature:*`, `:shared:*`, other `:data:*`                       |
| `:core:*`              | `:core:common`                                      | `:domain:*`, `:data:*`, `:feature:*`, `:shared:*`               |
| `:shared:ui`           | `:shared:designsystem`                              | `:core:*`, `:domain:*`, `:data:*`, `:feature:*`                 |
| `:shared:designsystem` | —                                                   | `:core:*`, `:domain:*`, `:data:*`, `:feature:*`, `:shared:ui`   |

**Two rules are mandatory review checkpoints**, because they are broken by adding a single line to
a `build.gradle.kts` and nothing in the build will complain:

1. A `:feature:*` module must not list any `:data:*` dependency.
2. A `:domain:*` module must not list `:core:*`, `:data:*`, `:feature:*` or another `:domain:*`.

### 2.4 Implicit Contracts

These are not visible in the build file, and getting them wrong produces confusing errors:

* **`:app` must depend on every module, not only the data ones.** Hilt aggregates all `@InstallIn`
  modules into the component generated in `:app`, and that generated code references types from
  every layer. A missing module is reported at **compile time** (`[Dagger/MissingBinding]`, or
  "cannot access class" when a type is absent from the classpath) — it will not slip through to
  runtime, but the error points at the consumer, not at the missing dependency.
* **No lower module needs `api`.** Because `:app` already sees the whole graph, `:data:*` and
  `:feature:*` declare their dependencies with `implementation`. Reaching for `api` in a library
  module means the graph has a module that is not on `:app`'s dependency list.
* **`@InstallIn` modules need `implementation`, not `runtimeOnly`.** They must be on the
  *compile* classpath of `:app` for Hilt's processor to see them.
* **`:domain:*` use cases use `javax.inject.Inject`, never `jakarta.inject.Inject`.** Dagger only
  recognises the `javax` namespace; the `jakarta` annotation is silently ignored and surfaces as
  "cannot be provided without an @Inject constructor".
* **Domain models are not Room entities.** `:data:*` owns the mapping in both directions.
* **`:core:database` owns every `@Entity` and `@Dao`; `:data:*` declares none.** A DAO placed in a
  `:data:*` module would either be invisible to the `@Database` class or force a circular
  dependency, and KSP generates no implementation for a `@Dao` compiled outside `template.room`.

### 2.5 Build Logic (`build-logic`)

All module configuration lives in `build-logic/convention` as convention plugins, so a module's
build file usually contains only its namespace and its own dependencies.

| Plugin ID                      | Composed of                                                       | Purpose                                                                                                        |
|--------------------------------|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `template.android.application` | `com.android.application` + `template.hilt` + `template.compose`  | Application setup: identity, SDK levels, Java 21, shared test dependencies                                     |
| `template.android.library`     | `com.android.library`                                             | Minimal library setup: SDK levels, Java 21, shared test dependencies                                           |
| `template.android.feature`     | `template.android.library` + `template.hilt` + `template.compose` | Feature setup, plus `:core:common` and `:shared:ui` dependencies                                               |
| `template.kotlin.library`      | `org.jetbrains.kotlin.jvm`                                        | Pure JVM module for `:domain:*`: Java 21 target, shared test dependencies, no Android artifacts                 |
| `template.compose`             | `org.jetbrains.kotlin.plugin.compose`                             | Compose compiler, `buildFeatures.compose`, the Compose BOM, instrumented test artifacts and debug-only tooling |
| `template.hilt`                | `com.google.devtools.ksp` + `com.google.dagger.hilt.android`      | `hilt-android` plus its KSP compiler                                                                           |
| `template.room`                | `com.google.devtools.ksp` + `androidx.room`                       | `room-runtime` / `room-ktx` plus the KSP compiler                                                              |
| `template.serialization`       | `org.jetbrains.kotlin.plugin.serialization`                       | Kotlin serialization compiler plugin, opt-in per module (needed for type-safe navigation routes)               |

**Capabilities are opt-in on purpose.** `template.android.library` stays minimal; modules add
`template.hilt`, `template.compose` or `template.room` only if they need them. Only
`template.android.feature` bundles Hilt and Compose, because feature modules are numerous and
uniform in shape.

**`AppConfig` is the single source of truth** for application id, version, SDK levels and the Java
version:

```kotlin
object AppConfig {

    // --- Application identity and version ---

    const val APPLICATION_ID = "com.example.template"

    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0"

    // --- SDK levels ---

    const val COMPILE_SDK = 37
    const val MIN_SDK = 28
    const val TARGET_SDK = 37

    // --- Compiler options ---

    /** Java / Kotlin target. AGP 9 has built-in Kotlin, so jvmTarget follows compileOptions. */
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_21
}
```

Modules no longer declare `compileSdk`, `minSdk` or `versionCode` themselves — change a value here
and every module follows.

Two further AGP 9 notes:

* Release builds currently set `optimization { enable = false }`, so R8 optimization is off until
  you opt in.
* Keep rules live in `src/main/keepRules/*.keep` (see `app/src/main/keepRules/rules.keep`); AGP
  merges every file in that directory into the single rule set handed to R8.

## 3. Development Guide

Adding a business capability means creating three modules. The example below adds a
**"User Settings"** capability.

### Step 1: Create the Domain Module

Create `domain/settings/` with a `build.gradle.kts`:

```kotlin
plugins {
    id("template.kotlin.library")
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
```

Register it in `settings.gradle.kts`:

```kotlin
include(":domain:settings")
```

Declare the contract — a model, a repository interface and a use case:

```kotlin
// :domain:settings
data class Settings(val enabled: Boolean)

interface SettingsRepository {
    fun observe(): Flow<Settings>
    suspend fun setEnabled(enabled: Boolean)
}

class ObserveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Settings> = repository.observe()
}

class SetEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
}
```

Use cases stay atomic: one business operation, no knowledge of other capabilities. Composition of
two use cases belongs in the ViewModel.

### Step 2: Register the Schema in `:core:database`

Room requires the `@Database` class to see every entity and DAO, so the schema lives in
`:core:database` — never in the data module (see 2.4). Add the table there:

```kotlin
// :core:database
@Entity(tableName = "settings")
data class SettingsEntity(@PrimaryKey val id: Int = 0, val enabled: Boolean)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun observe(): Flow<SettingsEntity?>

    @Upsert
    suspend fun upsert(entity: SettingsEntity)
}

@Database(
    entities = [SettingsEntity::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}
```

Expose the DAO through the module's Hilt module:

```kotlin
// :core:database
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "template.db").build()

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()
}
```

Each new capability adds its entity to `entities`, a DAO accessor to `AppDatabase`, and a
`@Provides` for the DAO. Bump `version` and add a migration whenever the schema changes.

### Step 3: Create the Data Module

Create `data/settings/` with a `build.gradle.kts`:

```kotlin
plugins {
    id("template.android.library")
    id("template.hilt")
}

android {
    namespace = "com.example.template.data.settings"
}

dependencies {
    // implementation, not api: :app depends on every module directly, so nothing is re-exported.
    implementation(project(":domain:settings"))
    implementation(project(":core:database"))
}
```

Note there is no `template.room` here: the schema lives in `:core:database`, so this module has
nothing for KSP to generate. Implement the repository, mapping the entity to the domain model:

```kotlin
// :data:settings
// Public: Hilt generates the component in :app and must reference this type.
class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao,
) : SettingsRepository {

    override fun observe(): Flow<Settings> =
        dao.observe().map { Settings(enabled = it?.enabled ?: false) }

    override suspend fun setEnabled(enabled: Boolean) =
        dao.upsert(SettingsEntity(enabled = enabled))
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
```

### Step 4: Create the Feature Module

Create `feature/settings/` with a `build.gradle.kts`:

```kotlin
plugins {
    id("template.android.feature")
    id("template.serialization")
}

android {
    namespace = "com.example.template.feature.settings"
}

dependencies {
    // Contracts only — never :data:settings.
    implementation(project(":domain:settings"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
```

Hilt, Compose, `:core:common` and `:shared:ui` already come from the convention plugin — do not
declare them again. `template.serialization` is needed because the feature declares a
`@Serializable` route object (Step 7).

### Step 5: Define State and Side Effects

Inside the feature module, model the screen as an immutable state plus a one-shot side-effect type:

```kotlin
data class SettingsState(
    val enabled: Boolean = false,
)

sealed interface SettingsSideEffect {
    data class ShowMessage(val text: String) : SettingsSideEffect
}
```

### Step 6: Implement the ViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val setEnabled: SetEnabledUseCase,
) : BaseMVIViewModel<SettingsState, SettingsSideEffect>() {

    override val initialState = SettingsState()

    init {
        intent {
            observeSettings().collect { settings ->
                reduce { copy(enabled = settings.enabled) }
            }
        }
    }

    fun onToggle() = intent {
        setEnabled(!state.enabled)
        postSideEffect(SettingsSideEffect.ShowMessage("Settings updated"))
    }
}
```

`intent { ... }` runs in `viewModelScope`; inside it `state` reads the current state, `reduce`
produces the next one, and `postSideEffect` emits a one-shot event.

When a ViewModel needs a `Context`, inject it — do not switch to `BaseAndroidMVIViewModel`:

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observeSettings: ObserveSettingsUseCase,
) : BaseMVIViewModel<SettingsState, SettingsSideEffect>() { /* ... */ }
```

`BaseAndroidMVIViewModel` is an `AndroidViewModel` variant that exposes `application` as a public
property to every caller. The qualifier delivers the same instance without widening the ViewModel's
surface, so the base class has no advantage — prefer the injection.

Cross-capability orchestration goes here, not in a domain module:

```kotlin
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val getUser: GetUserUseCase,          // :domain:user
    private val createOrder: CreateOrderUseCase,  // :domain:order
) : BaseMVIViewModel<CheckoutState, CheckoutSideEffect>() { /* ... */ }
```

### Step 7: Expose the Feature's Navigation API

In the module's `navigation` package, declare the type-safe route and the graph / controller
extensions (mirroring `MainNavigation.kt`):

```kotlin
@Serializable
data object SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions) =
    navigate(route = SettingsRoute, navOptions = navOptions)

fun NavGraphBuilder.settingsScreen() {
    composable<SettingsRoute> {
        SettingsRoute()
    }
}
```

### Step 8: Implement the Screen

Follow the `MainRoute` / `MainScreen` pattern from `:feature:main`: a stateful `Route` composable
that owns the ViewModel, delegating to a stateless `Screen` composable.

```kotlin
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.collectState()

    viewModel.collectSideEffect { effect ->
        when (effect) {
            is SettingsSideEffect.ShowMessage -> /* show a snackbar, navigate, ... */
        }
    }

    SettingsScreen(
        enabled = state.enabled,
        onToggle = viewModel::onToggle,
    )
}

@Composable
private fun SettingsScreen(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    // stateless Composable; reads no ViewModel, only parameters
}
```

`collectState` collects the state with `collectAsStateWithLifecycle` and `collectSideEffect`
collects one-shot effects on `flowWithLifecycle`, so nothing is observed while the view is stopped
and nothing leaks when it goes to the background.

### Step 9: Wire It Into the App

Add the new modules to `app/build.gradle.kts`. `:app` depends on the whole graph (see 2.4), so the
list mirrors every module:

```kotlin
dependencies {
    // Features
    implementation(project(":feature:main"))
    implementation(project(":feature:settings"))

    // Data implementations
    implementation(project(":data:main"))
    implementation(project(":data:settings"))

    // Domain contracts — Hilt references the interfaces from its generated component
    implementation(project(":domain:main"))
    implementation(project(":domain:settings"))

    // Infrastructure
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // Shared UI
    implementation(project(":shared:designsystem"))
    implementation(project(":shared:ui"))
}
```

Then register the destination in `TemplateNavHost` by calling `settingsScreen()` inside the `NavHost`
block. Cross-feature navigation is wired here too: pass lambdas (for example
`onSettingsClick = { navController.navigateToSettings(...) }`) down into the screens that need
them, instead of letting one feature reference another feature's route.

### Step 10: Test

The convention plugins already put JUnit 4 and `kotlinx-coroutines-test` on every module's test
classpath, so no extra dependency configuration is needed. Because `:domain:*` is pure Kotlin, use
cases are tested on the JVM with a fake repository and no Robolectric:

```kotlin
class ObserveSettingsUseCaseTest {

    @Test
    fun `emits the repository value`() = runTest {
        val useCase = ObserveSettingsUseCase(FakeSettingsRepository(enabled = true))

        assertEquals(Settings(enabled = true), useCase().first())
    }
}
```

Because intents run in `viewModelScope`, a ViewModel test does need the Main dispatcher replaced:

```kotlin
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `toggle flips the flag`() = runTest {
        // Use cases are plain classes: build them over a fake repository rather than mocking them.
        val repository = FakeSettingsRepository(enabled = false)
        val viewModel = SettingsViewModel(
            ObserveSettingsUseCase(repository),
            SetEnabledUseCase(repository),
        )

        viewModel.onToggle()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.enabled)
    }
}
```

### Step 11: Review Checklist

Automated dependency checks are not wired up yet, so these items are reviewed by hand on every pull
request. Check them against the `build.gradle.kts` files, not against the source imports:

- [ ] No `:feature:*` module declares a `:data:*` dependency.
- [ ] No `:domain:*` module declares `:core:*`, `:data:*`, `:feature:*`, `:shared:*` or another
      `:domain:*`.
- [ ] No `:domain:*` module applies an Android, Hilt, Room or Compose plugin.
- [ ] No `:data:*` module applies `template.room` or declares `@Entity` / `@Dao`.
- [ ] Every new entity and DAO is registered in `:core:database`: listed in `entities`, exposed by
      an `AppDatabase` accessor, and provided by a `@Provides` in its Hilt module.
- [ ] Every new module (`:feature:*`, `:data:*`, `:domain:*`) is added to `app/build.gradle.kts`
      with `implementation`.
- [ ] No library module uses `api(project(...))` — `:app` holds the full graph instead.
- [ ] `:domain:*` imports `javax.inject.Inject`, never `jakarta.inject.Inject`.
- [ ] Domain models and Room entities are separate types with an explicit mapper.

## 4. Build & Test

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew test                   # unit tests across all modules
./gradlew connectedAndroidTest   # instrumented tests on a device/emulator
./gradlew :feature:main:test     # scope a task to one module
./gradlew :domain:main:test      # domain tests run on the plain JVM
```

The build requires **JDK 21**. Configuration cache is enabled with `problems=fail`, so a task that
is not configuration-cache compatible will fail the build rather than silently degrade it.

## 5. License

This project is licensed under the Apache License, Version 2.0. See the `LICENSE` file for details.

```
Copyright 2025 WangZhiYao

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
