# Android Application Template

This project is an opinionated Android application template designed to provide a robust and
scalable foundation for modern application development. The template enforces a strict multi-module,
unidirectional architecture, and centralises build configuration in Gradle convention plugins so
that adding a new module stays a matter of a few lines rather than a copy-pasted block of
boilerplate.

## 1. Tech Stack & Core Principles

* **Language**: [Kotlin](https://kotlinlang.org/) with Kotlin DSL for Gradle. Kotlin compilation
  comes from **AGP 9's built-in Kotlin support**, so modules apply only the Android plugin and no
  `org.jetbrains.kotlin.android` plugin; `jvmTarget` follows `compileOptions` (Java 21).
* **Build System**: Gradle 9.8.0 + AGP 9.4.1, using Version Catalogs (`gradle/libs.versions.toml`)
  for dependency versions and `build-logic` convention plugins for shared configuration.
* **Toolchain**: JDK 21, `compileSdk` / `targetSdk` 37, `minSdk` 28 — all declared once in
  `AppConfig`.
* **UI**: Built entirely with [Jetpack Compose](https://developer.android.com/jetpack/compose)
  (Material 3, versions pinned by the Compose BOM). A single `MainActivity`, no XML layouts and no
  ViewBinding.
* **Navigation**: [Navigation-Compose](https://developer.android.com/jetpack/compose/navigation)
  with **type-safe routes** — destinations are `@Serializable` objects, not string routes.
* **Architectural Pattern**: MVI (Model-View-Intent) combined with Clean Architecture, implementing
  a strict unidirectional data flow.
* **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
  (via KSP) manages the dependency graph.
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

The architecture rests on three ideas:

1. **Unidirectional dependencies.** Dependencies point one way only: `:app` -> `:feature:*` ->
   `:shared:*` / `:core:*`. Nothing lower in that chain may reference something higher.
2. **A feature is a vertical slice.** A feature module owns everything needed to ship its screens:
   Composable screens, MVI ViewModels, state and side-effect types, its navigation API, and its own
   data access. It pulls in a `:core:*` module only when it actually needs that capability.
3. **Shared code is split by kind.** Reusable *presentation* code lives in `:shared:*`; reusable
   *infrastructure* lives in `:core:*`. Neither knows anything about a specific feature.

The benefit is that changes stay local: a data source can be swapped inside `:core:*`, a screen can
be added or deleted as a single module, and the compiler enforces the boundaries instead of code
review.

### 2.2 Module Map

```
:app
├── :feature                 # Business features (vertical slices)
│   └── :main
├── :shared                  # Reusable presentation code
│   ├── :designsystem
│   └── :ui
└── :core                    # Infrastructure capabilities
    ├── :common
    ├── :network
    ├── :database-api
    └── :database-impl
```

> **Scaffold status**: `:app`, `:feature:main`, `:shared:*` and the DI qualifiers in `:core:common`
> contain code today. `:core:network`, `:core:database-api` and `:core:database-impl` are fully
> wired into the build but still empty — they are the slots for your network and persistence layers.

#### `:app` — Composition Root

* **Responsibility**: Assembles the application. It is the only module that knows about every
  feature.
* **Contents**: `TemplateApplication` (`@HiltAndroidApp`), the single `MainActivity`
  (`@AndroidEntryPoint`, `setContent { AppTheme { TemplateApp() } }`), the app-level navigation
  (`TemplateAppState` + `TemplateNavHost`), resources and the launcher icon.
* **Development Rules**: Keep it thin. It wires features together and owns the navigation graph;
  business logic belongs in a feature module. Applies `template.android.application` (which also
  brings Hilt and Compose) plus `template.serialization`.

#### `:feature:*` — Business Features

* **Responsibility**: A self-contained slice of the product, owning its UI, its state handling and
  its data access.
* **Contents**: Composable screens, MVI `ViewModels`, state / side-effect types, feature-local
  repositories and mappers, and the feature's navigation API (see
  [2.3 Dependency Rules](#23-dependency-rules)).
* **Development Rules**: Applies `template.android.feature`, which configures Hilt, Compose and
  dependencies on `:core:common` and `:shared:ui`, plus `template.serialization` for the type-safe
  route object. Add `:core:network` / `:core:database-api` explicitly when the feature needs them.
  **A feature must never depend on another feature** — share through `:core:*` contracts or promote
  the code to `:shared:*` instead.

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
    * MVI runtime: `MVIContainer`, `IntentContext`, `BaseMVIViewModel`, `BaseAndroidMVIViewModel`.
    * Compose collect helpers: `MVIContainer.collectState()` (state as lifecycle-aware `State`)
      and `MVIContainer.collectSideEffect { ... }` (one-shot effects collected on
      `flowWithLifecycle`).
* **Development Rules**: Depends only on `:shared:designsystem` (plus activity-compose /
  lifecycle-runtime-compose, exposed via `api`). Applies `template.compose` but not Hilt — this
  module defines contracts, it does not inject anything.

#### `:core:common` — Foundation

* **Responsibility**: The baseline every module can rely on.
* **Contents**: Coroutines and kotlinx.serialization (exposed via `api`), DataStore Preferences, DI
  qualifiers such as `@IODispatcher`, and the `CoroutineModule` that provides them.

#### `:core:network` — Network Infrastructure

* **Responsibility**: Everything needed to talk to a backend.
* **Contents**: Retrofit, the kotlinx.serialization converter, OkHttp and the logging interceptor.
  API service definitions belong here.
* **Development Rules**: Depends on `:core:common`. Applies `template.hilt` so services can be
  injected.

#### `:core:database-api` & `:core:database-impl` — Persistence

* **Responsibility**: `:core:database-api` is where persistence contracts (entities, DAOs, the
  database interface) are declared, and it exposes Room / Paging as `api`. `:core:database-impl`
  holds the actual `RoomDatabase` implementation and its DI module.
* **Development Rules**: `:core:database-impl` applies `template.room` (KSP + `room-compiler`),
  writes schemas to `schemas/`, and enables `room.generateKotlin`. Consumers depend on
  `:core:database-api`; `:core:database-impl` must be on the runtime classpath (usually through
  `:app`) for Hilt to see its module.

### 2.3 Dependency Rules

| From                   | May depend on                                                    | Must not depend on                    |
|------------------------|------------------------------------------------------------------|---------------------------------------|
| `:app`                 | `:feature:*`, `:shared:*`, `:core:*`                             | — (composition root)                  |
| `:feature:*`           | `:shared:*`, `:core:*`                                           | other `:feature:*`                    |
| `:shared:ui`           | `:shared:designsystem`                                           | `:feature:*`, `:core:*`               |
| `:shared:designsystem` | —                                                                | `:feature:*`, `:core:*`, `:shared:ui` |
| `:core:*`              | `:core:common` (`:core:database-impl` also `:core:database-api`) | `:feature:*`, `:shared:*`             |

Navigation follows the same rule. Each feature exposes its destinations through a small navigation
API in its `navigation` package — a `@Serializable` route object plus
`NavController.navigateTo*()` and `NavGraphBuilder.*Screen()` extensions — and `:app`'s
`TemplateNavHost` is the only place that assembles the graph. Because a feature must not depend on
another feature, cross-feature navigation is wired by `:app` through lambda parameters (for example
an `onSettingsClick` callback) rather than direct references to another feature's route.

### 2.4 Build Logic (`build-logic`)

All module configuration lives in `build-logic/convention` as convention plugins, so a module's
build file usually contains only its namespace and its own dependencies.

| Plugin ID                      | Composed of                                                       | Purpose                                                                                                        |
|--------------------------------|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `template.android.application` | `com.android.application` + `template.hilt` + `template.compose`  | Application setup: identity, SDK levels, Java 21, shared test dependencies                                     |
| `template.android.library`     | `com.android.library`                                             | Minimal library setup: SDK levels, Java 21, shared test dependencies                                           |
| `template.android.feature`     | `template.android.library` + `template.hilt` + `template.compose` | Feature setup, plus `:core:common` and `:shared:ui` dependencies                                               |
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

Following this architecture to add a new feature involves more explicit steps than dropping a class
into `:app`, but it is what keeps the module graph honest. The example below adds a
**"User Settings" feature**.

### Step 1: Create the Feature Module

1. Create the module directory `feature/settings/` with a `build.gradle.kts`:

```kotlin
plugins {
    id("template.android.feature")
    id("template.serialization")
}

android {
    namespace = "com.example.template.feature.settings"
}

dependencies {
    // Only the infrastructure this feature actually uses.
    implementation(project(":core:network"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
```

2. Register it in `settings.gradle.kts`:

```kotlin
include(":feature:settings")
```

Hilt, Compose, `:core:common` and `:shared:ui` already come from the convention plugin — do not
declare them again. `template.serialization` is needed because the feature declares a
`@Serializable` route object (Step 5). If the feature needs persistence, add `:core:database-api`
here and make sure `:core:database-impl` is on the runtime classpath (usually via `:app`).

### Step 2: Define State and Side Effects

Inside the new module, model the screen as an immutable state plus a one-shot side-effect type:

```kotlin
data class SettingsState(
    val enabled: Boolean = false,
)

sealed interface SettingsSideEffect {
    data class ShowMessage(val text: String) : SettingsSideEffect
}
```

### Step 3: Implement the ViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : BaseMVIViewModel<SettingsState, SettingsSideEffect>() {

    override val initialState = SettingsState()

    fun onToggle() = intent {
        reduce { copy(enabled = !enabled) }
        postSideEffect(SettingsSideEffect.ShowMessage("Settings updated"))
    }
}
```

`intent { ... }` runs in `viewModelScope`; inside it `state` reads the current state, `reduce`
produces the next one, and `postSideEffect` emits a one-shot event. Use `BaseAndroidMVIViewModel`
instead when the ViewModel needs a `Context`.

### Step 4: Implement the Screen

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

### Step 5: Expose the Feature's Navigation API

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

### Step 6: Provide Dependencies

Declare the repository contract in the feature and bind the implementation with a feature-local Hilt
module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
```

### Step 7: Wire It Into the App

1. Add the module dependency in `app/build.gradle.kts`:
   `implementation(project(":feature:settings"))`.
2. Register the destination in `TemplateNavHost` by calling `settingsScreen()` inside the `NavHost`
   block. Cross-feature navigation is wired here too: pass lambdas (for example
   `onSettingsClick = { navController.navigateToSettings(...) }`) down into the screens that need
   them, instead of letting one feature reference another feature's route.

### Step 8: Test

The convention plugins already put JUnit 4 and `kotlinx-coroutines-test` on every module's test
classpath, so no extra dependency configuration is needed. Because intents run in `viewModelScope`,
a ViewModel test does need the Main dispatcher replaced:

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
        val viewModel = SettingsViewModel(FakeSettingsRepository())

        viewModel.onToggle()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.enabled)
    }
}
```

## 4. Build & Test

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew test                   # unit tests across all modules
./gradlew connectedAndroidTest   # instrumented tests on a device/emulator
./gradlew :feature:main:test     # scope a task to one module
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
