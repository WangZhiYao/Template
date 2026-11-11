# Android Application Template

**English** | [简体中文](README.zh-CN.md)

This repository is an opinionated Android application template designed to provide a robust and
scalable foundation for modern application development. The template enforces a strict
multi-module, unidirectional architecture, and centralises build configuration in Gradle
convention plugins so that adding a new module stays a matter of a few lines rather than a
copy-pasted block of boilerplate.

## 1. Tech Stack & Core Principles

* **Language**: [Kotlin](https://kotlinlang.org/) with Kotlin DSL for Gradle. Kotlin compilation
  comes from **AGP 9's built-in Kotlin support**, so modules apply only the Android plugin and no
  `org.jetbrains.kotlin.android` plugin; `jvmTarget` follows `compileOptions` (Java 21). Pure JVM
  modules (`:domain:*`) use the separate `org.jetbrains.kotlin.jvm` plugin instead.
* **Build System**: Gradle 9.9.1 + AGP 9.5.1, using Version Catalogs (`gradle/libs.versions.toml`)
  for dependency versions and `build-logic` convention plugins for shared configuration.
* **Toolchain**: JDK 21, `compileSdk` / `targetSdk` 37, `minSdk` 28 — all declared once in
  `AppConfig`.
* **UI**: Classic Android Views with **ViewBinding** (no `findViewById`, no Kotlin synthetics,
  no Compose). A single `MainActivity` hosting a `NavHostFragment`, Material themes via XML.
* **Navigation**: [Navigation Component](https://developer.android.com/guide/navigation) with an
  XML nav graph (`res/navigation/nav_graph.xml`) owned by `:app` — destinations are `<fragment>`
  entries, and cross-feature wiring happens in that graph.
* **Architectural Pattern**: Clean Architecture (domain / data / presentation) sliced vertically
  per business capability, with **MVI** (Model-View-Intent) on the presentation layer.
* **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
  (via KSP) manages the dependency graph. Domain UseCases use `javax.inject.Inject` — see
  [2.4 Implicit Contracts](#24-implicit-contracts).
* **Asynchronicity**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
  and [Flow](https://developer.android.com/kotlin/flow) for asynchronous work and reactive data
  streams.
* **Networking**: [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)
  with a logging interceptor and the kotlinx.serialization converter.
* **Data Persistence**:
    * [Room](https://developer.android.com/training/data-storage/room): structured local data
      (Kotlin codegen via KSP).
    * [DataStore](https://developer.android.com/topic/libraries/architecture/datastore):
      key-value storage.
    * [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview):
      paged data sources.
* **Testing**: JUnit 4, `kotlinx-coroutines-test`, AndroidX Test / Espresso and LeakCanary
  (debug builds only).
* **Gradle performance**: parallel builds, build cache, configuration cache (with `problems=fail`),
  and isolated projects are all enabled.

## 2. Architectural Design

### 2.1 Design Philosophy

The architecture rests on four principles:

1. **Unidirectional dependencies.** Dependencies point one way only: `:app` → `:feature:*` →
   `:domain:*`, `:data:*` implements `:domain:*`, and `:core:*` sits at the bottom. Nothing lower
   in that chain may reference something higher.
2. **Dependency inversion at the domain boundary.** `:domain:*` is **pure Kotlin**. It declares
   entities, Repository interfaces and UseCases, and knows nothing about Room, Retrofit, Android,
   Hilt or Views. `:data:*` implements those interfaces; `:feature:*` only ever sees contracts.
3. **A feature is a vertical slice with three layers.** Every business capability owns its
   `:domain:<name>`, `:data:<name>` and `:feature:<name>`. The slice is the unit of change; the
   layer is the unit of isolation.
4. **Shared code is split by kind.** Reusable *presentation* code lives in `:shared:*`; reusable
   *infrastructure* lives in `:core:*`. Neither knows anything about a specific business — with
   one deliberate exception: `:core:database` owns the complete Room schema and therefore knows
   every table.

The benefit is that changes stay local and boundaries are enforced by the compiler instead of
code review: swapping a data source touches a single `:data:*` module, deleting a capability is
deleting three modules, and a feature module physically cannot reach the database implementation
because it does not depend on it.

### 2.2 Module Map

```
:app                          # composition root
├── :feature                  # presentation (Fragment + ViewBinding + MVI ViewModel + navigation)
│   └── :main
├── :domain                   # business contracts (pure Kotlin)
│   └── :main
├── :data                     # implementations of the domain contracts
│   └── :main
├── :shared                   # reusable presentation code
│   ├── :designsystem
│   └── :ui
└── :core                     # infrastructure capabilities
    ├── :common
    ├── :network
    └── :database
```

#### `:app` — composition root

* **Responsibility**: assembles the application. It is the only module that knows the whole
  dependency graph.
* **Contents**: `TemplateApplication` (`@HiltAndroidApp`), the single `MainActivity`
  (`@AndroidEntryPoint`, extends `BaseActivity<ActivityMainBinding>` with toolbar +
  `NavHostFragment`), the app-level nav graph (`res/navigation/nav_graph.xml`), resources and the
  launcher icon.
* **Dependencies**: **every module in the graph** — `:feature:*`, `:data:*`, `:domain:*`,
  `:core:*` and `:shared:*`, one explicit line each. Do not assemble the list from constants or
  `forEach` loops, and do not introduce aggregate modules — explicit lines are IDE-indexable,
  clickable and diffable.

  Depending on the whole graph is exactly why every other module can use `implementation`. Hilt
  generates its component inside `:app`, and the generated code references types from all layers:
  the Repository interfaces of `:domain:*`, the DAOs and database module of `:core:database`, the
  ViewModels of `:feature:*`, the base classes of `:shared:ui`. As long as `:app` holds the full
  compile classpath, lower modules never need to export anything with `api`.
* **Development rule**: keep it thin. It wires features together, owns the nav graph and provides
  the entry point for Hilt bindings; business logic does not belong here.

#### `:feature:*` — presentation layer

* **Responsibility**: one screen group of the product: UI, state handling and orchestration.
* **Contents**: Fragment(s), ViewBinding layouts (`res/layout/*.xml`), MVI `ViewModel`,
  state / side-effect types.
* **Development rule**: apply `template.android.feature` (which brings Hilt, ViewBinding,
  `:core:common` and `:shared:ui`), then add `implementation(project(":domain:<name>"))` for the
  capabilities it consumes. **A feature never depends on `:data:*`**, and never on another
  `:feature:*`. Cross-capability orchestration happens in the ViewModel: inject two UseCases and
  combine them there instead of letting one domain module call another.

#### `:domain:*` — business contracts

* **Responsibility**: the business rules of a single capability, with no framework.
* **Contents**: domain models, Repository **interfaces**, UseCases. UseCases use
  `@Inject constructor` and are exposed as operator functions.
* **Development rule**: apply `template.kotlin.library` — a **pure JVM module**, no Android, no
  Hilt, no Room, no Views. Its only dependencies are:
  ```kotlin
  dependencies {
      api(libs.kotlinx.coroutines.core)   // UseCases are suspend or return Flow
      implementation(libs.javax.inject)   // only for @Inject constructor
  }
  ```
  Domain models are **not** Room entities: `:data:*` owns the mapping between the two. A
  `:domain:*` module must not depend on `:data:*`, `:core:*`, `:feature:*`, `:shared:*` or
  another `:domain:*`. Rules that genuinely span multiple capabilities stay in ViewModels as
  orchestration — there is no shared domain module to fall back to.

#### `:data:*` — implementation layer

* **Responsibility**: implements the Repository interfaces of exactly one domain capability.
* **Contents**: Repository implementations, entity ↔ domain mappers, Retrofit service
  definitions, and the Hilt modules binding the interfaces.
* **Development rule**: apply `template.android.library` + `template.hilt`, and **do not** apply
  `template.room` — the Room schema does not live here. Use plain `implementation` for
  `:domain:<name>` and for `:core:database` / `:core:network`: this is the only layer allowed to
  touch infrastructure, and since `:app` already depends on every module there is nothing to
  export. **`:data:*` modules must not depend on `:feature:*`, `:shared:*` or another
  `:data:*`.**

  Implementation classes must be `public`: Hilt generates the component in `:app` and needs to
  reference these types. Isolation comes from the dependency graph (`:feature:*` never depends on
  `:data:*`), not from `internal`. Classes that do not participate in DI at all (mappers) can and
  should be `internal`.

#### `:core:database` — persistence infrastructure

* **Responsibility**: owns the complete Room schema: the `@Database` class, every `@Entity`,
  every `@Dao`, Migrations, type converters, and the Hilt module providing the database and DAOs.
* **Contents**: the `RoomDatabase` subclass, all entities and DAOs, the `schemas/` export,
  `room.generateKotlin`, DI module.
* **Development rule**: apply `template.android.library` + `template.hilt` + `template.room`.
  Only `:data:*` modules depend on it.

  This module is the **single exception** to "infrastructure knows no business": it necessarily
  knows every table, because Room requires the `@Database` class to reference all entities and
  DAOs within one compilation unit.

  Putting an `@Entity` or `@Dao` into `:data:*` creates a circular dependency: `:core:database`
  would have to depend on `:data:*` to register the table, while `:data:*` depends on
  `:core:database` to use it. KSP cannot break this cycle either — it only generates `*_Impl`
  classes for `@Dao` sources inside the compiling module, so a DAO compiled in a module without
  `template.room` never gets an implementation at all. `:data:*` therefore injects DAOs and maps
  entities to domain models, and never declares them.

#### `:core:network` — network infrastructure

* **Responsibility**: everything needed to talk to a backend.
* **Contents**: Retrofit, the kotlinx.serialization converter, OkHttp and the logging
  interceptor. API service definitions live here.
* **Development rule**: depends on `:core:common`. Applies `template.hilt` so services can be
  injected. Only `:data:*` modules depend on it.

#### `:core:common` — baseline

* **Responsibility**: the baseline capabilities every module may depend on.
* **Contents**: Coroutines and kotlinx.serialization (exposed via `api`), DataStore Preferences,
  DI qualifiers such as `@IODispatcher`, and the `CoroutineModule` providing them.
* **Development rule**: must never depend on `:domain:*`, `:data:*`, `:feature:*` or `:shared:*`.
  This is the boundary most easily broken by accident ("the model is right there"), and once it
  is broken the domain is dragged back into the infrastructure layer.

#### `:shared:designsystem` — visual language

* **Responsibility**: defines the app's baseline look.
* **Contents**: Material themes (`values/themes.xml`, `values-night/themes.xml`), colors and
  dimens — the classic XML design system.
* **Development rule**: exposes `appcompat`, `material`, `activity-ktx` and `fragment-ktx` via
  `api` so every consumer resolves those artifacts through this module. It deliberately applies
  **no** Hilt, bringing in neither a DI framework nor a KSP processor.

#### `:shared:ui` — UI toolbox

* **Responsibility**: the reusable UI building blocks shared by every feature.
* **Contents**:
    * `BaseActivity<VB>` / `BaseFragment<VB>`: ViewBinding lifecycle owners with a nullable-safe
      `binding` accessor.
    * The MVI runtime: `MVIContainer`, `IntentContext`, `BaseMVIViewModel`, and
      `BaseAndroidMVIViewModel` — the `AndroidViewModel` variant, kept for compatibility only;
      prefer injecting `@ApplicationContext`.
    * Collection helper: `MVIContainer.observe(lifecycleOwner, state = ..., sideEffect = ...)`,
      which collects state and one-shot side effects under `repeatOnLifecycle(STARTED)`.
* **Development rule**: depends only on `:shared:designsystem` (plus navigation / lifecycle
  artifacts exposed via `api`). Applies `template.viewbinding` but not Hilt — this module defines
  contracts and injects nothing.

### 2.3 Dependency Rules

| From                    | May depend on                                     | Must not depend on                                                 |
|-------------------------|---------------------------------------------------|--------------------------------------------------------------------|
| `:app`                  | every module (`:feature:*`, `:data:*`, `:domain:*`, `:core:*`, `:shared:*`) | — (composition root)                        |
| `:feature:*`            | `:domain:*`, `:shared:*`, `:core:common`          | `:data:*`, other `:feature:*`, `:core:database`, `:core:network`   |
| `:domain:*`             | coroutines + `javax.inject` only                  | `:data:*`, `:core:*`, `:feature:*`, `:shared:*`, other `:domain:*` |
| `:data:*`               | its own `:domain:*`, `:core:*`                    | `:feature:*`, `:shared:*`, other `:data:*`                         |
| `:core:*`               | `:core:common`                                    | `:domain:*`, `:data:*`, `:feature:*`, `:shared:*`                  |
| `:shared:ui`            | `:shared:designsystem`                            | `:core:*`, `:domain:*`, `:data:*`, `:feature:*`                    |
| `:shared:designsystem`  | —                                                 | `:core:*`, `:domain:*`, `:data:*`, `:feature:*`, `:shared:ui`      |

**Two of these rules must be checked by hand**, because they can be broken by adding a single
line to a `build.gradle.kts` with no build-time error to stop it:

1. No `:feature:*` module declares a `:data:*` dependency.
2. No `:domain:*` module declares `:core:*`, `:data:*`, `:feature:*` or another `:domain:*`.

### 2.4 Implicit Contracts

The following conventions are invisible in build scripts and fail in very confusing ways when
violated:

* **`:app` must depend on every module, not only the data modules.** Hilt aggregates every
  `@InstallIn` module into the component generated in `:app`, and the generated code references
  types from all layers. A missing module fails at **compile time** (`[Dagger/MissingBinding]`,
  or "cannot access class" when the type is not on the classpath) — it never leaks to runtime,
  but the error points at a consumer instead of the missing dependency.
* **Lower modules need no `api`.** Because `:app` already sees the whole graph, `:data:*` and
  `:feature:*` declare dependencies with `implementation`. Using `api` in a library module is a
  sign that a module is missing from `:app`'s dependency list.
* **`@InstallIn` modules must be `implementation`, never `runtimeOnly`.** They have to sit on
  `:app`'s **compile** classpath for the Hilt processor to see them.
* **UseCases in `:domain:*` use `javax.inject.Inject`, never `jakarta.inject.Inject`.** Dagger
  only recognises the `javax` namespace; `jakarta` annotations are silently ignored and surface
  later as "cannot be provided without an @Inject constructor".
* **Domain models are not Room entities.** `:data:*` owns the mapping in both directions.
* **All `@Entity` and `@Dao` types live in `:core:database`; `:data:*` declares none.** A DAO in
  `:data:*` is either invisible to the `@Database` class or creates a circular dependency; and a
  DAO compiled without `template.room` never gets its generated implementation.
* **Overriding `initialState` must use the getter form
  (`override val initialState: X get() = X()`), never a field with an initializer.**
  Base-class property initializers run before the subclass assigns its fields, so the field
  form captures the not-yet-initialised `null` into the `MutableStateFlow` (generic erasure
  hides it from the type system) and the first render crashes with a `NullPointerException`.

### 2.5 Build Logic (`build-logic`)

All module configuration lives in `build-logic/convention` as convention plugins, so a module's
build script usually contains nothing but its namespace and its own dependencies.

| Plugin ID                     | Composed of                                                       | Purpose                                                                     |
|-------------------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `template.android.application`| `com.android.application` + `template.hilt` + `template.viewbinding` | App configuration: identity, SDK versions, Java 21, shared test dependencies |
| `template.android.library`    | `com.android.library`                                             | Minimal library configuration: SDK versions, Java 21, shared test dependencies |
| `template.android.feature`    | `template.android.library` + `template.hilt` + `template.viewbinding` | Feature configuration, plus the `:core:common` and `:shared:ui` dependencies |
| `template.kotlin.library`     | `org.jetbrains.kotlin.jvm`                                        | Pure JVM module for `:domain:*`: Java 21 target, shared test dependencies, no Android artifacts |
| `template.viewbinding`        | `buildFeatures.viewBinding`                                       | ViewBinding code generation for modules with XML layouts                     |
| `template.hilt`               | `com.google.devtools.ksp` + `com.google.dagger.hilt.android`      | `hilt-android` and its KSP compiler                                          |
| `template.room`               | `com.google.devtools.ksp` + `androidx.room`                       | `room-runtime` / `room-ktx` and its KSP compiler                             |
| `template.serialization`      | `org.jetbrains.kotlin.plugin.serialization`                       | Kotlin serialization compiler plugin, opted into per module when needed      |

**Capabilities are opt-in.** `template.android.library` stays minimal; modules add
`template.hilt`, `template.viewbinding` or `template.room` only when needed. Only
`template.android.feature` bundles Hilt and ViewBinding, because feature modules are numerous
and uniform in shape.

**`AppConfig` is the single source of truth** for app id, versions, SDK levels and the Java
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

Modules no longer declare `compileSdk`, `minSdk` or `versionCode` themselves — change the value
here and every module follows.

Two more AGP 9 notes:

* Release builds currently set `optimization { enable = false }`, so R8 optimization stays off
  until you opt in.
* Keep rules live in `src/main/keepRules/*.keep` (see `app/src/main/keepRules/rules.keep`);
  AGP merges every file in that directory into one ruleset handed to R8.

## 3. Development Guide

Adding a business capability means creating three modules. The example below adds a
**"settings"** capability.

### Step 1: Create the domain module

Create the `domain/settings/` directory with its `build.gradle.kts`:

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

Declare the contract — a model, a Repository interface and two UseCases:

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

Keep UseCases atomic: one business operation, unaware of other capabilities. Combining two
UseCases is a ViewModel's job.

### Step 2: Register the schema in `:core:database`

Room requires the `@Database` class to see all entities and DAOs, which is why the schema lives
in `:core:database` and never in a data module (see 2.4). Add the table there:

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

Expose the DAO through that module's Hilt module:

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

Every new capability does three things here: add the entity to `entities`, add a DAO accessor on
`AppDatabase`, and add a `@Provides`. When the schema changes, bump `version` and write a
Migration.

### Step 3: Create the data module

Create the `data/settings/` directory with its `build.gradle.kts`:

```kotlin
plugins {
    id("template.android.library")
    id("template.hilt")
}

android {
    namespace = "com.example.template.data.settings"
}

dependencies {
    // implementation, not api: :app depends on every module directly, nothing needs re-export.
    implementation(project(":domain:settings"))
    implementation(project(":core:database"))
}
```

Note there is no `template.room`: the schema lives in `:core:database`, and this module has
nothing for KSP to generate. Implement the Repository and map entities to domain models:

```kotlin
// :data:settings
// Must be public: Hilt generates the component in :app and references this type.
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

### Step 4: Create the feature module

Create the `feature/settings/` directory with its `build.gradle.kts`:

```kotlin
plugins {
    id("template.android.feature")
}

android {
    namespace = "com.example.template.feature.settings"
}

dependencies {
    // Contracts only — never :data:settings.
    implementation(project(":domain:settings"))
}
```

Hilt, ViewBinding, `:core:common` and `:shared:ui` already come from the convention plugin — do
not declare them again.

### Step 5: Define state and side effects

Inside the feature module, model the screen as immutable state plus one-shot side effects:

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

    // The getter form is mandatory, see 2.4 Implicit contracts
    override val initialState: SettingsState get() = SettingsState()

    init {
        intent {
            observeSettings().collect { settings ->
                reduce { copy(enabled = settings.enabled) }
            }
        }
    }

    fun onToggle(enabled: Boolean) = intent {
        setEnabled(enabled)
        postSideEffect(SettingsSideEffect.ShowMessage("Settings updated"))
    }
}
```

`intent { ... }` runs in `viewModelScope`; inside it `state` reads the current state, `reduce`
produces the next state, and `postSideEffect` emits a one-shot event.

When a ViewModel needs a `Context`, inject it instead of switching to
`BaseAndroidMVIViewModel`:

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observeSettings: ObserveSettingsUseCase,
) : BaseMVIViewModel<SettingsState, SettingsSideEffect>() { /* ... */ }
```

`BaseAndroidMVIViewModel` is an `AndroidViewModel` variant that exposes `application` as a public
property to every caller. The qualifier provides the same instance without widening the
ViewModel's visible interface, so the base class has no advantage — prefer injection.

Cross-capability orchestration lives here, not in a domain module:

```kotlin
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val getUser: GetUserUseCase,          // :domain:user
    private val createOrder: CreateOrderUseCase,  // :domain:order
) : BaseMVIViewModel<CheckoutState, CheckoutSideEffect>() { /* ... */ }
```

### Step 7: Register the destination in the nav graph

The app-level nav graph in `:app` owns every destination. Add the fragment there:

```xml
<!-- app/src/main/res/navigation/nav_graph.xml -->
<fragment
    android:id="@+id/fragment_settings"
    android:name="com.example.template.feature.settings.SettingsFragment"
    android:label="@string/app_name"
    tools:layout="@layout/fragment_settings" />
```

Navigate programmatically with `navController.navigate(R.id.fragment_settings)`. Cross-feature
navigation is wired in `:app` too — pass callbacks down to the screens that need them instead of
letting one feature reference another.

### Step 8: Implement the Fragment and layout

Follow the `MainFragment` / `fragment_main.xml` pattern in `:feature:main`: a stateful Fragment
that owns the ViewModel and renders through ViewBinding.

```kotlin
@AndroidEntryPoint
class SettingsFragment :
    BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // State and one-shot side effects are collected only while STARTED.
        viewModel.observe(
            viewLifecycleOwner,
            state = ::renderState,
            sideEffect = ::handleSideEffect,
        )

        binding.settingsSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.onToggle(checked)
        }
    }

    private fun renderState(state: SettingsState) {
        binding.settingsSwitch.isChecked = state.enabled
    }

    private suspend fun handleSideEffect(sideEffect: SettingsSideEffect) {
        when (sideEffect) {
            is SettingsSideEffect.ShowMessage ->
                Snackbar.make(binding.root, sideEffect.text, Snackbar.LENGTH_SHORT).show()
        }
    }
}
```

```xml
<!-- fragment_settings.xml -->
<com.google.android.material.materialswitch.MaterialSwitch
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/settings_switch"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/app_name" />
```

Use `viewLifecycleOwner` (not `this`) when observing from a Fragment, so collection stops when
the view is destroyed.

### Step 9: Wire the app

Add the new modules to `app/build.gradle.kts`. `:app` depends on the whole graph (see 2.4), so
the list mirrors the module tree one-to-one:

```kotlin
dependencies {
    // Features
    implementation(project(":feature:main"))
    implementation(project(":feature:settings"))

    // Data implementations
    implementation(project(":data:main"))
    implementation(project(":data:settings"))

    // Domain contracts — Hilt's generated component references these interfaces
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

### Step 10: Tests

The convention plugins already put JUnit 4 and `kotlinx-coroutines-test` on every module's test
classpath. Because `:domain:*` is pure Kotlin, UseCases are tested on the JVM against fake
repositories — no Robolectric:

```kotlin
class ObserveSettingsUseCaseTest {

    @Test
    fun `emits the repository value`() = runTest {
        val useCase = ObserveSettingsUseCase(FakeSettingsRepository(enabled = true))

        assertEquals(Settings(enabled = true), useCase().first())
    }
}
```

Because `intent` runs in `viewModelScope`, ViewModel tests must replace the Main dispatcher:

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
        // UseCases are plain classes: build them on a fake repository instead of mocking.
        val repository = FakeSettingsRepository(enabled = false)
        val viewModel = SettingsViewModel(
            ObserveSettingsUseCase(repository),
            SetEnabledUseCase(repository),
        )

        viewModel.onToggle(true)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.enabled)
    }
}
```

### Step 11: Review checklist

There is no automated dependency check yet, so these items are reviewed by hand in every pull
request. Check the `build.gradle.kts` files, not the source imports:

- [ ] No `:feature:*` module declares a `:data:*` dependency.
- [ ] No `:domain:*` module declares `:core:*`, `:data:*`, `:feature:*`, `:shared:*` or another
      `:domain:*`.
- [ ] No `:domain:*` module applies the Android, Hilt, Room or ViewBinding plugins.
- [ ] No `:data:*` module applies `template.room` or declares `@Entity` / `@Dao`.
- [ ] Every new entity and DAO is registered in `:core:database`: added to `entities`, given an
      accessor on `AppDatabase`, and provided via `@Provides` in the Hilt module.
- [ ] Every new module (`:feature:*`, `:data:*`, `:domain:*`) is listed in
      `app/build.gradle.kts` with `implementation`.
- [ ] No library module uses `api(project(...))` — the full graph is held by `:app`.
- [ ] `:domain:*` imports `javax.inject.Inject`, never `jakarta.inject.Inject`.
- [ ] Domain models and Room entities are distinct types with explicit mappers.
- [ ] Every `initialState` override uses the getter form (see 2.4).

## 4. Build & Test

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew test                   # run the unit tests of every module
./gradlew connectedAndroidTest   # run instrumented tests on a device / emulator
./gradlew :feature:main:test     # run a single module's tasks
./gradlew :domain:main:test      # domain tests run directly on the JVM
```

Building requires **JDK 21**. The configuration cache is enabled with `problems=fail`, so any
task that is incompatible with it fails the build instead of silently degrading.

## 5. License

This project is licensed under the Apache License 2.0. See the `LICENSE` file.

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
