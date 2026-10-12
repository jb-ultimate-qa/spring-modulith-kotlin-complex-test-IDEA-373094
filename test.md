# IDEA-373094 — Spring Modulith: support Kotlin — QA plan

**Issue:** [IDEA-373094 — Spring Modulith: support Kotlin](https://youtrack.jetbrains.com/issue/IDEA-373094)
**Parent:** [IDEA-332133 — Spring Modulith: framework support in IntelliJ IDEA (phase 1)](https://youtrack.jetbrains.com/issue/IDEA-332133)
**Subsystem:** Spring. Modulith · **Plugin:** `plugins/spring/spring-modulith` (`com.intellij.spring.modulith`)
**Reference project (Java):** https://github.com/jb-ultimate-qa/spring-modulith-complex-scenario-01
**Plan written:** 2026-10-09 against `master` at `7d66d4bc9255e`.

This file is a plan for an agent. Part A records the analysis. Part B specifies the Kotlin project to build.
Part C lists the test cases. Part D tells the agent how to execute and report.

> What this plan does not contain, and why:
> - The YouTrack issue body, state, and comments. The `youtrack` CLI found no token in the macOS Keychain
>   (`security add-generic-password -a "$USER" -s youtrack-token -w '<token>'` fixes it). The public page is a
>   SPA and returns no text to a fetcher. Only the title and the parent link were confirmed through a web search.
> - A fix commit. `git log --all --grep=IDEA-373094` returns nothing on 2026-10-09. Section A.1 tells the agent
>   how to analyze the commit once it exists. Section A.3 is the baseline the diff must be compared with.

---

## Part A — Analysis

### A.1 The commit to analyze

No commit mentions IDEA-373094 yet. The feature is therefore tested against the current plugin, which is the
pre-fix baseline. When the fix lands, the agent runs this procedure first:

```bash
git fetch origin
git log --all --oneline --grep='IDEA-373094'
git log --all --oneline -i --grep='kotlin' -- plugins/spring/spring-modulith
git show --stat <sha>
git show <sha> -- plugins/spring/spring-modulith/resources/META-INF/plugin.xml
```

Then the agent maps every changed file to the baseline table in A.3 and marks each row `fixed`, `partly`, or
`untouched`. Each `untouched` row stays a known gap and its test cases stay expected-to-fail.

Modulith commits that shape the baseline behavior (newest first):

| Commit | Subject | Why it matters for Kotlin |
|---|---|---|
| `693d3abefa8cc` | IDEA-378404 `@Modulithic.additionalPackages` | Reads the attribute through `MetaAnnotationUtil` and `@AliasFor`, so a Kotlin app class is a candidate. |
| `9941d06be4033` | IDEA-379856 String constants in `allowedDependencies` | Uses UAST `evaluateString()`, so Kotlin `const val` can work once the literal is reached. |
| `24fa4b4266fdf` | IDEA-376940 parent accesses exposed nested types | Nested-module rules in `springModulithVerification.kt`. The Kotlin project mirrors them. |
| `98bc435ceacd4` | IDEA-383778 nested accesses parent internals | Same. |
| `4c7ac27d8b697` | IDEA-386643 make-open fix with existing `package-info.java` | The fix still creates a `.java` file. See TC-QF-03. |
| `6acedadd6ed89` | IDEA-381250 named-interface icons for packages only | `NamedInterface.Kind` decides project-view icons. |
| `c1f32631df5e0` | IDEA-374869 move class to base package fix | Uses the Java move processor. See TC-QF-01. |
| `b7dc1d3922be4` | IDEA-373092 complete and validate allowed dependencies | Registered for `language="JAVA"` only. |

### A.2 How the plugin builds its model today

Source: `plugins/spring/spring-modulith/src/com/intellij/spring/modulith/`.

1. `SpringModulithModelCollector.getRootPackages()` takes the single `@SpringBootApplication` class of the IDE
   module and uses **its directory** as the root (`SpringModulithModelCollector.kt:90`). Two application classes
   give an empty model.
2. `SpringModulithDetectionStrategy` lists direct sub-directories of that one directory as modules. With
   `spring.modulith.detection-strategy=explicitly-annotated` it keeps only directories whose
   `package-info.java` carries `@ApplicationModule`. Nested modules are found the same way.
3. `getApplicationModule()` reads `id`, `displayName`, `allowedDependencies`, `type` from
   `package-info.java` only (`SpringModulithModelCollector.kt:301`, `:526-547`). Without the file the module gets
   the directory name, no allowed dependencies, and type `CLOSED`.
4. Classes come from `PsiClassOwner.classes`, so Kotlin light classes are included. `isPublicClass` checks
   `JvmModifier.PUBLIC` (`:610`).
5. Named interfaces come from `package-info.java` **or** from a type annotated with `@PackageInfo` and
   `@NamedInterface` (`:380-424`). Type-level `@NamedInterface("name")` exposes only that type.
6. `SpringModulithApiUsageInspection` is UAST and checks every `UReferenceExpression` that resolves to a
   `PsiClass`. Both files must sit under a `JavaSourceRootType.SOURCE` root (`:124`).
7. All other code insight is Java PSI: completion, reference, allowed-dependency inspection, event-listener
   inspection, and all quick fixes.

### A.3 Baseline: Java-only code paths a Kotlin fix must touch

| # | Area | Anchor | Java-only reason | Kotlin effect today |
|---|---|---|---|---|
| B1 | Module declaration | `SpringModulithModelCollector.kt:301`, `springModulithModelUtils.kt:17` | Reads only `package-info.java` | `@ApplicationModule` on a Kotlin type is ignored: no `id`, `displayName`, `allowedDependencies`, `OPEN` |
| B2 | Nested module detection | `SpringModulithDetectionStrategy.kt` → `getApplicationModuleAnnotation` | Same file lookup | A Kotlin-declared nested module is shown as an internal package |
| B3 | Explicit strategy | `SpringModulithDetectionStrategy.kt` `isDeclaredAsModule` | Same | `explicitly-annotated` + Kotlin = zero modules, plugin silent |
| B4 | Root directory | `SpringModulithModelCollector.kt:90` | One `PsiDirectory`, not the `PsiPackage` | Module packages that exist only in another source root (`src/main/java` vs `src/main/kotlin`) are missed; `package-info.java` in the other root is missed |
| B5 | `type = OPEN` | `SpringModulithModelCollector.kt:538` | `PsiReferenceExpression.resolve()` | A Kotlin light annotation value is not a `PsiReferenceExpression`; falls back to `CLOSED` |
| B6 | Allowed-dependency completion | `plugin.xml:58`, `SpringModulithDependenciesCompletionContributor.kt:40-42` | `language="JAVA"`, `PsiLiteralExpression`, `PsiNameValuePair` | No completion in `allowedDependencies = ["<caret>"]` |
| B7 | Allowed-dependency validation | `plugin.xml:74`, `SpringModulithAllowedDependencyInspection.kt:33` | Same pattern | No "Module 'x' does not exist" in Kotlin |
| B8 | Dependency reference | `plugin.xml:126` | `language="JAVA"` | No Ctrl+B from `"inventory"` to the package |
| B9 | Event-listener inspection | `plugin.xml:83`, `SpringModulithEventListenerInspection.kt:33` | `language="JAVA"`, `PsiMethod` | `@Async @Transactional @TransactionalEventListener fun` is not reported |
| B10 | Add allowed dependency fix | `SpringModulithHighlightingFixService.kt:58` | Edits `package-info.java` | Fix is offered only when the file exists; silent no-op otherwise |
| B11 | Mark class `@NamedInterface` fix | `SpringModulithHighlightingFixService.kt:87` | `addPhysicalAnnotationIfAbsent` on a light modifier list | Expect failure or no-op on a Kotlin class |
| B12 | Make module open fix | `SpringModulithHighlightingFixService.kt:228-231` | Creates `package-info.java` in the module directory | In `src/main/kotlin` the file is never compiled by Gradle (A.4 item 6) |
| B13 | Move class fix | `SpringModulithHighlightingFixService.kt:156` | `MoveClassesOrPackagesProcessor` | Expect failure or no-op on a Kotlin class |
| B14 | Implicit usage | `SpringModulithImplicitUsageProvider.kt:17` | `element is PsiClass` | Depends on the Kotlin unused-symbol inspection passing light classes |
| B15 | Model reset on VFS | `SpringModulithModelService.kt:246` | Special-cases `package-info.java` | Creating or deleting `ModuleMetadata.kt` relies on generic trackers |
| B16 | Public check | `SpringModulithModelCollector.kt:610` | `JvmModifier.PUBLIC` on light classes | Kotlin `internal` is public in bytecode and in light classes; a `private` top-level class is package-private. Both match the runtime |

### A.4 Java Modulith vs Kotlin Modulith: the differences that matter

Sources: Spring Modulith reference *Fundamentals* (2.1.1), `JavaPackage.java` and `ApplicationModuleInformationFactory.java`
on `1.4.x`, `PackageInfo.java`, the JetBrains Kotlin blog sample `maskaravivek/springmonolith`, Kotlin Gradle docs.

1. **No package annotations in Kotlin.** `package-info.java` cannot be written in Kotlin. The reference says:
   "As, for example, Kotlin lacks support for that file, you can also use the annotation on a single type located in
   the application module's root package." The documented shape is `class ModuleMetadata {}`. The name is free.
2. **`@PackageInfo` is the real switch.** `JavaPackage.findAnnotation` on the `1.4.x` branch head accepts a
   carrying type only when its simple name is `package-info` or it is meta-annotated with
   `org.springframework.modulith.PackageInfo` (since 1.2). The doc sample with a bare
   `@ApplicationModule(allowedDependencies = "order") class ModuleMetadata` is therefore **ignored**: the module
   still exists under the default strategy, but without `displayName`, `allowedDependencies`, or `OPEN`. Older
   1.4 patches may differ, so TC-DECL-07 records the runtime result first. Every other Kotlin declaration in this
   plan carries `@PackageInfo`.
3. **Two carriers in one package throw.** `findAnnotation` throws `IllegalStateException` ("Expected maximum of one
   type in package ... to be annotated with ...") when two `@PackageInfo` types carry the same annotation.
4. **The carrier type is itself API.** `getExposedClasses()` filters only the simple name `package-info`. A public
   Kotlin `ModuleMetadata` is an exposed type of the module. The IDE collector includes it too.
5. **Kotlin array syntax.** `allowedDependencies` and `name` are `String[]`. Kotlin requires `["a", "b"]` or
   `arrayOf(...)` for a named array attribute. `allowedDependencies = "order"` as written in the reference does
   not compile in Kotlin. `@NamedInterface("spi")` compiles because `value` is a vararg.
6. **Gradle ignores `.java` under `src/main/kotlin`.** Kotlin Gradle docs: "Do not store Java .java files in the
   src/*/kotlin directory, as the .java files will not be compiled." A `package-info.java` generated there is seen
   by the IDE and ignored by the build. The JetBrains Kotlin sample keeps its two `package-info.java` files under
   `src/main/java` next to Kotlin code under `src/main/kotlin`. That layout hits baseline gap B4.
7. **Visibility.** Kotlin `internal` compiles to `public`; the runtime exposes such types. A top-level `private`
   class compiles to package-private and is hidden. The community workaround is a sub-package named `internal`.
8. **File facades.** Top-level functions and properties in `Foo.kt` compile to class `FooKt` (or the `@file:JvmName`
   name). A call to a top-level function in another module's internal package is a dependency on that class at
   runtime. The IDE inspection only reports references that resolve to a class, so a bare function call is a
   candidate gap (TC-API-12).
9. **Entry point.** `fun main(args: Array<String>) = runApplication<Application>(*args)` lives in the file facade
   `ApplicationKt`, outside any module. The root package is the package of the `@SpringBootApplication` class.
10. **Events.** The preferred `@ApplicationModuleListener` is `org.springframework.modulith.events.ApplicationModuleListener`
    in `spring-modulith-events-api` (attributes `id`, `condition`, `propagation`, `readOnlyTransaction`). A twin
    `org.springframework.modulith.ApplicationModuleListener` in `spring-modulith-api` is deprecated since 1.1
    (`forRemoval = true`) and has only `readOnlyTransaction`. The plugin constant `APPLICATION_MODULE_LISTENER`
    (`springModulithConstants.kt:10`) names the preferred one. `spring-modulith-starter-core` (1.4.x pom) brings
    `spring-modulith-api`, `spring-modulith-apt`, `spring-modulith-core` (runtime), `spring-modulith-moments`,
    `spring-boot-starter`, and **not** `events-api`. So a Kotlin starter project can import only the deprecated
    twin unless it declares `events-api`. The IDE event-listener inspection needs `events-api` on the classpath
    (`hasModulithEventsApi`). Declare it explicitly, and test the deprecated twin too (TC-EVT-07).
11. **jMolecules `@Module`** is an alternative carrier the runtime accepts and the IDE does not read. Out of scope
    for this issue; one exploratory case (TC-X-06).

---

## Part B — Kotlin test project

Name: `spring-modulith-kotlin-scenario-01`. Keep it in the `jb-ultimate-qa` GitHub organization next to the Java
scenario. Mirror the Java project: the same `com.example` root, the same nested-module story, plus the
Kotlin-only shapes from A.4.

### B.1 Build

- Gradle Kotlin DSL, wrapper 8.14 or newer, JDK 21 toolchain.
- Kotlin 2.2.x with `kotlin("jvm")` and `kotlin("plugin.spring")`.
- Spring Boot `3.5.x` (the plugin fixture pins `3.5.3`), `spring-modulith-bom:1.4.x` (fixture pins `1.4.0`,
  the Java scenario pins `1.4.1`). Use the latest `1.4.x` patch.
- Second profile, optional: Boot `4.0.x` + Modulith `2.0.x` with
  `spring.modulith.runtime.verification-enabled=true` to fail at startup on violations.

`build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

repositories { mavenCentral() }

dependencyManagement {
    imports { mavenBom("org.springframework.modulith:spring-modulith-bom:1.4.6") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-events-api") // A.4 item 10

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<Test> { useJUnitPlatform() }
```

`settings.gradle.kts`: `rootProject.name = "spring-modulith-kotlin-scenario-01"`.

The agent verifies the versions against Maven Central before the first build and records the final ones in
Part D. `./gradlew dependencies --configuration compileClasspath` must list `spring-modulith-events-api`.

### B.2 Source layout

Legend: `[K]` Kotlin declaration carrier, `[J]` Java `package-info.java`, `(ni)` named interface.

```
src/main/kotlin/com/example
├── Application.kt                 @Modulithic(systemName = "Kotlin Modulith", sharedModules = ["shared"],
│                                             additionalPackages = ["com.other"])  @SpringBootApplication class Application
│                                  fun main(args: Array<String>) { runApplication<Application>(*args) }
├── ModuleIds.kt                   object ModuleIds { const val INVENTORY = "inventory"; const val CATALOG = "catalog" }
│
├── order/                         [K] module "order", CLOSED, displayName "Order Module",
│   │                                  allowedDependencies = [ModuleIds.INVENTORY, "catalog :: api", "shared :: *"]
│   ├── ModuleMetadata.kt
│   ├── OrderService.kt            @Service, injects InventoryApi, CatalogApi, SharedService; publishes OrderCompleted
│   ├── OrderCompleted.kt          data class OrderCompleted(val orderId: String)   (event, exposed)
│   ├── OrderFunctions.kt          top-level fun newOrderId(): String  → facade OrderFunctionsKt (exposed)
│   ├── internal/
│   │   ├── OrderRepository.kt     @Repository class OrderRepository
│   │   ├── OrderInternals.kt      top-level fun internalHelper(): String, object OrderInternalObject, typealias
│   │   └── OrderTag.kt            annotation class OrderTag, sealed interface OrderState, enum class OrderKind
│   ├── spi/                       [K] (ni "spi"):  @PackageInfo @NamedInterface("spi") class ModuleMetadata
│   │   └── OrderSpi.kt            interface OrderSpi
│   ├── web/                       [K] nested module "order.web", displayName "Order Web"
│   │   ├── ModuleMetadata.kt
│   │   ├── OrderController.kt     @RestController, injects order.internal.OrderRepository      (legal, nested → parent internal)
│   │   ├── internal/OrderWebInternal.kt
│   │   └── details/               [K] nested module "order.web.details" (grandchild)
│   │       ├── ModuleMetadata.kt
│   │       └── OrderWebDetail.kt  uses order.internal.OrderRepository                           (legal, grandchild → grandparent internal)
│   └── api/                       [K] nested module "order.api" (sibling of web)
│       ├── ModuleMetadata.kt      @ApplicationModule(allowedDependencies = [ModuleIds.INVENTORY]) @PackageInfo  (nested with own restriction)
│       ├── OrderApiFacade.kt      uses order.internal.OrderRepository and order.web.OrderController (legal)
│       └── internal/ApiSecret.kt
│
├── inventory/                     no carrier: module "inventory", unrestricted, default displayName "Inventory"
│   ├── InventoryApi.kt            interface InventoryApi
│   ├── InventoryEvents.kt         @Component, @ApplicationModuleListener fun on(event: OrderCompleted)
│   ├── LegacyListener.kt          @Component, @Async @Transactional(readOnly = true) @TransactionalEventListener(id = "legacy") fun on(event: OrderCompleted)
│   ├── InventoryInternalVisible.kt   internal class InventoryInternalVisible            (A.4 item 7: public in bytecode)
│   ├── InventoryPrivate.kt        private class InventoryPrivate                          (package-private in bytecode)
│   └── internal/
│       ├── InventoryRepository.kt
│       └── StockLevel.kt          data class StockLevel(val sku: String, val qty: Int)
│
├── catalog/                       [K] module id "catalog", displayName "Catalog"
│   ├── ModuleMetadata.kt          @ApplicationModule(id = ModuleIds.CATALOG, displayName = "Catalog") @PackageInfo object ModuleMetadata   (object, not class)
│   ├── CatalogService.kt
│   ├── api/                       [K] (ni "api"):  @PackageInfo @NamedInterface(name = ["api"]) class ApiMarker   (array syntax, free name)
│   │   └── CatalogApi.kt
│   ├── events/
│   │   ├── CatalogChanged.kt      @NamedInterface("events") data class CatalogChanged(val id: String)   (type-level ni, only this type)
│   │   └── CatalogEventsInternal.kt   class CatalogEventsInternal                       (same package, not exposed)
│   ├── legacy/                    [K] (ni "legacy", "compat"): @PackageInfo @NamedInterface("legacy", "compat") class ModuleMetadata
│   │   └── LegacyCatalog.kt
│   └── internal/CatalogRepository.kt
│
├── shared/                        [K] module "shared", OPEN:
│   ├── ModuleMetadata.kt          @ApplicationModule(type = ApplicationModule.Type.OPEN) @PackageInfo class ModuleMetadata
│   ├── SharedService.kt
│   └── internal/SharedInternal.kt  reachable from everywhere because the module is OPEN
│
├── billing/                       [K] module "billing", allowedDependencies = []  (nothing allowed)
│   ├── ModuleMetadata.kt
│   └── BillingService.kt          no cross-module reference in the committed state
│
├── pricing/                       [K] module "pricing", OPEN written with an import:  import org.springframework.modulith.ApplicationModule.Type
│   ├── ModuleMetadata.kt          @ApplicationModule(type = Type.OPEN) @PackageInfo class ModuleMetadata
│   └── internal/PriceTable.kt
│
├── notes/                         bare carrier (no @PackageInfo): @ApplicationModule(allowedDependencies = []) class ModuleMetadata
│   ├── ModuleMetadata.kt          runtime ignores it (A.4 item 2); module "notes" is unrestricted at runtime
│   └── NotesService.kt            references inventory.InventoryApi  → legal at runtime
│
└── reporting/                     Kotlin code here, Java carrier in the other root (JetBrains blog layout, gap B4)
    └── ReportingService.kt        references order.internal.OrderRepository  → ILLEGAL at runtime (closed module, not allowed)

src/main/java/com/example
└── reporting/package-info.java    [J] @ApplicationModule(displayName = "Reporting", allowedDependencies = {"inventory"})

src/main/kotlin/com/other
└── billing/                       additional root (com.other) → second module id "billing" (duplicate id, A.2 note)
    ├── ModuleMetadata.kt          @ApplicationModule(displayName = "Other Billing") @PackageInfo class ModuleMetadata
    └── OtherBillingService.kt

src/main/resources/application.properties      spring.modulith.detection-strategy left unset (default strategy)
src/test/kotlin/com/example/ModulithTests.kt   ApplicationModules.of(Application::class.java).verify(), structure assertions
```

Rules for the committed state:

- `./gradlew test` is green except for the one intentional runtime failure in `reporting` (see TC-SRC-02). Keep
  `reporting` in the committed tree but exclude it from the `verify()` call with
  `ApplicationModules.of(Application::class.java, JavaClass.Predicates.resideInAPackage("com.example.reporting"))`.
  A second test without the exclusion documents that the violation is real.
- Every negative case (`NC-*`) lives in a snippet inside Part C. The agent applies it, observes, and reverts.
- Each `TC-*` line in the sources carries a `// TC-xxx` comment, the same convention as the Java scenario.
- Also keep the Modulith runtime `IllegalStateException` cases out of the committed state (TC-DECL-08).

### B.3 Declaration templates

```kotlin
// order/ModuleMetadata.kt
package com.example.order

import com.example.ModuleIds
import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    displayName = "Order Module",
    allowedDependencies = [ModuleIds.INVENTORY, "catalog :: api", "shared :: *"],   // TC-DEP-01, TC-DEP-05
)
@PackageInfo
class ModuleMetadata
```

```kotlin
// order/spi/ModuleMetadata.kt
package com.example.order.spi

import org.springframework.modulith.NamedInterface
import org.springframework.modulith.PackageInfo

@PackageInfo
@NamedInterface("spi")
class ModuleMetadata
```

```kotlin
// shared/ModuleMetadata.kt
package com.example.shared

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(type = ApplicationModule.Type.OPEN)
@PackageInfo
class ModuleMetadata
```

```kotlin
// src/test/kotlin/com/example/ModulithTests.kt
package com.example

import com.tngtech.archunit.core.domain.JavaClass
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import kotlin.test.assertTrue

class ModulithTests {

    private val modules = ApplicationModules.of(
        Application::class.java,
        JavaClass.Predicates.resideInAPackage("com.example.reporting"),
    )

    @Test
    fun `structure is detected`() {
        modules.forEach(::println)
        val ids = modules.map { it.identifier.toString() }
        listOf("order", "order.web", "order.web.details", "order.api", "inventory", "catalog", "shared",
               "billing", "pricing", "notes").forEach { assertTrue(it in ids, "missing module $it") }
    }

    @Test
    fun `verification passes without reporting`() {
        modules.verify()
    }

    @Test
    fun `reporting violates its allowed dependencies`() {
        val all = ApplicationModules.of(Application::class.java)
        val violations = runCatching { all.verify() }.exceptionOrNull()
        assertTrue(violations != null && "reporting" in violations.message.orEmpty())
    }
}
```

---

## Part C — Test cases

Conventions:

- **Runtime** = what `ApplicationModules.verify()` or the printed structure says. It is the ground truth.
- **IDE expected** = the behavior the fixed plugin must show. **Baseline** = the behavior predicted for the current
  plugin from A.3. A row where the two differ is a Kotlin-support test. A row where they agree is a regression guard.
- PASS for a positive case: no Spring Modulith error on the marked line, nothing in *Problems*. PASS for a negative
  case: the error is shown. Hover and `Alt+Enter` on the marked reference. Run *Code | Inspect Code* on the module
  once per build as a second observation.
- Wait for indexing and for the 500 ms model debounce after every edit.

### C.1 Environment

| Item | Value |
|---|---|
| IntelliJ IDEA | Ultimate, a build with the IDEA-373094 fix, and one pre-fix build for the baseline |
| Plugin | Spring Modulith enabled (bundled) |
| Inspections | *Settings → Editor → Inspections → Spring Modulith*: all three enabled, default severity |
| Import | Gradle, JDK 21, full indexing done |
| Project | `spring-modulith-kotlin-scenario-01` from B.2 |

### C.2 Group DECL — module declaration in Kotlin

| ID | Scenario | Where | Runtime | IDE expected | Baseline |
|---|---|---|---|---|---|
| TC-DECL-01 | `@ApplicationModule @PackageInfo class ModuleMetadata` declares a top-level module | `order/` | module `order`, display "Order Module" | Project view shows the `order` directory with the green module icon and location text `Order Module`; Structure view lists id `order` | Icon yes; no location text; no allowed dependencies in Structure |
| TC-DECL-02 | `displayName` read from Kotlin | `order/`, `catalog/` | display names shown by `modules.forEach(::println)` | Project view location text equals `displayName` | Capitalized directory name only |
| TC-DECL-03 | `id` attribute from a `const val` (`ModuleIds.CATALOG`) | `catalog/` | id `catalog` | `allowedDependencies = ["catalog :: api"]` in `order` resolves; Structure shows `Id: catalog` | Falls back to the directory name (same string here, so pass by accident; rename the directory to `catalogue` to expose it) |
| TC-DECL-04 | Carrier is an `object`, not a `class` | `catalog/ModuleMetadata.kt` | detected | Same as a class | Same as a class |
| TC-DECL-05 | Carrier has a free name (`ApiMarker`) | `catalog/api/` | ni `api` detected | Icon and Structure entry for `api` | Works (named interfaces already read `@PackageInfo` types) |
| TC-DECL-06 | `type = ApplicationModule.Type.OPEN` | `shared/` | OPEN | `shared/internal` directory gets the green icon; `order → shared.internal.SharedInternal` not flagged; Structure shows `OPEN` | Red icon; reference flagged (false positive) |
| TC-DECL-06b | `type = Type.OPEN` through an import of the nested enum | `pricing/` | OPEN | Same as TC-DECL-06 | Same false positive; after the fix B5 must handle both spellings |
| TC-DECL-07 | Bare carrier without `@PackageInfo` | `notes/` | predicted **ignored** (A.4 item 2); `notes` is unrestricted. Confirm with `modules.forEach(::println)` first | The IDE must agree with the runtime: `notes → InventoryApi` not flagged. Optional improvement: a warning on the bare `@ApplicationModule` type | Not flagged (declaration is ignored for a different reason) |
| TC-DECL-08 | Two `@PackageInfo` types in one package, both with `@ApplicationModule` | temp copy of `order/ModuleMetadata.kt` as `Second.kt` | `IllegalStateException` from `findAnnotation` | No exception in the IDE log; one declaration wins; ideally a warning on both types | Model ignores both |
| TC-DECL-09 | `private class ModuleMetadata` (package-private in bytecode) | `order/spi/` temp edit | runtime still detects it (`classes` is unfiltered) | ni `spi` still detected | `getAnnotatedPackageNamedInterface` filters by `isPublicClass` → **ni lost**, `order.spi.OrderSpi` becomes internal, false positives appear |
| TC-DECL-10 | `internal class ModuleMetadata` | `order/spi/` temp edit | detected | detected | detected (public in bytecode) |
| TC-DECL-11 | Carrier type is exposed API (A.4 item 4) | reference `com.example.order.ModuleMetadata` from `inventory` | legal | not flagged | not flagged |
| TC-DECL-12 | Kotlin array syntax `allowedDependencies = arrayOf("inventory")` | temp edit in `order/` | same as `[...]` | same as `[...]` | ignored |
| TC-DECL-13 | `allowedDependencies = "inventory"` without brackets | temp edit | Kotlin compile error | Kotlin error shown; no plugin exception; model unchanged | same |
| TC-DECL-14 | `@NamedInterface(name = "api")` without brackets | temp edit in `catalog/api/` | compile error | Kotlin error; no plugin exception | same |
| TC-DECL-15 | `@NamedInterface("legacy", "compat")`: two names for one package | `catalog/legacy/` | both names listed | Structure lists both; `"catalog :: compat"` completes and validates | Both detected; completion absent (B6) |
| TC-DECL-16 | Bare `@PackageInfo @NamedInterface class ModuleMetadata` | temp edit in `order/spi/` | ni named `spi` after the package | same | same |
| TC-DECL-17 | `@NamedInterface("events")` on a type exposes only that type | `catalog/events/` | `CatalogChanged` exposed, `CatalogEventsInternal` not | `order → CatalogChanged` legal only with `"catalog :: events"` in its list; `CatalogEventsInternal` flagged | Type-level ni works; allowed list ignored, so the first half cannot be observed |
| TC-DECL-18 | `@file:JvmName("OrderUtils")` facade in the module root | temp edit on `OrderFunctions.kt` | exposed type `OrderUtils` | Structure view lists `OrderUtils` under the unnamed interface | same |

### C.3 Group NEST — nested modules declared in Kotlin

The runtime rules are those of the Java scenario (IDEA-383778, IDEA-376940). Only the carrier changed.

| ID | Scenario | Where (`// TC-…` marker) | Runtime | IDE expected | Baseline |
|---|---|---|---|---|---|
| TC-NEST-01 | Nested module is detected from a Kotlin carrier | `order/web/` | `order.web` | Project view: nested icon; Structure: `order.web` | **Shown as internal package of `order`**; `order.web` absent |
| TC-NEST-02 | Nested → parent internal | `order/web/OrderController.kt` | legal | not flagged | not flagged (same module in the baseline model) |
| TC-NEST-03 | Grandchild → grandparent internal | `order/web/details/OrderWebDetail.kt` | legal | not flagged | not flagged |
| TC-NEST-04 | Sibling nested → parent internal | `order/api/OrderApiFacade.kt` | legal | not flagged | not flagged |
| TC-NEST-05 | Sibling nested → sibling root type | `order/api/OrderApiFacade.kt` uses `order.web.OrderController` | legal | not flagged | not flagged |
| TC-NEST-06 | Parent → nested root type | `order/OrderService.kt` uses `order.web.OrderController` | legal | not flagged | not flagged |
| TC-NEST-07 | Nested module with its own `allowedDependencies` | `order/api/` allows only `inventory`; add a reference to `catalog.api.CatalogApi` (temp) | violation | flagged "Module 'order.api' depends on: 'catalog :: api'. But allowed dependencies are: ['inventory']" | **not flagged** (B1, B2) |
| TC-NEST-08 | Parent restriction does not leak into the nested module | `order/web/OrderController.kt` references `catalog.CatalogService` (root type; `order` allows only `catalog :: api`) | legal (`order.web` is unrestricted) | not flagged | not flagged, because the list of `order` is unknown. The row matters after the fix: the parent list must not apply to `order.web` |
| NC-NEST-01 | Unrelated top-level → nested internal | temp in `billing/`: `order.web.internal.OrderWebInternal` | violation | flagged (`has no allowed dependencies`, because `billing` allows nothing) | flagged as non-exposed type of `order` (different message) |
| NC-NEST-02 | Unrelated top-level → nested root type | temp in `inventory/`: `order.web.OrderController` | violation (invalid sub-module reference) | flagged "depends on non-exposed type … from module 'order.web'" | **not flagged** (`order.web` is not a module) |
| NC-NEST-03 | Sibling nested → sibling internal | temp in `order/web/`: `order.api.internal.ApiSecret` | violation | flagged | **not flagged** |
| NC-NEST-04 | Parent → nested internal | temp in `order/`: `order.web.internal.OrderWebInternal` | violation | flagged | **not flagged** |

### C.4 Group DEP — `allowedDependencies` in Kotlin

| ID | Scenario | Where | Runtime | IDE expected | Baseline |
|---|---|---|---|---|---|
| TC-DEP-01 | Module id from `const val` in an `object` | `order/ModuleMetadata.kt` `ModuleIds.INVENTORY` | allowed | `order → InventoryApi` not flagged; Structure lists `inventory` | not flagged (list ignored) |
| TC-DEP-02 | Named-interface dependency `"catalog :: api"` | `order/OrderService.kt` uses `CatalogApi` | allowed | not flagged | not flagged |
| TC-DEP-03 | Same module, non-listed type | `order/OrderService.kt` add `catalog.CatalogService` (temp) | violation | flagged "depends on: 'catalog'. But allowed dependencies are: […]" with quick fix *Add 'catalog' as an allowed dependency of the 'order' module* | **not flagged** |
| TC-DEP-04 | Wildcard `"shared :: *"` | `order` uses `shared.SharedService` | allowed | not flagged | not flagged |
| TC-DEP-05 | Empty list `[]` blocks everything | `billing/` add `inventory.InventoryApi` (temp) | violation | flagged "Module 'billing' has no allowed dependencies" | **not flagged** |
| TC-DEP-06 | Unknown module `"warehouse"` | temp edit in `order/` | startup/verify failure | string highlighted "Module 'warehouse' does not exist" | **nothing** (B7) |
| TC-DEP-07 | Unknown named interface `"catalog :: nope"` | temp | failure | "Named interface 'nope' does not exist" | nothing |
| TC-DEP-08 | Malformed `"a :: b :: c"` and empty `""` | temp | failure | "Malformed dependency identifier", "Module dependency identifier can't be empty" | nothing |
| TC-DEP-09 | Whitespace variants `"catalog::api"`, `" catalog :: api "` | temp | accepted | accepted, no error | nothing |
| TC-DEP-10 | String template of constants `"${ModuleIds.CATALOG} :: api"` | temp | accepted (constant expression) | accepted; if the evaluator cannot fold it, report a bug | nothing |
| TC-DEP-11 | Duplicate entry `["inventory", "inventory"]` | temp | accepted | accepted; completion no longer offers `inventory` | nothing |
| TC-DEP-12 | Self reference `["order"]` in `order` | temp | runtime: record the result | no crash; record the result | nothing |
| TC-DEP-13 | Reference to a nested id `"order.web"` from `billing` | temp | record the result (expected: invalid sub-module reference) | id resolves (no "does not exist"); access still reported by the API inspection | nothing |
| TC-DEP-14 | Completion inside `allowedDependencies = ["<caret>"]` | `billing/ModuleMetadata.kt` | n/a | Items: `catalog`, `catalog :: api`, `catalog :: events`, `catalog :: legacy`, `catalog :: compat`, `catalog :: *`, `inventory`, `inventory :: *`, `order`, `order :: spi`, `order :: *`, `shared`, `pricing`, `notes`, `reporting`; **no** `billing`, no nested ids (`order.web`, …) | **no items** (B6) |
| TC-DEP-15 | Completion excludes declared entries | caret in `order/ModuleMetadata.kt` list | n/a | `inventory`, `catalog :: api`, `shared :: *` absent; `catalog :: *` and `shared` present | no items |
| TC-DEP-16 | Completion inside a nested carrier offers only siblings and top-level modules | `order/api/ModuleMetadata.kt` | n/a | `order.web` present (sibling), `order.web.details` absent | no items |
| TC-DEP-17 | Ctrl+B on `"inventory"` and on `"catalog :: api"` | `order/ModuleMetadata.kt` | n/a | selects `com.example.inventory`, respectively `com.example.catalog.api`, in the Project view | **no reference** (B8) |
| TC-DEP-18 | Rename the `inventory` directory to `stock` with Refactor → Rename package | | id changes | `"inventory"` entries turn red "does not exist"; completion offers `stock` | nothing |

### C.5 Group API — restricted module API usage, Kotlin reference kinds

Target for every row: `com.example.order.internal.OrderRepository` (closed module `order`, not exposed) referenced from
`com.example.inventory` (unrestricted). Runtime: violation for every row. IDE expected: flagged at the marked reference,
message `Module 'inventory' depends on non-exposed type 'com.example.order.internal.OrderRepository' from module 'order'`
with the three quick fixes (move, `@NamedInterface`, make open). Each row is a temporary snippet in
`inventory/ApiProbe.kt`.

| ID | Reference kind | Snippet | Baseline prediction |
|---|---|---|---|
| TC-API-01 | import directive | `import com.example.order.internal.OrderRepository` | flagged |
| TC-API-02 | constructor property type | `class ApiProbe(private val repo: OrderRepository)` | flagged |
| TC-API-03 | function parameter and return type | `fun f(r: OrderRepository): OrderRepository = r` | flagged |
| TC-API-04 | generic argument | `val list: List<OrderRepository> = emptyList()` | flagged |
| TC-API-05 | local variable with explicit type and `is`/`as` | `val r: OrderRepository = x as OrderRepository; if (x is OrderRepository) …` | flagged |
| TC-API-06 | class literal | `OrderRepository::class.java` | flagged |
| TC-API-07 | inheritance and interface implementation | `class P : OrderRepository()` (make the class `open`), `class Q : OrderSpi` | flagged |
| TC-API-08 | `object` member call | `OrderInternalObject.describe()` | flagged (qualifier resolves to the class) |
| TC-API-09 | companion object call | `OrderRepository.create()` | flagged |
| TC-API-10 | annotation usage | `@OrderTag class Tagged` | flagged |
| TC-API-11 | typealias to an internal type | `typealias Repo = OrderRepository` and `val r: Repo` | the alias declaration is flagged; the use of `Repo` is a question: record it |
| TC-API-12 | **top-level function call** | `import com.example.order.internal.internalHelper` then `internalHelper()` | **not flagged** (resolves to a method, A.4 item 8). Runtime flags `OrderInternalsKt`. Expected after the fix: flagged at the import or the call |
| TC-API-13 | top-level property and extension function | `import com.example.order.internal.someProperty`, `"x".internalExt()` | same as TC-API-12 |
| TC-API-14 | enum entry and sealed subtype | `OrderKind.BULK`, `when (s) { is OrderState.Paid -> … }` | flagged on the type qualifier |
| TC-API-15 | lambda parameter type and delegation | `val h: (OrderRepository) -> Unit`, `class D(r: OrderSpi) : OrderSpi by r` | flagged |
| TC-API-16 | type inference only, no explicit type | `val r = OrderRepository.create()` | flagged on the qualifier; the inferred type itself has no reference |
| TC-API-17 | reference inside a Kotlin test source root | same snippet in `src/test/kotlin` | **not flagged** (test roots are excluded by `JavaSourceRootType.SOURCE`); runtime `verify()` ignores test classes too |
| TC-API-18 | reference from a Kotlin `script.kts` or a non-source file | scratch file | not flagged |
| TC-API-19 | Kotlin `internal class` as target (A.4 item 7) | `inventory.InventoryInternalVisible` referenced from `order` root (temp) | runtime: **legal** (public type in the root package); IDE: not flagged. Confirms light-class visibility |
| TC-API-20 | top-level `private class` as target | `inventory.InventoryPrivate` from `order` | Kotlin compile error; plugin silent |
| TC-API-21 | Java target from Kotlin source | `reporting` Java type, if any; otherwise skip | consistent with Java rules |
| TC-API-22 | Kotlin target from a Java source | add `src/main/java/com/example/inventory/JavaProbe.java` referencing `OrderRepository` | flagged (regression guard) |

### C.6 Group QF — quick fixes on Kotlin code

Setup: TC-API-02 snippet in `inventory/ApiProbe.kt`. `Alt+Enter` on `OrderRepository`.

| ID | Fix | IDE expected | Baseline prediction | Also check |
|---|---|---|---|---|
| TC-QF-01 | *Move 'com.example.order.internal.OrderRepository' to the 'order' module base package* | `OrderRepository.kt` moves to `com/example/order/`, imports and references update, error disappears | B13: Java move processor on a light class; expect no-op or exception | IDE log for exceptions; undo works |
| TC-QF-02 | *Annotate 'OrderRepository' with '@NamedInterface'* | `@NamedInterface` added to the Kotlin class with an import; the type becomes exposed; error disappears | B11: expect no-op or exception | the fix is offered only when the type is not already in the unnamed interface |
| TC-QF-03 | *Make the 'order' module open* | Kotlin-aware result: `type = ApplicationModule.Type.OPEN` added to `order/ModuleMetadata.kt`. Accept also: a `package-info.java` created under `src/main/java`. Reject: a `package-info.java` under `src/main/kotlin`, because Gradle ignores it (A.4 item 6) and `verify()` still fails | B12: creates `src/main/kotlin/com/example/order/package-info.java`; IDE shows green, `./gradlew test` fails | if the file is created, run `./gradlew test` to prove the mismatch |
| TC-QF-03b | Same fix when the module has no carrier at all (`inventory`) | Kotlin-aware result: a new `ModuleMetadata.kt` with `type = OPEN`, or a `package-info.java` under `src/main/java` | same as above | |
| TC-QF-04 | *Add 'catalog' as an allowed dependency of the 'order' module* (TC-DEP-03) | `allowedDependencies = [ModuleIds.INVENTORY, "catalog :: api", "shared :: *", "catalog"]` written into the Kotlin annotation; constant kept as a constant | B10: fix absent (list unknown) or silent no-op | formatting of the array literal; no duplicate import |
| TC-QF-05 | *Simplify with '@ApplicationModuleListener'* on `LegacyListener.on` | Weak warning on `on`; fix produces `@ApplicationModuleListener(id = "legacy", readOnlyTransaction = true) fun on(...)`; `@Async`, `@Transactional`, `@TransactionalEventListener` removed; imports optimized | B9: no warning | class-level `@Transactional` variant; `propagation` and `condition` are kept; two of three annotations are not reported |
| TC-QF-06 | Intention preview for each fix | preview opens or shows "no preview" without exception | same | |
| TC-QF-07 | Undo after each fix | one undo step restores the file | same | |

### C.7 Group VIEW — Project view, Structure view, bean completion

| ID | Scenario | IDE expected | Baseline |
|---|---|---|---|
| TC-VIEW-01 | Project view icons for all packages of B.2 | `order`, `inventory`, `catalog`, `shared`, `billing`, `pricing`, `notes`, `reporting`: module icon. `order/web`, `order/web/details`, `order/api`: nested icon. `order/spi`, `catalog/api`, `catalog/legacy`: named-interface (green) icon. `catalog/events`: **red** (type-level ni does not color the package). `*/internal`: red. `shared/internal`, `pricing/internal`: green (OPEN). `com/example` root and `com/other`: default folder icon | nested → red; OPEN internals → red; no display names |
| TC-VIEW-02 | Display names as location text | `Order Module`, `Catalog`, `Order Web`, `Reporting`, `Other Billing` | `Reporting` only if gap B4 is closed; else none |
| TC-VIEW-03 | Flatten packages / compact middle packages toggles | icons stay correct in both modes | same |
| TC-VIEW-04 | Second source root of the same package (`src/main/java/com/example/reporting`) | both directory nodes carry the same icon and text | record |
| TC-VIEW-05 | Structure view on `Application.kt` (Alt+7, logical view) | node *Modules* → every module id; per module: `Id`, `Allowed Dependencies` (with children resolving to packages), `OPEN` for `shared`/`pricing`, `Named Interfaces` with their types; `ModuleMetadata` listed as an exposed type | *Modules* node present; no `Allowed Dependencies`, no `OPEN`, nested modules missing |
| TC-VIEW-06 | Structure view with a Kotlin `object` application or a class in a file with a different name (`Main.kt` containing `class Application`) | same as TC-VIEW-05 | same |
| TC-VIEW-07 | Bean completion in a Kotlin constructor parameter `class BillingService(private val <caret>)` and in `@Autowired lateinit var` | the completion popup shows beans from other modules with the restricted indicator when access is not allowed (`billing` allows nothing) | indicator depends on the access provider; the model has no allowed list, so no indicator |
| TC-VIEW-08 | Export-free check of the model: compare the Structure view with `modules.forEach(::println)` output | identical module ids, display names, named interfaces | mismatch on everything in A.3 |

### C.8 Group EVT — events and implicit usage

| ID | Scenario | IDE expected | Baseline |
|---|---|---|---|
| TC-EVT-01 | `@ApplicationModuleListener fun on(event: OrderCompleted)` in `inventory/InventoryEvents.kt` | the function is not reported as unused by the Kotlin unused-symbol inspection; the class is not reported as unused | record (B14) |
| TC-EVT-02 | Event type `OrderCompleted` is a Kotlin `data class` in the `order` root | `inventory → OrderCompleted` not flagged (root type) | same |
| TC-EVT-03 | Event type moved to `order/internal` (temp) | flagged in the listener parameter | same |
| TC-EVT-04 | `LegacyListener` with `@Async @Transactional @TransactionalEventListener` | weak warning (see TC-QF-05) | none |
| TC-EVT-05 | Remove `spring-modulith-events-api` from `build.gradle.kts`, re-import | TC-EVT-04 warning disappears; `@ApplicationModuleListener` import turns red (expected, not a plugin issue) | same |
| TC-EVT-06 | `suspend fun on(event: OrderCompleted)` with `@ApplicationModuleListener` | no plugin exception; record what the inspection does | same |
| TC-EVT-07 | Deprecated twin `import org.springframework.modulith.ApplicationModuleListener` on a second listener (temp) | Kotlin shows the deprecation warning; the plugin treats the method like any other method (no implicit usage, no simplify offer); the TC-QF-05 output imports the `events` variant. Record whether the fix adds the twin as an entry point | same |

### C.9 Group SRC — source roots, mixed languages, build layout

| ID | Scenario | Runtime | IDE expected | Baseline |
|---|---|---|---|---|
| TC-SRC-01 | JetBrains blog layout: Kotlin code in `src/main/kotlin`, carrier `package-info.java` in `src/main/java` (`reporting`) | module `reporting` restricted to `inventory`; `ReportingService → order.internal.OrderRepository` is a violation | flagged; display name `Reporting` | **not flagged** (B4: the collector looks for the file in the Kotlin directory) |
| TC-SRC-02 | Prove TC-SRC-01 at runtime | `./gradlew test`: the third test in B.3 passes, i.e. unrestricted `verify()` fails on `reporting` | n/a | n/a |
| TC-SRC-03 | Module package that exists only in `src/main/java` (add `src/main/java/com/example/audit/AuditService.java`) | module `audit` detected | `audit` appears in Structure and gets the module icon | **missing** (B4) |
| TC-SRC-04 | Application class in Java (`src/main/java/.../Application.java`), modules in Kotlin | all modules detected | all detected | **all Kotlin-only module directories missing** (B4 in the other direction) |
| TC-SRC-05 | `package-info.java` placed under `src/main/kotlin` (temp copy for `billing`) | ignored by Gradle; `verify()` ignores it | the IDE must either ignore it too or warn. Record. A silent acceptance is a mismatch to report | accepted silently |
| TC-SRC-06 | `kotlin { sourceSets.main { kotlin.srcDir("src/main/java") } }` variant and a single `src/main/java` root with mixed files | detected | detected | detected (one directory) |
| TC-SRC-07 | Maven variant (`kotlin-maven-plugin`, `src/main/kotlin`) | same as Gradle | same | same |
| TC-SRC-08 | Multi-project Gradle build: `app` (Application) and `order-module` subprojects sharing `com.example` | runtime scans the classpath, modules from both jars | record; the current model is per IDE module and reads one directory | modules in the other subproject missing |
| TC-SRC-09 | Two `@SpringBootApplication` classes in `main` (temp `Application2.kt`) | runtime: ambiguous, user error | empty model, no exception; icons revert to default | same |
| TC-SRC-10 | `spring.modulith.detection-strategy=explicitly-annotated` in `application.properties` | only `order`, `order.*`, `catalog`, `shared`, `billing`, `pricing`, `reporting`, `com.other.billing` are modules; `inventory` and `notes` are not | same set; `inventory` directory loses its icon | **zero modules**, every icon default (B3) |
| TC-SRC-11 | Same key in `application.yml` and in `application-dev.properties` (with the `dev` profile only) | yml: same as TC-SRC-10; profile file: default strategy at verify time | yml honored; profile-specific file not honored (plugin reads production defaults) | record |
| TC-SRC-12 | `@Modulithic(additionalPackages = ["com.other"])` from Kotlin | `com.other.billing` detected | module icon and display name `Other Billing` under `com/other/billing` | detected? The attribute is read through PSI annotations of the light class. Record |
| TC-SRC-13 | Duplicate module id `billing` across roots | runtime: record (`ApplicationModules` rejects duplicate identifiers) | no inspection exists; log at debug level; references to `billing` resolve to `com.example.billing` | same |
| TC-SRC-14 | New project wizard: Spring Boot, language Kotlin, Gradle Kotlin, dependency *Spring Modulith* (+ *Spring Data JPA*, + *Actuator*) | n/a | `build.gradle.kts` has `spring-modulith-starter-core`, `spring-modulith-starter-test`, `spring-modulith-starter-jpa`, `spring-modulith-actuator`, `spring-modulith-observability` | same |

### C.10 Group LIVE — model updates without restart

| ID | Edit | IDE expected (within ~1 s after typing stops) | Baseline |
|---|---|---|---|
| TC-LIVE-01 | Change `displayName` in `order/ModuleMetadata.kt` | Project view text updates | no text |
| TC-LIVE-02 | Add `"catalog"` to `order`'s list | TC-DEP-03 error disappears | nothing to change |
| TC-LIVE-03 | Create `order/pricing/ModuleMetadata.kt` with `@ApplicationModule @PackageInfo` | directory becomes a nested module; icons switch | stays red |
| TC-LIVE-04 | Delete that file | directory reverts to internal | stays red |
| TC-LIVE-05 | Toggle `type = OPEN` on `shared` back and forth | `shared/internal` icon toggles; API errors toggle | stays red |
| TC-LIVE-06 | Change `spring.modulith.detection-strategy` in `application.properties` | module set changes without restart | goes to zero modules, back to all |
| TC-LIVE-07 | Undo each of the above | model returns to the previous state | same |
| TC-LIVE-08 | Edit during indexing (Dumb mode) | no exception; model refreshes after indexing | same |

### C.11 Group X — exploratory

| ID | Scenario | What to record |
|---|---|---|
| TC-X-01 | Nested carrier declared as a nested class: `class Outer { @PackageInfo @NamedInterface("inner") class Meta }` | runtime and IDE both; the IDE reads only top-level classes |
| TC-X-02 | Carrier is an `annotation class` or an `interface` | both detect |
| TC-X-03 | `@PackageInfo` and `@ApplicationModule` on the application class itself | runtime: root package is not a module; IDE: no crash |
| TC-X-04 | Kotlin `@Modulithic` composed annotation with `@AliasFor(annotation = Modulithic::class, attribute = "additionalPackages")` | A.1 commit `693d3abefa8cc` handles Java; check Kotlin |
| TC-X-05 | Module id with Kotlin-illegal characters via `id = "order-core"` | completion inserts the id correctly in a Kotlin string |
| TC-X-06 | jMolecules `@Module` on a Kotlin carrier | runtime detects; the IDE does not; confirm no exception |
| TC-X-07 | KMP/Android-style source set names (`src/jvmMain/kotlin`) | detection of `JavaSourceRootType.SOURCE` |
| TC-X-08 | Very large module (500 generated Kotlin files) | model build time in `idea.log` at debug level for `#com.intellij.spring.modulith`; UI stays responsive |
| TC-X-09 | Remote development (split mode) with the Kotlin project | icons, inspections, completion on the frontend |

---

## Part D — Execution plan for the agent

1. **Read** this file, `plugins/spring/spring-modulith/spring-modulith-knowledge.md`, and the Java scenario's
   `IDEA-383778-TEST-CASES.md`, `IDEA-376940-TEST-CASES.md` for the house format.
2. **Locate the fix** with A.1. Update A.3 with the `fixed / partly / untouched` column. Do not edit A.4.
3. **Create the project** from Part B. Confirm versions on Maven Central. Run `./gradlew test`. The first two tests in
   B.3 pass, the third passes by catching the `reporting` violation. Commit. Record the structure printed by
   `modules.forEach(::println)` as `runtime-structure.txt` in the project root; it is the oracle for Part C.
4. **Baseline run** on a pre-fix IDE build. Execute every group, fill the matrix in D.1 with `Baseline observed`. A
   difference between `Baseline prediction` and `Baseline observed` is a finding about this plan, not about the fix;
   correct the plan.
5. **Fixed-build run.** Execute every group again. Use one project copy per build.
6. **Negative and temporary cases.** Apply the snippet, wait for analysis, observe, run `./gradlew test` when the row
   says so, then `git checkout -- .` and `git clean -fd`.
7. **Log check** after each group: *Help → Show Log*, filter `com.intellij.spring.modulith`. Any exception is a
   finding. Enable `#com.intellij.spring.modulith` in *Help → Diagnostic Tools → Debug Log Settings* before group LIVE.
8. **Report.** Fill D.1. For each failed row file one YouTrack issue per root cause with the `youtrack` skill
   (`write issue create --project IDEA --subsystem 'Spring. Modulith' …`), link it `relates to IDEA-373094`, and put
   the row ids in the description. Attach `runtime-structure.txt` content inline.
9. **Automation hooks**, only when asked: plugin unit tests live in `plugins/spring/spring-modulith/test`, test data in
   `testData/modulith-project`; run with
   `./tests.cmd --module intellij.spring.modulith.tests --test 'com.intellij.spring.modulith.*'`.
   A Kotlin light test needs the Kotlin plugin test dependencies in `intellij.spring.modulith.tests.iml`. UI tests live
   in `tests/remote-driver-tests/test/com/intellij/driver/tests/idea/ultimate/spring/`, and their projects are
   zip archives on `packages.jetbrains.team` (see `SpringCoreAsyncInspectionsKotlin01.kt` for the pattern).

### D.1 Results matrix

**IDE build:** `________` · **Plugin:** `________` · **Modulith:** `1.4.13` · **Kotlin:** `2.2.21` · **Date:** `________` · **Tester:** `________`

| ID | Runtime | IDE expected | Baseline prediction | Baseline observed | Fixed build observed | Status | Issue |
|---|---|---|---|---|---|---|---|
| TC-DECL-01 … TC-DECL-18 | | | | | | ☐ Pass ☐ Fail | |
| TC-NEST-01 … NC-NEST-04 | | | | | | ☐ Pass ☐ Fail | |
| TC-DEP-01 … TC-DEP-18 | | | | | | ☐ Pass ☐ Fail | |
| TC-API-01 … TC-API-22 | | | | | | ☐ Pass ☐ Fail | |
| TC-QF-01 … TC-QF-07 | | | | | | ☐ Pass ☐ Fail | |
| TC-VIEW-01 … TC-VIEW-08 | | | | | | ☐ Pass ☐ Fail | |
| TC-EVT-01 … TC-EVT-07 | | | | | | ☐ Pass ☐ Fail | |
| TC-SRC-01 … TC-SRC-14 | | | | | | ☐ Pass ☐ Fail | |
| TC-LIVE-01 … TC-LIVE-08 | | | | | | ☐ Pass ☐ Fail | |
| TC-X-01 … TC-X-09 | | | | | | ☐ Pass ☐ Fail | |
| `./gradlew test` | green | | | | | ☐ Pass ☐ Fail | |

Expand the matrix to one line per id before the first run.

### D.2 Exit criteria

- Every `TC-*` row whose *IDE expected* equals the runtime passes on the fixed build.
- Every `NC-*` row shows the error on the fixed build.
- No exception from `com.intellij.spring.modulith` in `idea.log` during the run.
- Every `record` row has an observation written down, and each mismatch between IDE and runtime has an issue.
- Baseline rows marked **bold** in Part C were observed failing on the pre-fix build. If one passes there, the
  prediction in A.3 was wrong; correct the table.

### D.3 Priorities when time is short

1. TC-DECL-01, 06, 09; TC-NEST-01, 07, NC-NEST-02; TC-DEP-03, 05, 06, 14, 17; TC-API-02, 12; TC-QF-03, 04, 05;
   TC-SRC-01, 04, 10; TC-LIVE-03.
2. The rest of DECL, NEST, DEP, API, VIEW.
3. EVT, SRC, LIVE, X.

---

## References

- Spring Modulith reference, Fundamentals: https://docs.spring.io/spring-modulith/reference/fundamentals.html
- `JavaPackage.java` (1.4.x): https://github.com/spring-projects/spring-modulith/blob/1.4.x/spring-modulith-core/src/main/java/org/springframework/modulith/core/JavaPackage.java
- `ApplicationModuleInformationFactory.java` (1.4.x): https://github.com/spring-projects/spring-modulith/blob/1.4.x/spring-modulith-core/src/main/java/org/springframework/modulith/core/ApplicationModuleInformationFactory.java
- `PackageInfo.java`: https://github.com/spring-projects/spring-modulith/blob/main/spring-modulith-api/src/main/java/org/springframework/modulith/PackageInfo.java
- IntelliJ IDEA help, Spring Modulith: https://www.jetbrains.com/help/idea/spring-modulith.html
- JetBrains blog, Building Modular Monoliths With Kotlin and Spring (sample `maskaravivek/springmonolith`): https://blog.jetbrains.com/kotlin/2026/02/building-modular-monoliths-with-kotlin-and-spring/
- JetBrains blog, Migrating to Modular Monolith using Spring Modulith and IntelliJ IDEA: https://blog.jetbrains.com/idea/2026/02/migrating-to-modular-monolith-using-spring-modulith-and-intellij-idea/
- codecentric, Spring Modulith with Kotlin and hexagonal architecture (`@PackageInfo` requirement): https://www.codecentric.de/en/knowledge-hub/blog/modularization-the-easy-way-spring-modulith-with-kotlin-and-hexagonal-architecture
- Spring Modulith discussion #238, Kotlin visibility: https://github.com/spring-projects/spring-modulith/discussions/238
- Kotlin Gradle docs, Java files under `src/*/kotlin` are not compiled: https://kotlinlang.org/docs/gradle-configure-project.html
- Kotlin Discussions, `package-info.java` not compiled for Kotlin packages: https://discuss.kotlinlang.org/t/solved-package-info-java-not-compiled-for-kotlin-packages/20335
- Java QA scenario: https://github.com/jb-ultimate-qa/spring-modulith-complex-scenario-01
