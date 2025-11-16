# Android Application Template

This project is an opinionated Android application template designed to provide a robust and
scalable foundation for modern application development. The template enforces a strict multi-module,
layered architecture and integrates a comprehensive suite of industry-proven libraries and design
patterns to promote code quality, maintainability, and team collaboration efficiency.

## 1. Tech Stack & Core Principles

This template is built upon the following core technologies and design principles:

* **Language**: [Kotlin](https://kotlinlang.org/) (including Kotlin DSL for Gradle).
* **Architectural Pattern**: MVI (Model-View-Intent) combined with Clean Architecture, implementing
  a strict unidirectional data flow and layered design.
* **Dependency Injection
  **: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) is used to
  manage the dependency graph throughout the application.
* **Asynchronicity**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
  and [Flow](https://developer.android.com/kotlin/flow) are used to manage asynchronous operations
  and reactive data streams.
* **Networking**: [Retrofit](https://square.github.io/retrofit/)
  and [OkHttp](https://square.github.io/okhttp/) are responsible for network communication, with a
  logging interceptor configured.
* **Data Persistence**:
    * [Room](https://developer.android.com/training/data-storage/room): For local persistence of
      structured data.
    * [DataStore](https://developer.android.com/topic/libraries/architecture/datastore): For storing
      key-value pairs.
* **UI (View System)**: Built upon Android Views and ViewBinding, without Jetpack Compose.
* **Build System**: Gradle, utilizing Version Catalogs (`libs.versions.toml`) for centralized
  dependency version management.

## 2. Architectural Design

### 2.1 Design Philosophy

The core of this architecture is **Separation of Concerns (SoC)** and the **Dependency Inversion
Principle (DIP)**, which strictly partition the application into three layers: **UI Layer**, *
*Domain Layer**, and **Data Layer**.

* **The Dependency Rule**: Dependencies must be unidirectional, pointing inwards: `UI Layer` ->
  `Domain Layer` <- `Data Layer`. **The UI and Data layers must never depend on each other directly.
  **
* **Benefits of Layering**: Each layer has a clear responsibility, leading to highly decoupled,
  testable, and maintainable code. Any changes to data sources (e.g., switching from a database to a
  network source) are confined to the Data Layer, without affecting the UI Layer.

### 2.2 Module Details

```
:app
├── :core                 # Infrastructure Modules
│   ├── :common
│   ├── :database-api
│   └── :database-impl
├── :data                 # Data Layer
├── :domain               # Domain Layer
├── :feature              # UI (Presentation) Layer Modules
│   └── :main
└── :shared               # UI (Presentation) Layer Modules
    ├── :designsystem
    └── :ui
```

#### `:feature` & `:shared` (UI / Presentation Layer)

* **Responsibility**: The presentation layer of the application, responsible for displaying the user
  interface and handling user interactions.
* **Contents**:
    * `:feature:*`: Concrete business feature modules, containing `Fragments`, `ViewModels` (MVI),
      and feature-specific navigation graphs.
    * `:shared:designsystem`: Defines the basic visual specifications of the app (colors, themes,
      fonts).
    * `:shared:ui`: Contains reusable composite UI components and `Base` classes.
* **Development Rules**: **This layer depends only on the `:domain` module for data and business
  logic.** It should have no knowledge of data sources or implementation details.

#### `:domain` Module (Domain Layer)

* **Responsibility**: Defines the core business rules and data contracts of the application. This is
  the center of the architecture.
* **Contents**: `Repository` **interfaces**, pure Kotlin business data models, and optional
  `UseCases`.
* **Development Rules**: **This is a pure Kotlin module** and must not contain any Android framework
  dependencies, ensuring platform independence and high testability of the business logic.

#### `:data` Module (Data Layer)

* **Responsibility**: **Implements** the interfaces defined in the `:domain` layer. It is
  responsible for deciding where data comes from (network, database, cache) and for mapping and
  processing that data.
* **Contents**: `Repository` **implementations**, `DataSources` (local/remote), and `Mappers` for
  converting between different data models.
* **Development Rules**: Depends on the `:domain` layer to implement its interfaces and on the
  `:core` layer to access data manipulation infrastructure.

#### `:core` Module Group (Infrastructure Layer)

* **Responsibility**: Provides concrete technical implementations and common utilities for the
  `:data` layer.
* **Contents**:
    * `:core:common`: Common utilities, `DataStore` management, etc.
    * `:core:database-api` & `:core:database-impl`: The complete implementation of the Room
      database.
    * (Future extension) `:core:network`: The implementation for Retrofit.
* **Development Rules**: This is the lowest-level implementation detail, encapsulated by the `:data`
  layer.

## 3. Development Guide

Following this architecture to add new features, while involving more explicit steps, ensures
long-term code health. The following is an example of **adding a 'User Settings' feature**.

#### Step 1: Define the Domain Layer

In the `:domain` module, define the data contracts required for the new feature.

1. Create the business model: `data class UserSetting(...)`
2. Create the repository interface:
   `interface UserSettingRepository { fun getSettings(): Flow<UserSetting> }`

#### Step 2: Implement the Data Layer

In the `:data` and `:core` modules, provide the concrete implementation for the contracts from Step

1.

1. **Infrastructure**: If a new database table is needed, define the `SettingEntity` and
   `SettingDao` in `:core:database-api`, and register the `Entity` in `:core:database-impl`.
2. **Data Implementation**: In the `:data` module, create `UserSettingRepositoryImpl` that
   implements the `UserSettingRepository` interface from `:domain`. Inject the `SettingDao` or
   `ApiService` here and handle data mapping (e.g., Entity -> Domain Model).
3. **Dependency Injection**: In the `:data` module, use Hilt's `@Binds` annotation to bind the
   `UserSettingRepositoryImpl` to the `UserSettingRepository` interface.

#### Step 3: Implement the Presentation Layer (UI)

1. **Create Module**: Create a new Android Library module named `:feature:settings`.
2. **Configure Dependencies**:
    * In `:feature:settings/build.gradle.kts`, add dependencies on the `:domain` and `:shared:ui`
      modules.
    * In `:app/build.gradle.kts`, add a dependency on the new `:feature:settings` module.
3. **Create UI**: In `:feature:settings`, create `SettingsFragment` and `SettingsViewModel`. The
   ViewModel will **inject the `UserSettingRepository` interface from `:domain`** via Hilt and
   manage the UI state according to the MVI pattern.
4. **Integrate Navigation**: In the `:app` module's main navigation graph, add an action to navigate
   to the `:feature:settings` module's navigation graph.

## 4. License

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
