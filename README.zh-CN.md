# Android 应用模板

[English](README.md) | **简体中文**

本仓库是一份带有明确主张的 Android 应用模板，旨在为现代应用开发提供稳健、可扩展的基础。
模板强制采用严格的多模块单向架构，并把构建配置集中到 Gradle 约定插件中，因此新增一个模块
只需几行声明，而不必复制粘贴一大段样板代码。

## 1. 技术栈与核心原则

* **语言**：[Kotlin](https://kotlinlang.org/)，Gradle 使用 Kotlin DSL。Kotlin 编译由
  **AGP 9 内置的 Kotlin 支持**提供，因此模块只应用 Android 插件，不应用
  `org.jetbrains.kotlin.android` 插件；`jvmTarget` 跟随 `compileOptions`（Java 21）。纯 JVM
  模块（`:domain:*`）改用独立的 `org.jetbrains.kotlin.jvm` 插件。
* **构建系统**：Gradle 9.9.1 + AGP 9.5.1，使用 Version Catalog（`gradle/libs.versions.toml`）
  管理依赖版本，使用 `build-logic` 约定插件统一配置。
* **工具链**：JDK 21，`compileSdk` / `targetSdk` 37，`minSdk` 28 —— 全部在 `AppConfig` 中
  声明一次。
* **UI**：经典 View 体系 + **ViewBinding**（没有 `findViewById`，没有 Compose）。唯一的
  `MainActivity` 挂载 `NavHostFragment`，Material 主题由 XML 定义。
* **导航**：[Navigation Component](https://developer.android.com/guide/navigation) 配合
  `:app` 持有的 XML 导航图（`res/navigation/nav_graph.xml`）—— 目的地是 `<fragment>` 条目，
  跨 feature 接线也发生在这张图里。
* **架构模式**：Clean Architecture（domain / data / presentation）按业务垂直切分，表现层
  采用 **MVI**（Model-View-Intent）。
* **依赖注入**：[Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
  （通过 KSP）管理依赖图。Domain 层的 useCase 使用 `javax.inject.Inject` —— 见
  [2.4 隐式契约](#24-隐式契约)。
* **异步**：[Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) 与
  [Flow](https://developer.android.com/kotlin/flow)，用于异步任务和响应式数据流。
* **网络**：[Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)，
  带日志拦截器和 kotlinx.serialization 转换器。
* **数据持久化**：
    * [Room](https://developer.android.com/training/data-storage/room)：结构化本地数据
      （通过 KSP 生成 Kotlin 代码）。
    * [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)：键值
      存储。
    * [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)：
      分页数据源。
* **测试**：JUnit 4、`kotlinx-coroutines-test`、AndroidX Test / Espresso，以及 LeakCanary
  （仅 debug 构建）。
* **Gradle 性能**：并行构建、构建缓存、配置缓存（`problems=fail`）和 isolated projects
  全部开启。

## 2. 架构设计

### 2.1 设计理念

这套架构建立在四个原则之上：

1. **单向依赖。** 依赖只朝一个方向：`:app` → `:feature:*` → `:domain:*`，`:data:*` 实现
   `:domain:*`，`:core:*` 位于最底层。链条中较低层不得引用较高层。
2. **在 domain 边界做依赖倒置。** `:domain:*` 是**纯 Kotlin**。它声明实体、Repository 接口
   和 UseCase，对 Room、Retrofit、Android、Hilt、View 一无所知。`:data:*` 实现这些接口，
   `:feature:*` 永远只看到契约。
3. **一个功能是一个垂直切片，切成三层。** 每个业务能力拥有自己的 `:domain:<name>`、
   `:data:<name>` 和 `:feature:<name>`。切片是变更的单位，层是隔离的单位。
4. **共享代码按种类拆分。** 可复用的**表现层**代码放 `:shared:*`；可复用的**基础设施**放
   `:core:*`。两者都不知道具体业务的存在，只有一个刻意的例外：`:core:database` 拥有完整的
   Room 表结构，因此知道每一张表。

这样做的好处是变更保持局部，边界由编译器而不是代码评审来保证：替换数据源只需改动一个
`:data:*` 模块，删除一个业务能力只需删掉三个模块，而 feature 模块从物理上就无法触及数据库
实现，因为它根本没有依赖。

### 2.2 模块地图

```
:app                          # 组装根（Composition Root）
├── :feature                  # 表现层（Fragment + ViewBinding + MVI ViewModel + 导航）
│   └── :main
├── :domain                   # 业务契约（纯 Kotlin）
│   └── :main
├── :data                     # domain 契约的实现
│   └── :main
├── :shared                   # 可复用的表现层代码
│   ├── :designsystem
│   └── :ui
└── :core                     # 基础设施能力
    ├── :common
    ├── :network
    └── :database
```

#### `:app` —— 组装根

* **职责**：组装应用。它是唯一了解整张依赖图的模块。
* **内容**：`TemplateApplication`（`@HiltAndroidApp`）、唯一的 `MainActivity`
  （`@AndroidEntryPoint`，继承 `BaseActivity<ActivityMainBinding>`，带 toolbar +
  `NavHostFragment`）、应用级导航图（`res/navigation/nav_graph.xml`）、资源与启动图标。
* **依赖**：**图中的每一个模块** —— `:feature:*`、`:data:*`、`:domain:*`、`:core:*` 和
  `:shared:*`，一个模块一行显式声明。不要用常量或 `forEach` 循环拼列表，也不要引入聚合模块
  —— 显式的一行才能被 IDE 索引、点击跳转，也更容易 diff。

  依赖整张图正是其他模块都能用 `implementation` 的原因。Hilt 的组件在 `:app` 内部生成，
  而生成代码引用了所有层的类型：`:domain:*` 的 Repository 接口、`:core:database` 的 DAO 和
  数据库模块、`:feature:*` 的 ViewModel、`:shared:ui` 的基类。只要 `:app` 持有完整的编译类
  路径，下层模块就不需要用 `api` 再导出任何东西。
* **开发规则**：保持轻薄。它负责把 feature 串起来、持有导航图、提供 Hilt 绑定的入口；
  业务逻辑不属于这里。

#### `:feature:*` —— 表现层

* **职责**：产品中的一个界面组：UI、状态处理与编排。
* **内容**：Fragment、ViewBinding 布局（`res/layout/*.xml`）、MVI `ViewModel`、状态 /
  副作用类型。
* **开发规则**：应用 `template.android.feature`（自带 Hilt、ViewBinding、`:core:common` 和
  `:shared:ui`），再为它消费的能力添加 `implementation(project(":domain:<name>"))`。
  **feature 绝不能依赖 `:data:*`**，也绝不能依赖另一个 `:feature:*`。跨业务编排发生在
  ViewModel 里：注入两个 UseCase 在那里组合，而不是让一个 domain 模块去调用另一个。

#### `:domain:*` —— 业务契约

* **职责**：单个业务能力的业务规则，不依赖任何框架。
* **内容**：Domain 模型、Repository **接口**、UseCase。UseCase 使用 `@Inject constructor`
  并以便于调用的操作符形式暴露。
* **开发规则**：应用 `template.kotlin.library` —— 一个**纯 JVM 模块**，没有 Android、没有
  Hilt、没有 Room、没有 View。它的全部依赖只有：
  ```kotlin
  dependencies {
      api(libs.kotlinx.coroutines.core)   // UseCase 是 suspend 或返回 Flow
      implementation(libs.javax.inject)   // 只用于 @Inject constructor
  }
  ```
  Domain 模型**不是** Room 实体：`:data:*` 负责在两者之间映射。`:domain:*` 模块不得依赖
  `:data:*`、`:core:*`、`:feature:*`、`:shared:*` 或另一个 `:domain:*`。真正跨越多个能力的
  规则只能以编排的形式留在 ViewModel 里 —— 这里没有共享的 domain 模块可以退守。

#### `:data:*` —— 实现层

* **职责**：实现且仅实现一个 domain 能力的 Repository 接口。
* **内容**：Repository 实现、实体 ↔ domain 模型映射器、Retrofit 服务定义，以及绑定这些接口的
  Hilt 模块。
* **开发规则**：应用 `template.android.library` + `template.hilt`，**不应用** `template.room`
  —— Room 表结构不在这里。对 `:domain:<name>` 以及 `:core:database` / `:core:network` 一律用
  `implementation`：这里是唯一允许触碰基础设施的层，而 `:app` 已经依赖了每一个模块，所以
  不需要导出任何东西。**`:data:*` 模块不得依赖 `:feature:*`、`:shared:*` 或另一个
  `:data:*`。**

  实现类必须是 `public`：Hilt 在 `:app` 中生成组件，需要引用这些类型。隔离来自依赖图
  （`:feature:*` 从不依赖 `:data:*`），而不是 `internal`。完全不参与依赖注入的类（映射器）
  可以也应该声明为 `internal`。

#### `:core:database` —— 持久化基础设施

* **职责**：拥有完整的 Room 表结构：`@Database` 类、所有 `@Entity`、所有 `@Dao`、Migration、
  类型转换器，以及提供数据库和各 DAO 的 Hilt 模块。
* **内容**：`RoomDatabase` 子类、全部实体与 DAO、`schemas/` 输出、`room.generateKotlin`、DI
  模块。
* **开发规则**：应用 `template.android.library` + `template.hilt` + `template.room`。只有
  `:data:*` 模块依赖它。

  这个模块是"基础设施不了解业务"原则的**唯一例外**：它必然知道每一张表，因为 Room 要求
  `@Database` 类在同一个编译单元内引用所有实体和 DAO。

  如果把 `@Entity` 或 `@Dao` 放进 `:data:*`，会形成循环依赖：`:core:database` 为了注册这张表
  必须依赖 `:data:*`，而 `:data:*` 又要依赖 `:core:database` 才能使用它。KSP 也无法打破这个
  循环 —— 它只为自身编译单元内的 `@Dao` 源码生成 `*_Impl` 类，在没有 `template.room` 的模块
  里编译的 DAO 根本不会生成实现。因此 `:data:*` 只注入 DAO 并把实体映射成 domain 模型，从不
  声明它们。

#### `:core:network` —— 网络基础设施

* **职责**：与后端通信所需的一切。
* **内容**：Retrofit、kotlinx.serialization 转换器、OkHttp 与日志拦截器。API 服务定义放在
  这里。
* **开发规则**：依赖 `:core:common`。应用 `template.hilt` 以便服务可被注入。只有 `:data:*`
  模块依赖它。

#### `:core:common` —— 基础层

* **职责**：每个模块都可以依赖的基线能力。
* **内容**：Coroutines 与 kotlinx.serialization（通过 `api` 暴露）、DataStore Preferences、
  诸如 `@IODispatcher` 的 DI 限定符，以及提供它们的 `CoroutineModule`。
* **开发规则**：绝不能依赖 `:domain:*`、`:data:*`、`:feature:*` 或 `:shared:*`。这是最容易
  被无意打破的边界（"模型就在手边"），而一旦打破，domain 就被重新拖回基础设施层。

#### `:shared:designsystem` —— 视觉语言

* **职责**：定义应用的基础视觉规范。
* **内容**：Material 主题（`values/themes.xml`、`values-night/themes.xml`）、颜色与尺寸
  —— 经典的 XML 设计系统。
* **开发规则**：以 `api` 方式暴露 `appcompat`、`material`、`activity-ktx` 和 `fragment-ktx`，
  使所有消费方都通过这个模块解析这些产物。它刻意**不**应用 Hilt，从而不引入 DI 框架和 KSP
  处理器。

#### `:shared:ui` —— UI 工具箱

* **职责**：所有 feature 共用的可复用 UI 构件。
* **内容**：
    * `BaseActivity<VB>` / `BaseFragment<VB>`：ViewBinding 生命周期持有者，提供空安全的
      `binding` 访问器。
    * MVI 运行时：`MVIContainer`、`IntentContext`、`BaseMVIViewModel`，以及
      `BaseAndroidMVIViewModel` —— `AndroidViewModel` 变体，仅为兼容保留；优先注入
      `@ApplicationContext`。
    * 收集辅助：`MVIContainer.observe(lifecycleOwner, state = ..., sideEffect = ...)`，基于
      `repeatOnLifecycle(STARTED)` 收集状态与一次性副作用。
* **开发规则**：只依赖 `:shared:designsystem`（加上通过 `api` 暴露的 navigation / lifecycle
  产物）。应用 `template.viewbinding` 但不应用 Hilt —— 这个模块定义契约，不注入任何东西。

### 2.3 依赖规则

| 从                     | 可以依赖                                        | 禁止依赖                                                        |
|------------------------|-------------------------------------------------|-----------------------------------------------------------------|
| `:app`                 | 所有模块（`:feature:*`、`:data:*`、`:domain:*`、`:core:*`、`:shared:*`） | ——（组装根）                                                     |
| `:feature:*`           | `:domain:*`、`:shared:*`、`:core:common`        | `:data:*`、其他 `:feature:*`、`:core:database`、`:core:network`  |
| `:domain:*`            | 仅 coroutines + `javax.inject`                  | `:data:*`、`:core:*`、`:feature:*`、`:shared:*`、其他 `:domain:*` |
| `:data:*`              | `:domain:*`（自己的）、`:core:*`                | `:feature:*`、`:shared:*`、其他 `:data:*`                        |
| `:core:*`              | `:core:common`                                  | `:domain:*`、`:data:*`、`:feature:*`、`:shared:*`                |
| `:shared:ui`           | `:shared:designsystem`                          | `:core:*`、`:domain:*`、`:data:*`、`:feature:*`                  |
| `:shared:designsystem` | ——                                              | `:core:*`、`:domain:*`、`:data:*`、`:feature:*`、`:shared:ui`    |

**其中两条是必须人工检查的项**，因为它们只需在 `build.gradle.kts` 里加一行就会被打破，而
构建过程不会有任何报错：

1. `:feature:*` 模块不得声明任何 `:data:*` 依赖。
2. `:domain:*` 模块不得声明 `:core:*`、`:data:*`、`:feature:*` 或另一个 `:domain:*`。

### 2.4 隐式契约

以下约定在构建脚本中不可见，一旦弄错会产生非常难懂的错误：

* **`:app` 必须依赖每一个模块，而不只是 data 模块。** Hilt 把所有 `@InstallIn` 模块聚合进
  `:app` 中生成的组件，而生成代码引用了所有层的类型。缺少某个模块会在**编译期**报错
  （`[Dagger/MissingBinding]`，或类型不在类路径时的 "cannot access class"）—— 它不会漏到
  运行期，但报错位置指向消费方，而不是缺失的依赖。
* **下层模块不需要 `api`。** 因为 `:app` 已经能看到整张图，`:data:*` 和 `:feature:*` 用
  `implementation` 声明依赖。在库模块里使用 `api`，意味着有一个模块没出现在 `:app` 的依赖
  清单里。
* **`@InstallIn` 模块必须用 `implementation`，不能用 `runtimeOnly`。** 它们必须位于 `:app`
  的**编译**类路径上，Hilt 处理器才能看到。
* **`:domain:*` 的 UseCase 使用 `javax.inject.Inject`，绝不能用 `jakarta.inject.Inject`。**
  Dagger 只识别 `javax` 命名空间；`jakarta` 注解会被静默忽略，最终表现为
  "cannot be provided without an @Inject constructor"。
* **Domain 模型不是 Room 实体。** 双向映射由 `:data:*` 负责。
* **所有 `@Entity` 和 `@Dao` 都由 `:core:database` 持有，`:data:*` 一个都不声明。** 放在
  `:data:*` 的 DAO 要么对 `@Database` 类不可见，要么造成循环依赖；而且在没有 `template.room`
  的模块里编译的 `@Dao` 不会生成实现。
* **覆盖 `initialState` 必须用 getter 形式（`override val initialState: X get() = X()`），
  绝不能用带初始化器的字段形式。** 基类的属性初始化器先于子类字段赋值执行，字段形式会把
  尚未初始化的 `null` 装进 `MutableStateFlow`（泛型擦除让它逃过类型检查），首次渲染时即以
  `NullPointerException` 崩溃。

### 2.5 构建逻辑（`build-logic`）

所有模块配置都以约定插件的形式放在 `build-logic/convention` 中，因此模块的构建脚本通常只
包含命名空间和它自己的依赖。

| 插件 ID                        | 由什么组成                                                        | 用途                                                                                    |
|--------------------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `template.android.application` | `com.android.application` + `template.hilt` + `template.viewbinding` | 应用配置：标识、SDK 版本、Java 21、共享测试依赖                                      |
| `template.android.library`     | `com.android.library`                                             | 最小库配置：SDK 版本、Java 21、共享测试依赖                                              |
| `template.android.feature`     | `template.android.library` + `template.hilt` + `template.viewbinding` | Feature 配置，并附带 `:core:common` 与 `:shared:ui` 依赖                                 |
| `template.kotlin.library`      | `org.jetbrains.kotlin.jvm`                                        | 供 `:domain:*` 使用的纯 JVM 模块：Java 21 目标、共享测试依赖、不含 Android 产物          |
| `template.viewbinding`         | `buildFeatures.viewBinding`                                       | 为含 XML 布局的模块生成 ViewBinding                                                      |
| `template.hilt`                | `com.google.devtools.ksp` + `com.google.dagger.hilt.android`      | `hilt-android` 及其 KSP 编译器                                                           |
| `template.room`                | `com.google.devtools.ksp` + `androidx.room`                       | `room-runtime` / `room-ktx` 及其 KSP 编译器                                              |
| `template.serialization`       | `org.jetbrains.kotlin.plugin.serialization`                       | Kotlin 序列化编译器插件，按需选择启用                                                    |

**能力是按需启用的。** `template.android.library` 保持最小化；模块只在需要时添加
`template.hilt`、`template.viewbinding` 或 `template.room`。只有 `template.android.feature`
捆绑了 Hilt 和 ViewBinding，因为 feature 模块数量多且形态统一。

**`AppConfig` 是唯一事实来源**，负责应用 ID、版本、SDK 版本和 Java 版本：

```kotlin
object AppConfig {

    // --- 应用标识与版本 ---

    const val APPLICATION_ID = "com.example.template"

    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0"

    // --- SDK 版本 ---

    const val COMPILE_SDK = 37
    const val MIN_SDK = 28
    const val TARGET_SDK = 37

    // --- 编译选项 ---

    /** Java / Kotlin 目标版本。AGP 9 内置 Kotlin，因此 jvmTarget 跟随 compileOptions。 */
    val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_21
}
```

模块不再各自声明 `compileSdk`、`minSdk` 或 `versionCode` —— 改这里的值，所有模块随之生效。

另外两点 AGP 9 的说明：

* Release 构建目前设置 `optimization { enable = false }`，因此在你主动开启之前 R8 优化是
  关闭的。
* Keep 规则放在 `src/main/keepRules/*.keep`（参见 `app/src/main/keepRules/rules.keep`）；
  AGP 会把该目录下所有文件合并成一份规则集交给 R8。

## 3. 开发指南

新增一个业务能力意味着创建三个模块。下面的例子新增一个 **"用户设置"** 能力。

### 第 1 步：创建 Domain 模块

创建 `domain/settings/` 目录及其 `build.gradle.kts`：

```kotlin
plugins {
    id("template.kotlin.library")
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
```

在 `settings.gradle.kts` 中注册：

```kotlin
include(":domain:settings")
```

声明契约 —— 一个模型、一个 Repository 接口和若干 UseCase：

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

UseCase 保持原子性：一次业务操作，不了解其他能力。两个 UseCase 的组合属于 ViewModel。

### 第 2 步：在 `:core:database` 中注册表结构

Room 要求 `@Database` 类能看到所有实体和 DAO，所以表结构放在 `:core:database`，绝不能放在
data 模块（见 2.4）。在那里添加这张表：

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

再通过该模块的 Hilt 模块把 DAO 暴露出去：

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

每个新业务能力都要做三件事：把实体加进 `entities`，在 `AppDatabase` 里加一个 DAO 访问器，
再补一个 `@Provides`。表结构变化时提升 `version` 并编写 Migration。

### 第 3 步：创建 Data 模块

创建 `data/settings/` 目录及其 `build.gradle.kts`：

```kotlin
plugins {
    id("template.android.library")
    id("template.hilt")
}

android {
    namespace = "com.example.template.data.settings"
}

dependencies {
    // 用 implementation 而不是 api：:app 直接依赖每个模块，无需再导出任何东西。
    implementation(project(":domain:settings"))
    implementation(project(":core:database"))
}
```

注意这里没有 `template.room`：表结构在 `:core:database`，这个模块没有需要 KSP 生成的东西。
实现 Repository，把实体映射成 domain 模型：

```kotlin
// :data:settings
// 必须是 public：Hilt 在 :app 中生成组件，需要引用这个类型。
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

### 第 4 步：创建 Feature 模块

创建 `feature/settings/` 目录及其 `build.gradle.kts`：

```kotlin
plugins {
    id("template.android.feature")
}

android {
    namespace = "com.example.template.feature.settings"
}

dependencies {
    // 只依赖契约 —— 绝不依赖 :data:settings。
    implementation(project(":domain:settings"))
}
```

Hilt、ViewBinding、`:core:common` 和 `:shared:ui` 已经由约定插件提供 —— 不要重复声明。

### 第 5 步：定义状态与副作用

在 feature 模块内，把界面建模为不可变状态加一次性副作用类型：

```kotlin
data class SettingsState(
    val enabled: Boolean = false,
)

sealed interface SettingsSideEffect {
    data class ShowMessage(val text: String) : SettingsSideEffect
}
```

### 第 6 步：实现 ViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val setEnabled: SetEnabledUseCase,
) : BaseMVIViewModel<SettingsState, SettingsSideEffect>() {

    // getter 形式是必须的，见 2.4 隐式契约
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

`intent { ... }` 运行在 `viewModelScope` 中；在其内部 `state` 读取当前状态，`reduce` 产出
下一个状态，`postSideEffect` 发出一次性事件。

ViewModel 需要 `Context` 时直接注入，不要改用 `BaseAndroidMVIViewModel`：

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    observeSettings: ObserveSettingsUseCase,
) : BaseMVIViewModel<SettingsState, SettingsSideEffect>() { /* ... */ }
```

`BaseAndroidMVIViewModel` 是一个 `AndroidViewModel` 变体，它把 `application` 作为 public 属性
暴露给所有调用方。限定符能提供同一个实例，又不会扩大 ViewModel 的可见接口，所以那个基类没有
任何优势 —— 用注入就好。

跨业务编排放在这里，而不是放在 domain 模块：

```kotlin
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val getUser: GetUserUseCase,          // :domain:user
    private val createOrder: CreateOrderUseCase,  // :domain:order
) : BaseMVIViewModel<CheckoutState, CheckoutSideEffect>() { /* ... */ }
```

### 第 7 步：在导航图中注册目的地

应用级导航图由 `:app` 持有，所有目的地都加在这里：

```xml
<!-- app/src/main/res/navigation/nav_graph.xml -->
<fragment
    android:id="@+id/fragment_settings"
    android:name="com.example.template.feature.settings.SettingsFragment"
    android:label="@string/app_name"
    tools:layout="@layout/fragment_settings" />
```

代码里用 `navController.navigate(R.id.fragment_settings)` 跳转。跨 feature 导航同样在
`:app` 接线 —— 把回调传给需要它的界面，而不是让一个 feature 引用另一个。

### 第 8 步：实现 Fragment 与布局

遵循 `:feature:main` 中的 `MainFragment` / `fragment_main.xml` 模式：一个有状态的 Fragment
持有 ViewModel，通过 ViewBinding 渲染。

```kotlin
@AndroidEntryPoint
class SettingsFragment :
    BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 只有处于 STARTED 及以上时才收集状态与一次性副作用。
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

在 Fragment 中观察时务必用 `viewLifecycleOwner`（而不是 `this`），这样视图销毁时收集随之
停止。

### 第 9 步：接入 App

把新增模块加到 `app/build.gradle.kts`。`:app` 依赖整张图（见 2.4），因此清单与所有模块
一一对应：

```kotlin
dependencies {
    // Features
    implementation(project(":feature:main"))
    implementation(project(":feature:settings"))

    // Data 实现
    implementation(project(":data:main"))
    implementation(project(":data:settings"))

    // Domain 契约 —— Hilt 生成的组件会引用这些接口
    implementation(project(":domain:main"))
    implementation(project(":domain:settings"))

    // 基础设施
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    // 共享 UI
    implementation(project(":shared:designsystem"))
    implementation(project(":shared:ui"))
}
```

### 第 10 步：测试

约定插件已经把 JUnit 4 和 `kotlinx-coroutines-test` 放进每个模块的测试类路径，无需额外
配置。由于 `:domain:*` 是纯 Kotlin，UseCase 可以在 JVM 上用假 Repository 测试，不需要
Robolectric：

```kotlin
class ObserveSettingsUseCaseTest {

    @Test
    fun `emits the repository value`() = runTest {
        val useCase = ObserveSettingsUseCase(FakeSettingsRepository(enabled = true))

        assertEquals(Settings(enabled = true), useCase().first())
    }
}
```

由于 intent 运行在 `viewModelScope` 中，ViewModel 测试需要替换 Main 调度器：

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
        // UseCase 是普通类：基于假 Repository 构造它们，而不是 mock 掉。
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

### 第 11 步：评审清单

目前尚未接入自动化的依赖检查，因此以下项目需要在每次 Pull Request 中人工核对。请对照
`build.gradle.kts` 文件检查，而不是对照源码的 import：

- [ ] 没有任何 `:feature:*` 模块声明 `:data:*` 依赖。
- [ ] 没有任何 `:domain:*` 模块声明 `:core:*`、`:data:*`、`:feature:*`、`:shared:*` 或另一个
      `:domain:*`。
- [ ] 没有任何 `:domain:*` 模块应用 Android、Hilt、Room 或 ViewBinding 插件。
- [ ] 没有任何 `:data:*` 模块应用 `template.room` 或声明 `@Entity` / `@Dao`。
- [ ] 每个新增的实体和 DAO 都在 `:core:database` 中注册：加入 `entities`、在 `AppDatabase` 中
      提供访问器、并在 Hilt 模块中提供 `@Provides`。
- [ ] 每个新增模块（`:feature:*`、`:data:*`、`:domain:*`）都以 `implementation` 加入了
      `app/build.gradle.kts`。
- [ ] 没有任何库模块使用 `api(project(...))` —— 完整依赖图由 `:app` 持有。
- [ ] `:domain:*` 导入的是 `javax.inject.Inject`，绝不是 `jakarta.inject.Inject`。
- [ ] Domain 模型与 Room 实体是两套类型，并且有显式的映射器。
- [ ] 每个 `initialState` 覆盖都用 getter 形式（见 2.4）。

## 4. 构建与测试

```bash
./gradlew assembleDebug          # 构建 debug APK
./gradlew test                   # 运行所有模块的单元测试
./gradlew connectedAndroidTest   # 在真机 / 模拟器上运行仪器化测试
./gradlew :feature:main:test     # 只跑单个模块的任务
./gradlew :domain:main:test      # domain 测试直接跑在 JVM 上
```

构建需要 **JDK 21**。配置缓存已开启并设置 `problems=fail`，因此任何不兼容配置缓存的任务都会
让构建失败，而不是悄悄降级。

## 5. 许可证

本项目基于 Apache License 2.0 授权。详见 `LICENSE` 文件。

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
