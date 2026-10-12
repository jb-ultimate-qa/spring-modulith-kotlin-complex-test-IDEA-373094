# IDEA-373094 — QA Test Cases (Spring Modulith: support Kotlin)

**Issue:** [IDEA-373094 — Spring Modulith: support Kotlin](https://youtrack.jetbrains.com/issue/IDEA-373094)
**Parent:** [IDEA-332133](https://youtrack.jetbrains.com/issue/IDEA-332133) · **Subsystem:** Spring. Modulith · **Plugin:** `plugins/spring/spring-modulith`
**Plan:** [`test.md`](test.md). This file implements Part B (the project) and Part C (the test cases). Parts A and D stay in `test.md`.
**Fix commit:** none on 2026-10-09 (`test.md` A.1), so every *Baseline* cell is a prediction for the current plugin.
**Java counterpart:** [jb-ultimate-qa/spring-modulith-complex-scenario-01](https://github.com/jb-ultimate-qa/spring-modulith-complex-scenario-01) (house format of this file)
**Runtime oracle:** [`runtime-structure.txt`](runtime-structure.txt) · **Prepared:** 2026-10-12

---

## 1. Summary

Spring Modulith reads module metadata from `package-info.java`. Kotlin has no package annotations, so the reference
documentation allows "the annotation on a single type located in the application module's root package". IDEA-373094
asks the IDE to support that and the rest of Kotlin. The cases check four things:

1. **Declarations.** `@ApplicationModule` and `@NamedInterface` on a Kotlin `class`, `object`, `interface`, or
   `annotation class`, with Kotlin argument syntax (`[...]`, `arrayOf`, `const val`, string templates).
2. **References.** Every Kotlin construct that depends on a non-exposed type: properties, generics, `is`, class literals,
   inheritance, delegation, `object` and companion calls, annotations, typealiases, top-level functions and properties.
3. **Code insight on Kotlin.** Completion, validation, and navigation in `allowedDependencies = [...]`, the
   event-listener inspection, and the four quick fixes.
4. **Layouts.** Kotlin and Java source roots, Java carriers next to Kotlin code, a Java application class, the detection
   strategy, and live updates of the model.

**Ground truth.** The result of `ApplicationModules.verify()` decides each case. A *Runtime* cell marked ✔ was observed
on 2026-10-12 with Spring Modulith 1.4.13. The snippet was applied to a copy of this project, and
`ApplicationModules.of(Application::class.java)` plus `detectViolations()` were printed. The same printout is available
here: run `RuntimeProbe` (§6). *n/a* marks IDE-only cells; *not probed* marks cells taken over from `test.md` unverified.

**Interpretation.** `verify()` green and an IDE error on the marked reference ⇒ IDE false positive. `verify()` red and no
IDE error ⇒ IDE false negative. The IDE may be stricter than the runtime only where R9 says so.

---

## 2. Runtime findings that correct `test.md`

`test.md` Part D step 2 says not to edit A.4, so the corrections are recorded here. Each row is backed by a probe run.

| # | `test.md` says | Spring Modulith 1.4.13 does | What changed here |
|---|---|---|---|
| R1 | A.4 item 2, TC-DECL-07: a bare `@ApplicationModule` type without `@PackageInfo` is ignored | `@ApplicationModule` is itself meta-annotated with `@PackageInfo` (checked in 1.2.0, 1.3.0, 1.4.0, 1.4.6, 1.4.13). The bare carrier **is read**. `@NamedInterface` is not meta-annotated, so a package-level named interface still needs `@PackageInfo` | `notes` keeps the bare carrier with a display name. `notes → InventoryApi` moved into a snippet, where it is a violation. New row TC-DECL-16b shows the `@NamedInterface` contrast. The fixed IDE must read bare `@ApplicationModule` types |
| R2 | B.2: `com.other.billing` is a second module with the id `billing` | `ApplicationModules.of()` throws `IllegalStateException: Duplicate key billing` | `com.other.billing` declares `id = "other-billing"`. TC-SRC-13 is a snippet that removes the id |
| R3 | TC-NEST-04/05/06: nested → parent internal, sibling → sibling root, parent → nested root are legal | Legal only when the origin module declares **no** `allowedDependencies`. A declared list is checked first, and parent or sibling modules missing from it are violations | `order.api` (list `[inventory]`) no longer uses `OrderRepository` or `OrderController`. TC-NEST-04 and TC-NEST-06 are negative snippets. TC-NEST-05 is committed in the legal direction (`order.web → order.api`); the B.2 direction is TC-NEST-05b |
| R4 | B.2: `OrderService` injects `InventoryApi` while `inventory` listens to `OrderCompleted` | `verify()` rejects cycles between closed modules: `Cycle detected: Slice inventory -> Slice order -> Slice inventory` | `order.api.OrderApiFacade` is the `InventoryApi` consumer (TC-DEP-01). TC-API-19 runs from `order.api`. TC-NEST-05b, TC-NEST-06, NC-NEST-02 and NC-NEST-04 also report a cycle; the IDE has no cycle check |
| R5 | TC-SRC-10: with `explicitly-annotated`, `inventory` and `notes` are not modules | `notes` **is** a module (R1). `inventory` is not, so the `"inventory"` entries of `order`, `order.api` and `reporting` make `verify()` throw `IllegalArgumentException: Invalid explicit module dependency in order! No module found with name 'inventory'.` | TC-SRC-10, TC-SRC-11 and TC-LIVE-06 expect the three declarations to be reported as unknown |
| R6 | TC-EVT-05: removing `events-api` breaks only the `@ApplicationModuleListener` import | `events-api` is the only source of `spring-tx` on the compile classpath. `LegacyListener` loses `@Transactional` and `@TransactionalEventListener` too | TC-EVT-05 expects unresolved imports in both listeners |
| R7 | TC-API-07/15: `class Q : OrderSpi` and `OrderSpi by r` are flagged | `OrderSpi` is exposed through the named interface `spi`, so both are **legal** | New internal interface `order.internal.OrderCallback` carries the violation. The `OrderSpi` lines are controls |
| R8 | TC-QF-03: "Accept also: a `package-info.java` created under `src/main/java`" | For `order`, which has a Kotlin carrier, that file is a second carrier: `IllegalStateException: Expected maximum of one type in package com.example.order to be annotated with o.s.m.ApplicationModule` | Rejected for `order`. Still acceptable for `inventory`, which has no carrier (TC-QF-03b) |
| R9 | C.5: the runtime reports a violation for every row | Bytecode keeps no trace of an unused import, an `as` cast, a local variable type, or a typealias declaration. These are **legal** at runtime | TC-API-01, the cast and local parts of TC-API-05, and the declaration in TC-API-11 are IDE-only. Flagging them is consistent with the Java inspection, not a mismatch |
| R10 | TC-DEP-08: `"a :: b :: c"` is malformed | The runtime splits on `::` and reads two segments. `"a :: b :: c"` fails only because module `a` does not exist; `"catalog :: api :: extra"` is **accepted** as `catalog :: api` | New row TC-DEP-08c |
| R11 | TC-SRC-01: `reporting → OrderRepository` is not flagged in the baseline (B4) | `OrderRepository` is internal to `order`; the UAST inspection reports it with or without the Java carrier | Added `reporting → catalog.CatalogService`: an exposed type that only the Java carrier forbids. That reference isolates B4 |
| R12 | B.3 template: `// TC-DEP-01, TC-DEP-05` on the list of `order` | TC-DEP-05 is the empty list of `billing` | Marker is `// TC-DEP-01, TC-DEP-02, TC-DEP-04` |
| R13 | — | `sharedModules = ["shared"]` adds `shared` to every declared list; runtime messages end with `Allowed targets: …, shared.` | An IDE message without `shared` is fine. A flagged `shared` target is a bug |
| R14 | TC-X-01: nested carrier class, record | A nested `@PackageInfo @NamedInterface("inner") class Meta` is a carrier. In a sub-package it creates `inner`. In the module **base package** the unnamed interface becomes empty and `inner` is not registered, so the module exposes nothing | TC-X-01 records both placements |
| R15 | TC-X-06: jMolecules `@Module` on a Kotlin carrier | `org.jmolecules.ddd.annotation.Module` 1.10.0 cannot target a Kotlin class: `This annotation is not applicable to target 'class'` | TC-X-06 only checks that the IDE does not crash |

Other changes, without contradicting `test.md`:
- Gradle 8.14.6 instead of 9.x. Boot 3.5 supports Gradle 7.6.4+ and 8.x, and KGP 2.2.21 is tested up to 8.14.
- `gradle/gradle-daemon-jvm.properties` pins the daemon to JDK 21. The IDE project SDK on the machine that built this is 25, and Gradle 8.14 does not run on it.
- `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` avoids a Gradle deprecation warning.
- `ModulithTests` also asserts `other-billing` and prints the violations of the unrestricted model. `RuntimeProbe` is new (§6).

---

## 3. Project under test

### 3.1 Build

| Item | Value (checked on Maven Central and services.gradle.org, 2026-10-12) |
|---|---|
| Kotlin | `2.2.21` (latest 2.2.x): `kotlin("jvm")`, `kotlin("plugin.spring")`, `-Xjsr305=strict` |
| Spring Boot | `3.5.16` (latest 3.5.x), `io.spring.dependency-management` `1.1.7` |
| Spring Modulith | `spring-modulith-bom:1.4.13` (latest 1.4.x); `spring-modulith-starter-core`, `spring-modulith-events-api` (listed by `./gradlew dependencies --configuration compileClasspath` ✔), `spring-modulith-starter-test` |
| Gradle | wrapper `8.14.6`, Kotlin DSL; daemon JVM 21 |
| JDK | toolchain 21 |
| Second profile (Boot 4.0 + Modulith 2.0) | not created; optional in B.1 |

### 3.2 Module structure

Paths are relative to `src/main/kotlin/com/example/` unless they start with `src/`. `[K]` Kotlin carrier, `[J]`
`package-info.java`, `(ni)` named interface. A `// TC-…` marker sits on every line named in the tables.

```
Application.kt              @Modulithic(systemName = "Kotlin Modulith", sharedModules = ["shared"], additionalPackages = ["com.other"])
                            @SpringBootApplication class Application; fun main() in the facade ApplicationKt
ModuleIds.kt                object ModuleIds { const val INVENTORY = "inventory"; const val CATALOG = "catalog" }
order/                      [K] "order", CLOSED, "Order Module", allowedDependencies = [ModuleIds.INVENTORY, "catalog :: api", "shared :: *"]
├── OrderService.kt         → catalog.api.CatalogApi, shared.SharedService, shared.internal.SharedInternal; publishes OrderCompleted
├── OrderCompleted.kt       data class OrderCompleted(val orderId: String)            event, exposed
├── OrderFunctions.kt       fun newOrderId(): String                                   facade OrderFunctionsKt, exposed
├── internal/               OrderRepository (+ companion create()); OrderInternals.kt: internalHelper(), someProperty,
│                           String.internalExt(), object OrderInternalObject, typealias RepositoryAlias, interface OrderCallback;
│                           OrderTag.kt: annotation class OrderTag, sealed interface OrderState, enum class OrderKind
├── spi/                    [K] (ni "spi") @PackageInfo @NamedInterface("spi") class ModuleMetadata · OrderSpi
├── web/                    [K] nested "order.web", "Order Web", no list
│   ├── OrderController.kt  → order.internal.OrderRepository, catalog.CatalogService, order.api.OrderApiFacade
│   ├── internal/OrderWebInternal.kt
│   └── details/            [K] nested "order.web.details" (display "Details") · OrderWebDetail → order.internal.OrderRepository
└── api/                    [K] nested "order.api" (display "Api"), allowedDependencies = [ModuleIds.INVENTORY]
    ├── OrderApiFacade.kt   → inventory.InventoryApi
    └── internal/ApiSecret.kt
inventory/                  no carrier: "inventory", no list
├── InventoryApi.kt · InventoryEvents.kt (@ApplicationModuleListener) · LegacyListener.kt (@Async @Transactional(readOnly = true)
│                           @TransactionalEventListener(id = "legacy")) · InventoryInternalVisible.kt (internal) · InventoryPrivate.kt (private)
└── internal/               InventoryRepository (implements InventoryApi) · StockLevel
catalog/                    [K] object ModuleMetadata, id = ModuleIds.CATALOG, "Catalog" · CatalogService (implements CatalogApi, → pricing.internal.PriceTable)
├── api/                    [K] (ni "api") @PackageInfo @NamedInterface(name = ["api"]) class ApiMarker · CatalogApi
├── events/                 @NamedInterface("events") data class CatalogChanged · CatalogEventsInternal
├── legacy/                 [K] (ni "legacy", "compat") · LegacyCatalog
└── internal/CatalogRepository.kt
shared/                     [K] type = ApplicationModule.Type.OPEN, shared module · SharedService · internal/SharedInternal
billing/                    [K] allowedDependencies = [] · BillingService (no cross-module reference)
pricing/                    [K] type = Type.OPEN (import of the nested enum) · internal/PriceTable
notes/                      bare carrier, no @PackageInfo: "Notes (bare carrier)", allowedDependencies = [] · NotesService (no cross-module reference)
reporting/ReportingService.kt   → order.internal.OrderRepository, catalog.CatalogService   ILLEGAL at runtime, excluded from the main verify()
src/main/java/com/example/reporting/package-info.java   [J] @ApplicationModule(displayName = "Reporting", allowedDependencies = {"inventory"})
src/main/kotlin/com/other/billing/                      [K] id = "other-billing", "Other Billing" · OtherBillingService   (additional package)
src/main/resources/application.properties               spring.modulith.detection-strategy unset
src/test/kotlin/com/example/ModulithTests.kt            B.3 tests · RuntimeProbe.kt: prints model and violations
```

### 3.3 Runtime model

The oracle for the VIEW, DEP, and DECL rows. Observed on the committed state (`ApplicationModules.of(Application::class.java)`), ✔ 2026-10-12.
"no list" = unrestricted; "+shared" = added through `sharedModules`.

| Module id | Display name | Base package | Parent | Type | Allowed dependencies | Named interfaces and their types |
|---|---|---|---|---|---|---|
| `order` | Order Module | `com.example.order` | – | CLOSED | `inventory`, `catalog :: api`, `shared :: *`, +shared | unnamed: `ModuleMetadata`, `OrderCompleted`, `OrderFunctionsKt`, `OrderService` · `spi`: `ModuleMetadata`, `OrderSpi` |
| `order.web` | Order Web | `com.example.order.web` | `order` | CLOSED | no list | unnamed: `ModuleMetadata`, `OrderController` |
| `order.web.details` | Details | `com.example.order.web.details` | `order.web` | CLOSED | no list | unnamed: `ModuleMetadata`, `OrderWebDetail` |
| `order.api` | Api | `com.example.order.api` | `order` | CLOSED | `inventory`, +shared | unnamed: `ModuleMetadata`, `OrderApiFacade` |
| `inventory` | Inventory | `com.example.inventory` | – | CLOSED | no list | unnamed: `InventoryApi`, `InventoryEvents`, `InventoryInternalVisible`, `LegacyListener` |
| `catalog` | Catalog | `com.example.catalog` | – | CLOSED | no list | unnamed: `CatalogService`, `ModuleMetadata` · `api`: `ApiMarker`, `CatalogApi` · `compat`, `legacy`: `LegacyCatalog`, `ModuleMetadata` · `events`: `CatalogChanged` |
| `shared` | Shared | `com.example.shared` | – | OPEN | no list | every type |
| `billing` | Billing | `com.example.billing` | – | CLOSED | none, +shared | unnamed: `BillingService`, `ModuleMetadata` |
| `pricing` | Pricing | `com.example.pricing` | – | OPEN | no list | every type |
| `notes` | Notes (bare carrier) | `com.example.notes` | – | CLOSED | none, +shared | unnamed: `ModuleMetadata`, `NotesService` |
| `reporting` | Reporting | `com.example.reporting` | – | CLOSED | `inventory`, +shared | unnamed: `ReportingService` |
| `other-billing` | Other Billing | `com.other.billing` | – | CLOSED | no list | unnamed: `ModuleMetadata`, `OtherBillingService` |

Every carrier type belongs to an interface (A.4 item 4). `InventoryInternalVisible` (Kotlin `internal`) is exposed;
`InventoryPrivate` (Kotlin `private`) is not.

---

## 4. Environment and procedure

| Item | Value |
|---|---|
| IntelliJ IDEA | Ultimate: one build with the IDEA-373094 fix and one pre-fix build for the baseline; one project copy per build |
| Plugin | Spring Modulith (bundled), enabled |
| Inspections | *Settings → Editor → Inspections → Spring Modulith*: all three enabled, default severity |
| Import | Open as a Gradle project; Gradle JVM 21 (selected by the daemon criteria file); wait for sync and indexing |
| Debug log | Enable `#com.intellij.spring.modulith` in *Help → Diagnostic Tools → Debug Log Settings* before group LIVE |

How to observe:
- **Runtime legal:** PASS = no Spring Modulith error on the marked reference and none in *Problems*.
- **Runtime violation:** PASS = the error is shown on the marked reference.
- Hover the reference and press `Alt+Enter`. Run *Code | Inspect Code* on the module once per build as a second observation.
- Wait for indexing and for the 500 ms model debounce after every edit.
- **Snippets:** apply one, observe, run `./gradlew test` when the row says so, then revert with
  `git checkout -- . && git clean -fd`.
- After every group, open *Help → Show Log* and filter on `com.intellij.spring.modulith`. Any exception is a finding.

Runtime messages below are quoted from `verify()`. The IDE words them differently, for example
`Module 'inventory' depends on non-exposed type 'com.example.order.internal.OrderRepository' from module 'order'`.
Compare meaning, not text, and write the IDE text into the matrix.

---

## 5. Test cases

### 5.1 DECL — module declaration in Kotlin

| ID | Scenario | Where / snippet | Runtime (1.4.13) | IDE expected | Baseline prediction |
|---|---|---|---|---|---|
| TC-DECL-01 | `@ApplicationModule @PackageInfo class ModuleMetadata` declares a top-level module | `order/ModuleMetadata.kt:12` | ✔ module `order`, display `Order Module` | Project view: `order` has the module icon and the location text `Order Module`; Structure view lists id `order` | Icon yes; no location text; no allowed dependencies in Structure |
| TC-DECL-02 | `displayName` read from Kotlin | `order/ModuleMetadata.kt:8`, `catalog/ModuleMetadata.kt:7` | ✔ `Order Module`, `Catalog`; also `Order Web`, `Other Billing`, `Notes (bare carrier)` | Location text equals `displayName` | Capitalized directory name only |
| TC-DECL-03 | `id` from a `const val` | `catalog/ModuleMetadata.kt:7` (`ModuleIds.kt:5`); then rename the package `catalog` → `catalogue` (*Refactor → Rename package*) | ✔ id `catalog`. After the rename: id still `catalog`, base package `com.example.catalogue`, `"catalog :: api"` still resolves, `verify()` passes | Before and after the rename: `"catalog :: api"` in `order` resolves; Structure shows `Id: catalog` | Falls back to the directory name. Passes by accident before the rename; after it the module is `catalogue` |
| TC-DECL-04 | Carrier is an `object` | `catalog/ModuleMetadata.kt:9` | ✔ detected | Same as a class | Same as a class |
| TC-DECL-05 | Carrier with a free name (`ApiMarker`) | `catalog/api/ApiMarker.kt:7` | ✔ ni `api`: `ApiMarker`, `CatalogApi` | Named-interface icon on `catalog/api`; Structure lists `api` with both types | Works (named interfaces already read `@PackageInfo` types) |
| TC-DECL-06 | `type = ApplicationModule.Type.OPEN` | `shared/ModuleMetadata.kt:6`; reference `order/OrderService.kt:13` | ✔ `shared` OPEN; the reference to `SharedInternal` is legal | `shared/internal` green; `OrderService.kt:13` not flagged; Structure shows `OPEN` | **Red icon; `OrderService.kt:13` flagged (false positive)** |
| TC-DECL-06b | `type = Type.OPEN` through `import org.springframework.modulith.ApplicationModule.Type` | `pricing/ModuleMetadata.kt:7`; reference `catalog/CatalogService.kt:11` | ✔ `pricing` OPEN; the reference to `PriceTable` is legal | Same as TC-DECL-06 | **Same false positive on `CatalogService.kt:11`**; B5 must handle both spellings |
| TC-DECL-07 | Bare carrier without `@PackageInfo` (R1) | `notes/ModuleMetadata.kt:7`; [S-DECL-07](#s-decl-07) | ✔ **read**: display `Notes (bare carrier)`, allowed none (+shared). Snippet: `Module 'notes' depends on module 'inventory' via com.example.notes.NotesService -> com.example.inventory.InventoryApi. Allowed targets: shared.` | Agrees with the runtime: location text `Notes (bare carrier)`; the snippet is flagged "Module 'notes' has no allowed dependencies" | **No location text; snippet not flagged** |
| TC-DECL-08 | Two `@PackageInfo` types with `@ApplicationModule` in one package | [S-DECL-08](#s-decl-08) | ✔ `IllegalStateException: Expected maximum of one type in package com.example.order to be annotated with o.s.m.ApplicationModule, but got [...]` from `ApplicationModules.of()`; all three B.3 tests fail | No exception in the IDE log; one declaration wins; ideally a warning on both types | Model ignores both |
| TC-DECL-09 | `private class ModuleMetadata` in `order/spi` (package-private in bytecode) | [S-DECL-09](#s-decl-09) | ✔ ni `spi` still detected, now only `OrderSpi`; the consumer `inventory/SpiProbe.kt` is legal | ni `spi` still detected; consumer not flagged | **`isPublicClass` filter: ni lost, `OrderSpi` internal, consumer flagged (false positive)** |
| TC-DECL-10 | `internal class ModuleMetadata` in `order/spi` | `order/spi/ModuleMetadata.kt:8` → `internal class ModuleMetadata` | ✔ ni `spi`: `ModuleMetadata`, `OrderSpi` | Detected | Detected (public in bytecode) |
| TC-DECL-11 | The carrier is exposed API (A.4 item 4) | [S-DECL-11](#s-decl-11) | ✔ legal | Not flagged | Not flagged |
| TC-DECL-12 | `arrayOf(...)` instead of `[...]` | `order/ModuleMetadata.kt:9` → `allowedDependencies = arrayOf(ModuleIds.INVENTORY, "catalog :: api", "shared :: *"),` | ✔ same list as `[...]` | Same as `[...]` (Structure, completion, validation) | List ignored |
| TC-DECL-13 | `allowedDependencies = "inventory"` without brackets | `order/ModuleMetadata.kt:9` → `allowedDependencies = "inventory",` | ✔ compile error `Argument type mismatch: actual type is 'String', but 'Array<String>' was expected.` | Kotlin error shown; no plugin exception; model unchanged | Same |
| TC-DECL-14 | `@NamedInterface(name = "api")` without brackets | `catalog/api/ApiMarker.kt:7` → `@NamedInterface(name = "api")` | ✔ the same compile error | Kotlin error; no plugin exception | Same |
| TC-DECL-15 | `@NamedInterface("legacy", "compat")`: two names for one package | `catalog/legacy/ModuleMetadata.kt:7` | ✔ ni `legacy` and `compat`, both `LegacyCatalog`, `ModuleMetadata` | Structure lists both; `"catalog :: compat"` completes and validates | Both detected; **completion absent (B6)** |
| TC-DECL-16 | `@PackageInfo @NamedInterface class ModuleMetadata` without a name | `order/spi/ModuleMetadata.kt:7` → `@NamedInterface` | ✔ ni named `spi` after the package: `ModuleMetadata`, `OrderSpi` | Same | Same |
| TC-DECL-16b | *New (R1).* `@NamedInterface("spi")` **without** `@PackageInfo` | delete `order/spi/ModuleMetadata.kt:6` (`@PackageInfo`) | ✔ type-level ni: `spi` = `ModuleMetadata` only; `OrderSpi` becomes internal | Same as the runtime: no named-interface icon on `order/spi`; the S-DECL-09 consumer `SpiProbe.kt` is flagged | Same (type-level `@NamedInterface` is read) |
| TC-DECL-17 | `@NamedInterface("events")` on a type exposes only that type | `catalog/events/CatalogChanged.kt:5`; [S-DECL-17](#s-decl-17) | ✔ ni `events`: `CatalogChanged`. **a)** `Module 'order' depends on named interface(s) 'catalog :: events' via com.example.order.CatalogEventsProbe -> com.example.catalog.events.CatalogChanged. Allowed targets: inventory, catalog :: api, shared :: *, shared.` and `Module 'order' depends on non-exposed type com.example.catalog.events.CatalogEventsInternal within module 'catalog'!` **b)** with `"catalog :: events"` in the list: legal | a) both references flagged; b) not flagged | a) **`CatalogChanged` not flagged** (list ignored), `CatalogEventsInternal` flagged; b) not flagged |
| TC-DECL-18 | `@file:JvmName("OrderUtils")` on a facade in the module root | insert `@file:JvmName("OrderUtils")` before line 1 of `order/OrderFunctions.kt` | ✔ unnamed interface lists `OrderUtils` instead of `OrderFunctionsKt` | Structure lists `OrderUtils` as an exposed type | Same |

### 5.2 NEST — nested modules declared in Kotlin

The rules are those of the Java scenario (IDEA-383778, IDEA-376940), plus R3: a declared list removes the implicit parent and sibling access.

| ID | Scenario | Where / snippet | Runtime (1.4.13) | IDE expected | Baseline prediction |
|---|---|---|---|---|---|
| TC-NEST-01 | Nested module from a Kotlin carrier | `order/web/ModuleMetadata.kt:6` | ✔ `order.web`, parent `order` | Nested-module icon; Structure lists `order.web` | **Internal package of `order`; `order.web` absent** |
| TC-NEST-02 | Nested → parent internal | `order/web/OrderController.kt:15` | ✔ legal | Not flagged | Not flagged (same module in the baseline model) |
| TC-NEST-03 | Grandchild → grandparent internal | `order/web/details/OrderWebDetail.kt:8` | ✔ legal | Not flagged | Not flagged |
| TC-NEST-04 | *Changed (R3).* Nested module **with its own list** → parent internal | [S-NEST-04](#s-nest-04) | ✔ **violation** `Module 'order.api' depends on module 'order' via com.example.order.api.ParentInternalProbe -> com.example.order.internal.OrderRepository. Allowed targets: inventory, shared.` | Flagged. If not, repeat with a Java `package-info.java` carrying the same list; if Java is silent too, file a separate, language-independent issue | Not flagged |
| TC-NEST-05 | *Direction changed (R3).* Sibling nested → sibling root type | `order/web/OrderController.kt:17` (`order.web → order.api.OrderApiFacade`) | ✔ legal | Not flagged | Not flagged |
| TC-NEST-05b | *B.2 direction.* Sibling with its own list → sibling root type | [S-NEST-05b](#s-nest-05b) | ✔ violation `Module 'order.api' depends on module 'order.web' via com.example.order.api.SiblingRootProbe -> com.example.order.web.OrderController. Allowed targets: inventory, shared.` and `Cycle detected: order.api -> order.web -> order.api` | Flagged | Not flagged |
| TC-NEST-06 | *Changed (R3).* Parent with a list → nested root type | [S-NEST-06](#s-nest-06) | ✔ violation `Module 'order' depends on module 'order.web' via com.example.order.OrderService -> com.example.order.web.OrderController. Allowed targets: inventory, catalog :: api, shared :: *, shared.` and cycles `order -> order.web -> order`, `inventory -> order -> order.web -> order.api -> inventory` | Flagged; record the message | Not flagged |
| TC-NEST-07 | Nested module with its own `allowedDependencies` | `order/api/ModuleMetadata.kt:7`, positive `order/api/OrderApiFacade.kt:9`; [S-NEST-07](#s-nest-07) | ✔ `OrderApiFacade.kt:9` legal. Snippet: `Module 'order.api' depends on named interface(s) 'catalog :: api' via com.example.order.api.CatalogProbe -> com.example.catalog.api.CatalogApi. Allowed targets: inventory, shared.` | `OrderApiFacade.kt:9` not flagged; snippet flagged "Module 'order.api' depends on: 'catalog :: api'. But allowed dependencies are: ['inventory']" | **Snippet not flagged (B1, B2)** |
| TC-NEST-08 | The parent's list does not apply to the nested module | `order/web/OrderController.kt:16` (`catalog.CatalogService`; `order` allows only `catalog :: api`) | ✔ legal (`order.web` has no list) | Not flagged | Not flagged, because the list of `order` is unknown. After the fix the list must not leak into `order.web` |
| NC-NEST-01 | Unrelated top-level → nested internal | [S-NC-NEST-01](#s-nc-nest-01) | ✔ violation `Module 'billing' depends on module 'order.web' via com.example.billing.NestedInternalProbe -> com.example.order.web.internal.OrderWebInternal. Allowed targets: shared.` | Flagged (`billing` allows nothing) | Flagged as a non-exposed type of `order` (different message) |
| NC-NEST-02 | Unrelated top-level → nested root type | [S-NC-NEST-02](#s-nc-nest-02) | ✔ violation `Invalid sub-module reference from module 'inventory' to module 'order.web' (via c.e.i.NestedRootProbe -> c.e.o.w.OrderController)!` and `Cycle detected: inventory -> order.web -> order.api -> inventory` | Flagged; record the message | **Not flagged** (`order.web` is not a module) |
| NC-NEST-03 | Sibling nested → sibling internal | [S-NC-NEST-03](#s-nc-nest-03) | ✔ violation `Module 'order.web' depends on non-exposed type com.example.order.api.internal.ApiSecret within module 'order.api'!` | Flagged | **Not flagged** |
| NC-NEST-04 | Parent → nested internal | [S-NC-NEST-04](#s-nc-nest-04) | ✔ violation `Module 'order' depends on module 'order.web' via com.example.order.NestedInternalProbe -> com.example.order.web.internal.OrderWebInternal. Allowed targets: inventory, catalog :: api, shared :: *, shared.` and the cycles of TC-NEST-06 | Flagged | **Not flagged** |

### 5.3 DEP — `allowedDependencies` in Kotlin

| ID | Scenario | Where / snippet | Runtime (1.4.13) | IDE expected | Baseline prediction |
|---|---|---|---|---|---|
| TC-DEP-01 | Module id from a `const val` in an `object` | `order/ModuleMetadata.kt:9`, `order/api/ModuleMetadata.kt:7`; use `order/api/OrderApiFacade.kt:9` | ✔ allowed. The B.2 placement ([S-DEP-01b](#s-dep-01b)) passes the list check but produces `Cycle detected: inventory -> order -> inventory` (R4) | `OrderApiFacade.kt:9` not flagged; Structure lists `inventory` under `order` and `order.api` | Not flagged (list ignored) |
| TC-DEP-02 | Named-interface dependency `"catalog :: api"` | `order/OrderService.kt:11` | ✔ allowed | Not flagged | Not flagged |
| TC-DEP-03 | Same module, non-listed type | [S-DEP-03](#s-dep-03) | ✔ violation `Module 'order' depends on module 'catalog' via com.example.order.OrderService -> com.example.catalog.CatalogService. Allowed targets: inventory, catalog :: api, shared :: *, shared.` | Flagged "depends on: 'catalog'. But allowed dependencies are: […]" with the fix *Add 'catalog' as an allowed dependency of the 'order' module* | **Not flagged** |
| TC-DEP-04 | Wildcard `"shared :: *"` | `order/OrderService.kt:12` | ✔ allowed (also through `sharedModules`) | Not flagged | Not flagged |
| TC-DEP-05 | Empty list `[]` blocks everything | `billing/ModuleMetadata.kt:6`; [S-DEP-05](#s-dep-05) | ✔ violation `Module 'billing' depends on module 'inventory' via com.example.billing.BillingService -> com.example.inventory.InventoryApi. Allowed targets: shared.` | Flagged "Module 'billing' has no allowed dependencies" | **Not flagged** |
| TC-DEP-06 | Unknown module `"warehouse"` | append `, "warehouse"` to the list in `order/ModuleMetadata.kt:9` | ✔ `verify()` throws `IllegalArgumentException: Invalid explicit module dependency in order! No module found with name 'warehouse'.`; tests 2 and 3 fail | String highlighted "Module 'warehouse' does not exist" | **Nothing (B7)** |
| TC-DEP-07 | Unknown named interface `"catalog :: nope"` | append `, "catalog :: nope"` | ✔ `IllegalArgumentException: No named interface named 'nope' found! Original dependency declaration: order -> catalog :: nope.` | "Named interface 'nope' does not exist" | Nothing |
| TC-DEP-08 | Malformed `"a :: b :: c"` and empty `""` | append `, "a :: b :: c"`; in a second run `, ""` | ✔ `… No module found with name 'a'.`; `IllegalArgumentException: Module dependency identifier must not be null or empty!` | "Malformed dependency identifier"; "Module dependency identifier can't be empty" | Nothing |
| TC-DEP-08c | *New (R10).* Extra segment on a valid id | replace `"catalog :: api"` with `"catalog :: api :: extra"` | ✔ **accepted** as `catalog :: api`; `verify()` passes | Record. "Malformed dependency identifier" is acceptable (stricter than the runtime); an exception or a broken model is not | Nothing |
| TC-DEP-09 | Whitespace variants `"catalog::api"`, `" catalog :: api "` | [S-DEP-09](#s-dep-09) (shared by TC-DEP-09 to TC-DEP-12) | ✔ accepted, both parsed as `catalog :: api` | Accepted, no error | Nothing |
| TC-DEP-10 | String template of constants `"${ModuleIds.CATALOG} :: api"` | same snippet | ✔ accepted (a compile-time constant) | Accepted; if the IDE cannot fold the template, report a bug | Nothing |
| TC-DEP-11 | Duplicate entry `"inventory"` next to `ModuleIds.INVENTORY` | same snippet | ✔ accepted, collapsed | Accepted; completion no longer offers `inventory` | Nothing |
| TC-DEP-12 | Self reference `"order"` in `order` | same snippet | ✔ accepted: `inventory, catalog :: api, shared :: *, order, shared`; no violation | No crash; record the result | Nothing |
| TC-DEP-13 | Nested id `"order.web"` from `billing` | [S-DEP-13](#s-dep-13) | ✔ declaration accepted (`order.web`, +shared); the reference: `Invalid sub-module reference from module 'billing' to module 'order.web' (via c.e.b.NestedRootProbe -> c.e.o.w.OrderController)!` | Id resolves (no "does not exist"); the access is still reported | Nothing |
| TC-DEP-14 | Completion in `allowedDependencies = ["<caret>"]` | `billing/ModuleMetadata.kt:6` | n/a (ids and named interfaces in §3.3) | Every top-level id except `billing`: `catalog`, `inventory`, `notes`, `order`, `other-billing`, `pricing`, `reporting`, `shared`. Every named interface: `catalog :: api`, `catalog :: compat`, `catalog :: events`, `catalog :: legacy`, `order :: spi`. The `<id> :: *` items that the Java completion offers for the same model (`test.md` names `catalog :: *`, `inventory :: *`, `order :: *`). **No** `billing`, no nested ids | **No items (B6)** |
| TC-DEP-15 | Completion excludes declared entries | `order/ModuleMetadata.kt:9`, add `, "<caret>"` | n/a | `inventory`, `catalog :: api`, `shared :: *` absent; `catalog :: *` and `shared` present | No items |
| TC-DEP-16 | Completion in a nested carrier | `order/api/ModuleMetadata.kt:7`, add `, "<caret>"` | n/a | `order.web` present (sibling); `order.web.details` absent; `inventory` absent (declared) | No items |
| TC-DEP-17 | Ctrl+B on `"catalog :: api"` and on `ModuleIds.INVENTORY` | `order/ModuleMetadata.kt:9` | n/a | `"catalog :: api"` selects `com.example.catalog.api` in the Project view; on `INVENTORY` Kotlin goes to the `const val`; record whether the module reference is also offered | **No reference (B8)** |
| TC-DEP-18 | Rename the `inventory` package to `stock` (*Refactor → Rename package*) | whole project | ✔ the module becomes `stock`; `ModuleIds.INVENTORY` is still `"inventory"`: `IllegalArgumentException: Invalid explicit module dependency in order! No module found with name 'inventory'.` (also `order.api`, `reporting`) | The three declarations that evaluate to `"inventory"` are reported "Module 'inventory' does not exist"; completion offers `stock` | Kotlin: nothing (B7). `reporting/package-info.java`: reported by the Java inspection |

### 5.4 API — restricted module API usage, Kotlin reference kinds

Every row references `com.example.order.internal.*` from `com.example.inventory` (no list). The runtime message is
`Module 'inventory' depends on non-exposed type com.example.order.internal.<Type> within module 'order'!`. For every
row that the runtime flags, the IDE expectation is `Module 'inventory' depends on non-exposed type
'com.example.order.internal.<Type>' from module 'order'` with the three quick fixes. Apply [S-API](#s-api): one class
per row, each line marked with its id.

| ID | Reference kind | Snippet | Runtime (1.4.13): what the bytecode shows | Baseline prediction |
|---|---|---|---|---|
| TC-API-01 | import directive alone | `inventory/ApiProbe01.kt` | ✔ **legal**: an unused import leaves no bytecode (R9). IDE-only row: flagged, as in Java | flagged |
| TC-API-02 | constructor property | `ApiProbe02` | ✔ violation: field, constructor parameter, method call | flagged |
| TC-API-03 | parameter and return type | `ApiProbe03` | ✔ violation: parameter and return type | flagged |
| TC-API-04 | generic argument | `ApiProbe04` | ✔ violation: "has generic type <java.util.List<…OrderRepository>> with type argument depending on …" | flagged |
| TC-API-05 | `is`, `as`, explicit local type | `ApiProbe05Is`, `ApiProbe05Cast`, `ApiProbe05Local` | ✔ `is`: violation (instanceof). `as` and the local type: **legal**, not in bytecode (R9). IDE: flag all three | flagged |
| TC-API-06 | class literal | `ApiProbe06` | ✔ violation: "references class object" | flagged |
| TC-API-07 | inheritance and interface implementation | `ApiProbe07Extends` (`OrderRepository` is open through `kotlin-spring`), `ApiProbe07Implements` (`OrderCallback`); control `ApiProbe07Spi` | ✔ violation for `OrderRepository` (extends, constructor call) and `OrderCallback`. Control **legal** (R7) | flagged; control not flagged |
| TC-API-08 | `object` member call | `ApiProbe08` | ✔ violation: `OrderInternalObject.INSTANCE` and `describe()` | flagged (qualifier resolves to the class) |
| TC-API-09 | companion call | `ApiProbe09` | ✔ violation on `OrderRepository` and `OrderRepository$Companion` | flagged |
| TC-API-10 | annotation | `ApiProbe10` | ✔ violation: "is annotated with <…OrderTag>" | flagged |
| TC-API-11 | typealias | `typealias ApiProbeRepo = OrderRepository` and `ApiProbe11`; variant `ApiProbe11Internal` uses `order.internal.RepositoryAlias` | ✔ the declaration is legal (no class, R9); every **use** is a violation on the expanded `OrderRepository` | declaration flagged; use of the alias: record |
| TC-API-12 | **top-level function call** | `ApiProbe12` (`internalHelper()`) | ✔ violation on the facade `com.example.order.internal.OrderInternalsKt` | **not flagged** (resolves to a method, A.4 item 8). After the fix: flagged at the import or the call |
| TC-API-13 | top-level property, extension function | `ApiProbe13Property` (`someProperty`, deliberately not `const`), `ApiProbe13Extension` (`"x".internalExt()`) | ✔ violation on `OrderInternalsKt` (`getSomeProperty()`, `internalExt(String)`) | **not flagged** (as TC-API-12) |
| TC-API-14 | enum entry, sealed subtype | `ApiProbe14Enum`, `ApiProbe14Sealed` | ✔ violation on `OrderKind`, `OrderState`, `OrderState$Paid`, `OrderState$New` | flagged on the type qualifier |
| TC-API-15 | lambda type, delegation | `ApiProbe15Lambda`, `ApiProbe15Delegation` (`OrderCallback by r`); control `ApiProbe15Spi` | ✔ violation (generic `Function1` field, synthetic lambda method, `$$delegate_0`). Control **legal** (R7) | flagged; control not flagged |
| TC-API-16 | inferred type only | `ApiProbe16` | ✔ violation (companion call, return type) | flagged on the qualifier; the inferred type itself has no reference |
| TC-API-17 | Kotlin test source root | `src/test/kotlin/com/example/inventory/TestApiProbe.kt` | ✔ legal (`verify()` imports production classes only) | **not flagged** (test roots excluded) |
| TC-API-18 | Kotlin script or scratch file | scratch file with the `ApiProbe02` line | n/a (not part of the build) | not flagged |
| TC-API-19 | Kotlin `internal class` as target | `order/api/VisibilityProbe.kt` (from `order.api`; from `order` it closes a cycle, R4) | ✔ legal: `InventoryInternalVisible` is public in bytecode and exposed | not flagged |
| TC-API-20 | top-level `private class` as target | `order/api/PrivateProbe.kt` (apply alone) | ✔ compile error `Cannot access 'class InventoryPrivate : Any': it is private in file.` | Kotlin error; plugin silent |
| TC-API-21 | Java target from Kotlin source | — | skipped: `reporting` has no Java type. Optional with TC-SRC-03: Kotlin → `com.example.audit.AuditService` (not probed) | consistent with Java rules |
| TC-API-22 | Kotlin target from Java source | `src/main/java/com/example/inventory/JavaProbe.java` | ✔ violation (field, constructor parameter) | flagged (regression guard) |

### 5.5 QF — quick fixes on Kotlin code

Setup: [S-QF](#s-qf), then `Alt+Enter` on `OrderRepository`. The runtime cell is the `verify()` result of the code the fix should produce.

| ID | Fix | IDE expected | Runtime of the result (1.4.13) | Baseline prediction | Also check |
|---|---|---|---|---|---|
| TC-QF-01 | *Move 'com.example.order.internal.OrderRepository' to the 'order' module base package* | `OrderRepository.kt` moves to `order/`; imports and references update; error disappears | legal (root types are exposed); not probed | B13: Java move processor on a light class; expect no-op or exception | IDE log for exceptions; undo works |
| TC-QF-02 | *Annotate 'OrderRepository' with '@NamedInterface'* | `@NamedInterface` and its import added to the Kotlin class; error disappears | ✔ legal: ni `internal` = `OrderRepository` (the name defaults to the package) | B11: no-op or exception | offered only when the type is not already exposed |
| TC-QF-03 | *Make the 'order' module open* | Kotlin-aware: `type = ApplicationModule.Type.OPEN` added to `order/ModuleMetadata.kt`. **Reject** a `package-info.java` under `src/main/kotlin` (never compiled) and under `src/main/java` (second carrier, R8) | ✔ `type = OPEN` in the Kotlin carrier: legal. ✔ `package-info.java` under `src/main/kotlin`: ignored, the violation stays. ✔ under `src/main/java`: `IllegalStateException: Expected maximum of one type in package com.example.order …` | **B12: creates `src/main/kotlin/com/example/order/package-info.java`; IDE green, `./gradlew test` red** | if a file is created, run `./gradlew test` |
| TC-QF-03b | Same fix for a module without a carrier (`inventory`) | Kotlin-aware: new `inventory/ModuleMetadata.kt` with `@ApplicationModule(type = ApplicationModule.Type.OPEN) @PackageInfo`. A `package-info.java` under `src/main/java` is also fine | setup [S-QF-03b](#s-qf-03b): `Module 'order.web' depends on non-exposed type com.example.inventory.internal.StockLevel within module 'inventory'!`. ✔ Kotlin carrier with OPEN: legal. ✔ `src/main/java/com/example/inventory/package-info.java` with OPEN: legal | same as TC-QF-03 | |
| TC-QF-04 | *Add 'catalog' as an allowed dependency of the 'order' module* (from TC-DEP-03) | `allowedDependencies = [ModuleIds.INVENTORY, "catalog :: api", "shared :: *", "catalog"]`; the constant stays a constant | ✔ legal: `inventory, catalog :: api, shared :: *, catalog, shared` | **B10: fix absent or silent no-op** | formatting of the array literal; no duplicate import |
| TC-QF-05 | *Simplify with '@ApplicationModuleListener'* on `inventory/LegacyListener.kt:15` | Weak warning on `on`; result `@ApplicationModuleListener(id = "legacy", readOnlyTransaction = true) fun on(...)` with the `events` import; `@Async`, `@Transactional`, `@TransactionalEventListener` and their imports removed | ✔ that result compiles and the Spring context starts | **B9: no warning** | class-level `@Transactional` variant; `propagation` and `condition` are kept; two of three annotations are not reported |
| TC-QF-06 | Intention preview for every fix above | Preview or "no preview", without exception | n/a | Same | |
| TC-QF-07 | Undo after every fix | One undo step restores the file | n/a | Same | |

### 5.6 VIEW — Project view, Structure view, bean completion

| ID | Scenario | IDE expected | Baseline prediction |
|---|---|---|---|
| TC-VIEW-01 | Project view icons | Module icon: `order`, `inventory`, `catalog`, `shared`, `billing`, `pricing`, `notes`, `reporting`, `com/other/billing`. Nested-module icon: `order/web`, `order/web/details`, `order/api`. Named interface (green): `order/spi`, `catalog/api`, `catalog/legacy`. **Red:** `catalog/events` (a type-level ni does not color the package) and every `*/internal` except `shared/internal` and `pricing/internal`, which are green (OPEN). Default folder: `com/example`, `com/other` | Nested → red; OPEN internals → red; no display names |
| TC-VIEW-02 | Display names as location text | `Order Module`, `Catalog`, `Order Web`, `Reporting`, `Other Billing`, `Notes (bare carrier)` | `Reporting` only if gap B4 is closed; otherwise none |
| TC-VIEW-03 | *Flatten packages* and *Compact middle packages* toggles | Icons and text stay correct in both modes | Same |
| TC-VIEW-04 | One package in two source roots (`src/main/kotlin/…/reporting`, `src/main/java/…/reporting`) | Both directory nodes carry the same icon and text | Record |
| TC-VIEW-05 | Structure view on `Application.kt` (`Alt+7`, logical view; `Application.kt:13`) | *Modules* lists every id of §3.3; per module `Id`, `Allowed Dependencies` (children resolve to packages), `OPEN` for `shared` and `pricing`, `Named Interfaces` with their types; carriers listed as exposed types | *Modules* node present; no `Allowed Dependencies`, no `OPEN`, nested modules missing |
| TC-VIEW-06 | Application class in a file with another name (`Main.kt` containing `class Application`) | Same as TC-VIEW-05 | Same |
| TC-VIEW-07 | Bean completion in `billing/BillingService.kt`: `class BillingService(private val <caret>)` and `@Autowired lateinit var <caret>` | Beans of other modules carry the restricted indicator (`billing` allows nothing) | Indicator depends on the access provider; the model has no list, so no indicator |
| TC-VIEW-08 | Compare the Structure view with `runtime-structure.txt` and §3.3 | Identical ids, display names, named interfaces, allowed dependencies | Mismatch on everything in `test.md` A.3 |

### 5.7 EVT — events and implicit usage

| ID | Scenario | Runtime (1.4.13) | IDE expected | Baseline prediction |
|---|---|---|---|---|
| TC-EVT-01 | `@ApplicationModuleListener fun on(event: OrderCompleted)` (`inventory/InventoryEvents.kt:11-12`) | n/a | Neither `on` nor `InventoryEvents` (`:9`) is reported as unused by the Kotlin unused-symbol inspection | Record (B14) |
| TC-EVT-02 | Event type is a Kotlin `data class` in the `order` root (`inventory/InventoryEvents.kt:12`, `LegacyListener.kt:15`) | ✔ legal | Not flagged | Same |
| TC-EVT-03 | Event type moved to `order/internal` | not probed (same rule as TC-API-03) | Flagged in both listener parameters | Same |
| TC-EVT-04 | `LegacyListener.on` with `@Async @Transactional(readOnly = true) @TransactionalEventListener(id = "legacy")` (`inventory/LegacyListener.kt:15`) | n/a | Weak warning; fix in TC-QF-05 | **None** |
| TC-EVT-05 | Remove the `spring-modulith-events-api` line from `build.gradle.kts`, reload Gradle | ✔ compile errors: `Unresolved reference 'events'` in `InventoryEvents.kt`; `Unresolved reference 'transaction'` in `LegacyListener.kt` (R6) | TC-EVT-04 warning disappears; both listeners show unresolved imports (expected, not a plugin issue); no plugin exception | Same |
| TC-EVT-06 | `@ApplicationModuleListener suspend fun on(event: OrderCompleted)` ([S-EVT-06](#s-evt-06)) | ✔ compiles; the Spring context starts | No plugin exception; record what the inspection does | Same |
| TC-EVT-07 | Deprecated twin `org.springframework.modulith.ApplicationModuleListener` on a second listener ([S-EVT-07](#s-evt-07)) | ✔ compiles with `'annotation class ApplicationModuleListener : Any, Annotation' is deprecated. Deprecated in Java.` | Kotlin deprecation warning; the plugin treats the method like any other method (no implicit usage, no simplify offer); the TC-QF-05 result imports the `events` variant. Record whether the twin counts as an entry point | Same |

### 5.8 SRC — source roots, mixed languages, build layout

| ID | Scenario | Runtime (1.4.13) | IDE expected | Baseline prediction |
|---|---|---|---|---|
| TC-SRC-01 | JetBrains blog layout: Kotlin code, carrier `package-info.java` in `src/main/java` (`reporting/ReportingService.kt:9`, `:10`) | ✔ `reporting` = `Reporting`, list `inventory`; both references are violations (`runtime-structure.txt` §2) | Both flagged; location text `Reporting` | `:9` flagged as a non-exposed type (the UAST check needs no carrier, R11); **`:10` not flagged (B4)**; no `Reporting` text |
| TC-SRC-02 | Prove TC-SRC-01 at runtime | ✔ `./gradlew test` green; the third test catches both `reporting` violations | n/a | n/a |
| TC-SRC-03 | Module package only in `src/main/java` ([S-SRC-03](#s-src-03)) | ✔ module `audit` (`Audit`) detected | `audit` in Structure; module icon | **Missing (B4)** |
| TC-SRC-04 | Application class in Java, modules in Kotlin ([S-SRC-04](#s-src-04)) | ✔ the same 12 modules as §3.3 | All detected | **All Kotlin-only module directories missing (B4)** |
| TC-SRC-05 | `package-info.java` under `src/main/kotlin` for `billing` ([S-SRC-05](#s-src-05)) | ✔ ignored: `billing` keeps `Billing` and an empty list | Ignore it too, or warn that the file is not compiled. Silent acceptance (location text `Billing from src/main/kotlin`) is a mismatch to report | Accepted silently |
| TC-SRC-06 | `kotlin { sourceSets.main { kotlin.srcDir("src/main/java") } }`, or one `src/main/java` root with mixed files | not probed | Detected | Detected (one directory) |
| TC-SRC-07 | Maven variant (`kotlin-maven-plugin`, `src/main/kotlin`) | not probed | Same as Gradle | Same |
| TC-SRC-08 | Multi-project build: `app` and `order-module` sharing `com.example` | not probed (the runtime scans the classpath) | Record; the model is per IDE module | Modules of the other subproject missing |
| TC-SRC-09 | Second `@SpringBootApplication` in `main` ([S-SRC-09](#s-src-09)) | ✔ no effect: `ApplicationModules.of(Application::class.java)` names the class | Empty model, no exception; icons revert to default | Same |
| TC-SRC-10 | Append `spring.modulith.detection-strategy=explicitly-annotated` to `application.properties` | ✔ modules `order`, `order.web`, `order.web.details`, `order.api`, `catalog`, `shared`, `billing`, `pricing`, **`notes`**, `reporting`, `other-billing`; not `inventory`. `verify()` throws `IllegalArgumentException: Invalid explicit module dependency in order! No module found with name 'inventory'.` (R5) | Same module set; `inventory` loses its icon; the `"inventory"` declarations of `order`, `order.api`, `reporting` are reported as unknown | **Zero modules, every icon default (B3)** |
| TC-SRC-11 | The same key in `src/main/resources/application.yml`; separately in `application-dev.properties` | ✔ yml: as TC-SRC-10. Profile file: default strategy | yml honored; profile file not honored (production defaults) | Record |
| TC-SRC-12 | `@Modulithic(additionalPackages = ["com.other"])` from Kotlin (`Application.kt:10`) | ✔ `other-billing` (`com.other.billing`) detected | Module icon and `Other Billing` on `com/other/billing` | Record (read through the PSI annotations of the light class); the id stays `billing` (B1) |
| TC-SRC-13 | Duplicate module id across roots ([S-SRC-13](#s-src-13)) | ✔ `IllegalStateException: Duplicate key billing (attempted merging values …)` from `ApplicationModules.of()` (R2) | No exception; ideally a warning on both declarations; `"billing"` resolves to `com.example.billing` | Same; in the committed state the baseline already sees two `billing` ids (B1) |
| TC-SRC-14 | New project wizard: Spring Boot, Kotlin, Gradle Kotlin DSL, *Spring Modulith* (+ *Spring Data JPA*, + *Actuator*) | n/a | `build.gradle.kts` has `spring-modulith-starter-core`, `spring-modulith-starter-test`, `spring-modulith-starter-jpa`, `spring-modulith-actuator`, `spring-modulith-observability` | Same. Data point: the scaffold this project replaced (IDEA 2027.1, Gradle Groovy, *Spring Modulith* only) had `starter-core`, `starter-test`, BOM 2.1.1, Boot 4.1.1 |

### 5.9 LIVE — model updates without restart (IDE only)

| ID | Edit | IDE expected (within about 1 s after typing stops) | Baseline prediction |
|---|---|---|---|
| TC-LIVE-01 | Change `displayName` in `order/ModuleMetadata.kt:8` | Location text updates | No text |
| TC-LIVE-02 | With S-DEP-03 applied, add `"catalog"` to the list in `order/ModuleMetadata.kt:9` | TC-DEP-03 error disappears (runtime ✔ legal, TC-QF-04) | Nothing to change |
| TC-LIVE-03 | Create `order/pricing/ModuleMetadata.kt` ([S-LIVE-03](#s-live-03)) | Directory becomes a nested module; icons switch | Stays red |
| TC-LIVE-04 | Delete that file | Directory reverts to internal | Stays red |
| TC-LIVE-05 | Toggle `type = ApplicationModule.Type.OPEN` in `shared/ModuleMetadata.kt:6` | `shared/internal` icon and the error on `order/OrderService.kt:13` toggle | Stays red |
| TC-LIVE-06 | Toggle `spring.modulith.detection-strategy=explicitly-annotated` | Module set changes as in TC-SRC-10 without restart | Zero modules, then all |
| TC-LIVE-07 | Undo each edit above | Model returns to the previous state | Same |
| TC-LIVE-08 | Edit during indexing (dumb mode) | No exception; model refreshes after indexing | Same |

### 5.10 X — exploratory

| ID | Scenario | Runtime (1.4.13) | What to record in the IDE |
|---|---|---|---|
| TC-X-01 | Nested carrier `class Outer { @PackageInfo @NamedInterface("inner") class Meta }` ([S-X-01](#s-x-01)) | ✔ in `inventory/internal`: ni `inner` = `InventoryRepository`, `Meta`, `Outer`, `StockLevel`. In the base package `inventory`: unnamed interface empty, `inner` not registered, `order.api → InventoryApi` becomes `Module 'order.api' depends on non-exposed type com.example.inventory.InventoryApi within module 'inventory'!` (R14) | The IDE reads only top-level classes: record both placements |
| TC-X-02 | Carrier is an `interface` or an `annotation class`: `billing/ModuleMetadata.kt:8` → `interface ModuleMetadata`; `src/main/kotlin/com/other/billing/ModuleMetadata.kt:10` → `annotation class ModuleMetadata` | ✔ both detected, attributes read | Both detected |
| TC-X-03 | `@PackageInfo @ApplicationModule(displayName = "Root")` on `Application` (`Application.kt`, above `@SpringBootApplication`) | ✔ the root package is not a module; model unchanged | No crash; no `Root` module |
| TC-X-04 | Kotlin composed annotation with `@get:AliasFor(annotation = Modulithic::class, attribute = "additionalPackages")` ([S-X-04](#s-x-04)) | ✔ `additionalPackages` and `sharedModules` honored (`other-billing` detected, `+shared` in the lists) | Commit `693d3abefa8cc` handles Java; check Kotlin |
| TC-X-05 | Id with Kotlin-illegal characters: `pricing/ModuleMetadata.kt:7` → `@ApplicationModule(id = "pricing-core", type = Type.OPEN)` | ✔ accepted (`pricing-core`) | Completion inserts `pricing-core` correctly into a Kotlin string |
| TC-X-06 | jMolecules `@Module` on a Kotlin carrier ([S-X-06](#s-x-06)) | ✔ compile error `This annotation is not applicable to target 'class'` (R15) | No plugin exception |
| TC-X-07 | KMP/Android-style source sets (`src/jvmMain/kotlin`) | not probed | Detection of `JavaSourceRootType.SOURCE` |
| TC-X-08 | Very large module (500 generated Kotlin files) | not probed | Model build time in `idea.log` at debug level; UI stays responsive |
| TC-X-09 | Remote development (split mode) | n/a | Icons, inspections, completion on the frontend |

### 5.11 Snippets

Apply one snippet at a time unless a row says otherwise, then revert with `git checkout -- . && git clean -fd`.
New files are complete. Edits name the line in the committed state.

#### S-DECL-07
Replace `notes/NotesService.kt`:
```kotlin
package com.example.notes

import com.example.inventory.InventoryApi
import org.springframework.stereotype.Service

@Service
class NotesService(private val inventoryApi: InventoryApi) { // TC-DECL-07
    fun note(sku: String): String = "$sku: ${inventoryApi.stockOf(sku)}"
}
```

#### S-DECL-08
Copy `order/ModuleMetadata.kt` to `order/Second.kt` and rename the class to `Second` (keep both annotations):
```kotlin
class Second // TC-DECL-08
```

#### S-DECL-09
`order/spi/ModuleMetadata.kt:8` → `private class ModuleMetadata`, and add `inventory/SpiProbe.kt`:
```kotlin
package com.example.inventory

import com.example.order.spi.OrderSpi

class SpiProbe : OrderSpi { // TC-DECL-09
    override fun onOrder(orderId: String) {}
}
```

#### S-DECL-11
`inventory/CarrierProbe.kt`:
```kotlin
package com.example.inventory

class CarrierProbe(val carrier: com.example.order.ModuleMetadata) // TC-DECL-11
```

#### S-DECL-17
a) `order/CatalogEventsProbe.kt`:
```kotlin
package com.example.order

import com.example.catalog.events.CatalogChanged
import com.example.catalog.events.CatalogEventsInternal

class CatalogEventsProbe(val changed: CatalogChanged, val internal: CatalogEventsInternal) // TC-DECL-17
```
b) Remove `val internal: CatalogEventsInternal` and its import, and append `"catalog :: events"` to the list in `order/ModuleMetadata.kt:9`.

#### S-NEST-04
`order/api/ParentInternalProbe.kt`:
```kotlin
package com.example.order.api

import com.example.order.internal.OrderRepository

class ParentInternalProbe(val repository: OrderRepository) // TC-NEST-04
```

#### S-NEST-05b
`order/api/SiblingRootProbe.kt`:
```kotlin
package com.example.order.api

import com.example.order.web.OrderController

class SiblingRootProbe(val controller: OrderController) // TC-NEST-05b
```

#### S-NEST-06
In `order/OrderService.kt`, add a constructor parameter after line 14:
```kotlin
    private val orderController: com.example.order.web.OrderController, // TC-NEST-06
```

#### S-NEST-07
`order/api/CatalogProbe.kt`:
```kotlin
package com.example.order.api

import com.example.catalog.api.CatalogApi

class CatalogProbe(val catalogApi: CatalogApi) // TC-NEST-07
```

#### S-NC-NEST-01
`billing/NestedInternalProbe.kt`:
```kotlin
package com.example.billing

import com.example.order.web.internal.OrderWebInternal

class NestedInternalProbe(val webInternal: OrderWebInternal) // NC-NEST-01
```

#### S-NC-NEST-02
`inventory/NestedRootProbe.kt`:
```kotlin
package com.example.inventory

import com.example.order.web.OrderController

class NestedRootProbe(val controller: OrderController) // NC-NEST-02
```

#### S-NC-NEST-03
`order/web/SiblingInternalProbe.kt`:
```kotlin
package com.example.order.web

import com.example.order.api.internal.ApiSecret

class SiblingInternalProbe(val secret: ApiSecret) // NC-NEST-03
```

#### S-NC-NEST-04
`order/NestedInternalProbe.kt`:
```kotlin
package com.example.order

import com.example.order.web.internal.OrderWebInternal

class NestedInternalProbe(val webInternal: OrderWebInternal) // NC-NEST-04
```

#### S-DEP-01b
In `order/OrderService.kt`, add a constructor parameter after line 14:
```kotlin
    private val inventoryApi: com.example.inventory.InventoryApi, // TC-DEP-01 (B.2 placement)
```

#### S-DEP-03
In `order/OrderService.kt`, add a constructor parameter after line 11:
```kotlin
    private val catalogService: com.example.catalog.CatalogService, // TC-DEP-03
```

#### S-DEP-05
`billing/BillingService.kt:6` →
```kotlin
class BillingService(private val inventoryApi: com.example.inventory.InventoryApi) { // TC-DEP-05
```

#### S-DEP-09
`order/ModuleMetadata.kt:9` →
```kotlin
    allowedDependencies = [ModuleIds.INVENTORY, "catalog::api", " catalog :: api ", "shared :: *", "${ModuleIds.CATALOG} :: api", "inventory", "order"], // TC-DEP-09 … TC-DEP-12
```

#### S-DEP-13
`billing/ModuleMetadata.kt:6` → `@ApplicationModule(allowedDependencies = ["order.web"])`, and add `billing/NestedRootProbe.kt`:
```kotlin
package com.example.billing

import com.example.order.web.OrderController

class NestedRootProbe(val controller: OrderController) // TC-DEP-13
```

#### S-API
`inventory/ApiProbe01.kt`:
```kotlin
package com.example.inventory

import com.example.order.internal.OrderRepository // TC-API-01

class ApiProbe01
```
`inventory/ApiProbe.kt`:
```kotlin
package com.example.inventory

import com.example.order.internal.OrderCallback
import com.example.order.internal.OrderInternalObject
import com.example.order.internal.OrderKind
import com.example.order.internal.OrderRepository
import com.example.order.internal.OrderState
import com.example.order.internal.OrderTag
import com.example.order.internal.RepositoryAlias
import com.example.order.internal.internalExt
import com.example.order.internal.internalHelper
import com.example.order.internal.someProperty
import com.example.order.spi.OrderSpi

class ApiProbe02(private val repo: OrderRepository) { override fun toString() = repo.toString() } // TC-API-02
class ApiProbe03 { fun f(r: OrderRepository): OrderRepository = r } // TC-API-03
class ApiProbe04 { val list: List<OrderRepository> = emptyList() } // TC-API-04
class ApiProbe05Cast { fun g(x: Any): Any = x as OrderRepository } // TC-API-05
class ApiProbe05Is { fun g(x: Any): Boolean = x is OrderRepository } // TC-API-05
class ApiProbe05Local { fun g(): String { val r: OrderRepository? = null; return r.toString() } } // TC-API-05
class ApiProbe06 { fun t(): Class<*> = OrderRepository::class.java } // TC-API-06
class ApiProbe07Extends : OrderRepository() // TC-API-07
class ApiProbe07Implements : OrderCallback { override fun onOrder(orderId: String) {} } // TC-API-07
class ApiProbe07Spi : OrderSpi { override fun onOrder(orderId: String) {} } // TC-API-07 control
class ApiProbe08 { fun d() = OrderInternalObject.describe() } // TC-API-08
class ApiProbe09 { fun c() = OrderRepository.create() } // TC-API-09
@OrderTag class ApiProbe10 // TC-API-10
typealias ApiProbeRepo = OrderRepository // TC-API-11
class ApiProbe11 { val r: ApiProbeRepo? = null } // TC-API-11
class ApiProbe11Internal { val r: RepositoryAlias? = null } // TC-API-11 variant
class ApiProbe12 { fun h() = internalHelper() } // TC-API-12
class ApiProbe13Property { fun p() = someProperty } // TC-API-13
class ApiProbe13Extension { fun e() = "x".internalExt() } // TC-API-13
class ApiProbe14Enum { fun k() = OrderKind.BULK } // TC-API-14
class ApiProbe14Sealed { fun s(state: OrderState) = when (state) { is OrderState.Paid -> state.amount; OrderState.New -> 0L } } // TC-API-14
class ApiProbe15Lambda { val h: (OrderRepository) -> Unit = {} } // TC-API-15
class ApiProbe15Delegation(r: OrderCallback) : OrderCallback by r // TC-API-15
class ApiProbe15Spi(r: OrderSpi) : OrderSpi by r // TC-API-15 control
class ApiProbe16 { fun i(): String { val r = OrderRepository.create(); return r.statusOf("1") } } // TC-API-16
```
`src/test/kotlin/com/example/inventory/TestApiProbe.kt`:
```kotlin
package com.example.inventory

import com.example.order.internal.OrderRepository

class TestApiProbe(val repo: OrderRepository) // TC-API-17
```
`order/api/VisibilityProbe.kt`:
```kotlin
package com.example.order.api

import com.example.inventory.InventoryInternalVisible

class VisibilityProbe { // TC-API-19
    private val visible = InventoryInternalVisible()
    override fun toString() = visible.toString()
}
```
`order/api/PrivateProbe.kt` (compile error; apply alone):
```kotlin
package com.example.order.api

class PrivateProbe { // TC-API-20
    private val hidden = com.example.inventory.InventoryPrivate()
}
```
`src/main/java/com/example/inventory/JavaProbe.java`:
```java
package com.example.inventory;

import com.example.order.internal.OrderRepository;

public class JavaProbe { // TC-API-22
    private final OrderRepository repository;

    public JavaProbe(OrderRepository repository) {
        this.repository = repository;
    }
}
```

#### S-QF
`inventory/ApiProbe.kt` with one class only:
```kotlin
package com.example.inventory

import com.example.order.internal.OrderRepository

class ApiProbe(private val repo: OrderRepository) // TC-API-02
```

#### S-QF-03b
`order/web/StockProbe.kt`:
```kotlin
package com.example.order.web

import com.example.inventory.internal.StockLevel

class StockProbe(val level: StockLevel) // TC-QF-03b
```

#### S-EVT-06
`inventory/SuspendListener.kt`:
```kotlin
package com.example.inventory

import com.example.order.OrderCompleted
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class SuspendListener {
    @ApplicationModuleListener
    suspend fun on(event: OrderCompleted) { // TC-EVT-06
        println(event)
    }
}
```

#### S-EVT-07
`inventory/DeprecatedListener.kt`:
```kotlin
package com.example.inventory

import com.example.order.OrderCompleted
import org.springframework.modulith.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class DeprecatedListener {
    @ApplicationModuleListener
    fun on(event: OrderCompleted) { // TC-EVT-07
        println(event)
    }
}
```

#### S-SRC-03
`src/main/java/com/example/audit/AuditService.java`:
```java
package com.example.audit;

public class AuditService { // TC-SRC-03
}
```

#### S-SRC-04
Delete `Application.kt` and add `src/main/java/com/example/Application.java`:
```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "Kotlin Modulith", sharedModules = "shared", additionalPackages = "com.other")
@SpringBootApplication
public class Application { // TC-SRC-04

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### S-SRC-05
`src/main/kotlin/com/example/billing/package-info.java`:
```java
@ApplicationModule(displayName = "Billing from src/main/kotlin", allowedDependencies = {"inventory"}) // TC-SRC-05
package com.example.billing;

import org.springframework.modulith.ApplicationModule;
```

#### S-SRC-09
`Application2.kt`:
```kotlin
package com.example

import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Application2 // TC-SRC-09
```

#### S-SRC-13
`src/main/kotlin/com/other/billing/ModuleMetadata.kt:8` → `@ApplicationModule(displayName = "Other Billing")`

#### S-LIVE-03
`order/pricing/ModuleMetadata.kt`:
```kotlin
package com.example.order.pricing

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule // TC-LIVE-03
@PackageInfo
class ModuleMetadata
```

#### S-X-01
`inventory/internal/Outer.kt`; for the second placement use `inventory/Outer.kt` with `package com.example.inventory`:
```kotlin
package com.example.inventory.internal

import org.springframework.modulith.NamedInterface
import org.springframework.modulith.PackageInfo

class Outer { // TC-X-01
    @PackageInfo
    @NamedInterface("inner")
    class Meta
}
```

#### S-X-04
Add `KotlinModulithic.kt`, then replace the `@Modulithic(...)` block in `Application.kt` (lines 7–11) with
`@KotlinModulithic(extraPackages = ["com.other"], shared = ["shared"])` and remove the `Modulithic` import:
```kotlin
package com.example

import org.springframework.core.annotation.AliasFor
import org.springframework.modulith.Modulithic

@Modulithic
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KotlinModulithic( // TC-X-04
    @get:AliasFor(annotation = Modulithic::class, attribute = "additionalPackages")
    val extraPackages: Array<String> = [],
    @get:AliasFor(annotation = Modulithic::class, attribute = "sharedModules")
    val shared: Array<String> = [],
)
```

#### S-X-06
`build.gradle.kts`: add `implementation("org.jmolecules:jmolecules-ddd:1.10.0")`, and add `inventory/ModuleMetadata.kt`:
```kotlin
package com.example.inventory

import org.jmolecules.ddd.annotation.Module
import org.springframework.modulith.PackageInfo

@PackageInfo
@Module(name = "Inventory (jMolecules)") // TC-X-06
class ModuleMetadata
```

---

## 6. Runtime cross-check (ground truth)

```bash
./gradlew test            # gradlew.bat test on Windows
```

| Test | Model | Committed state |
|---|---|---|
| `ModulithTests` › `structure is detected` | without `com.example.reporting` | ✔ passes; prints `runtime-structure.txt` §1 |
| `ModulithTests` › `verification passes without reporting` | without `com.example.reporting` | ✔ passes: no violation, no cycle |
| `ModulithTests` › `reporting violates its allowed dependencies` | full | ✔ passes by catching both `reporting` violations (`runtime-structure.txt` §2) |
| `RuntimeProbe` › `prints model and violations` | both | ✔ always passes; prints id, display name, base package, parent, OPEN, allowed dependencies, named interfaces, and every violation or exception |

`RuntimeProbe` is the tool behind every ✔ in the Runtime columns. With a snippet applied, run it from the IDE, or run
`./gradlew test --tests com.example.RuntimeProbe` and read `build/test-results/test/TEST-com.example.RuntimeProbe.xml`.

> For QA: `verify()` green and an IDE error on a marked line ⇒ IDE false positive. `verify()` red and no IDE error
> on the marked line ⇒ IDE false negative.

---

## 7. Results matrix

**IDE build:** `________` · **Plugin:** `________` · **Modulith:** `1.4.13` · **Kotlin:** `2.2.21` · **Date:** `________` · **Tester:** `________`

Priority follows `test.md` D.3. TC-DECL-07 is raised to P1 because of R1. Pre-fix predictions in **bold** are
expected failures; if one passes on the pre-fix build, correct `test.md` A.3.

| ID | Pri | Runtime | IDE expected | Baseline prediction | Baseline observed | Fixed build observed | Status | Issue |
|---|---|---|---|---|---|---|---|---|
| TC-DECL-01 | P1 | module `order` ✔ | icon + `Order Module` + Structure | icon, no text | | | ☐ Pass ☐ Fail | |
| TC-DECL-02 | P2 | display names ✔ | location text | dir name only | | | ☐ Pass ☐ Fail | |
| TC-DECL-03 | P2 | id `catalog` ✔, survives rename ✔ | `Id: catalog` before and after rename | dir name | | | ☐ Pass ☐ Fail | |
| TC-DECL-04 | P2 | detected ✔ | as class | as class | | | ☐ Pass ☐ Fail | |
| TC-DECL-05 | P2 | ni `api` ✔ | icon + Structure | works | | | ☐ Pass ☐ Fail | |
| TC-DECL-06 | P1 | OPEN, legal ✔ | not flagged, green | **flagged** | | | ☐ Pass ☐ Fail | |
| TC-DECL-06b | P2 | OPEN, legal ✔ | not flagged, green | **flagged** | | | ☐ Pass ☐ Fail | |
| TC-DECL-07 | P1 | carrier read; snippet violation ✔ | text + snippet flagged | **ignored** | | | ☐ Pass ☐ Fail | |
| TC-DECL-08 | P2 | IllegalStateException ✔ | no exception, warning | ignores both | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-09 | P1 | ni kept, legal ✔ | ni kept, not flagged | **ni lost, flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-10 | P2 | ni kept ✔ | detected | detected | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-11 | P2 | legal ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-12 | P2 | same list ✔ | same as `[...]` | **ignored** | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-13 | P2 | compile error ✔ | Kotlin error, no exception | same | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-14 | P2 | compile error ✔ | Kotlin error, no exception | same | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-15 | P2 | two ni ✔ | both listed, completion | **no completion** | | | ☐ Pass ☐ Fail | |
| TC-DECL-16 | P2 | ni `spi` ✔ | same | same | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-16b | P2 | type-level ni only ✔ | same as runtime | same | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-17 | P2 | a) 2 violations, b) legal ✔ | a) both flagged, b) none | **a) `CatalogChanged` not flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-DECL-18 | P2 | `OrderUtils` exposed ✔ | listed | same | | | ☐ Pass ☐ Fail | temporary |
| TC-NEST-01 | P1 | `order.web` ✔ | nested icon, Structure | **internal package** | | | ☐ Pass ☐ Fail | |
| TC-NEST-02 | P2 | legal ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | |
| TC-NEST-03 | P2 | legal ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | |
| TC-NEST-04 | P2 | violation ✔ | flagged | not flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-NEST-05 | P2 | legal ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | |
| TC-NEST-05b | P2 | violation + cycle ✔ | flagged | not flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-NEST-06 | P2 | violation + cycles ✔ | flagged | not flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-NEST-07 | P1 | violation ✔ (`:9` legal) | flagged (`:9` not) | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-NEST-08 | P2 | legal ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | |
| NC-NEST-01 | P2 | violation ✔ | flagged | flagged (other text) | | | ☐ Pass ☐ Fail | temporary |
| NC-NEST-02 | P1 | invalid sub-module + cycle ✔ | flagged | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| NC-NEST-03 | P2 | violation ✔ | flagged | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| NC-NEST-04 | P2 | violation + cycles ✔ | flagged | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-01 | P2 | allowed ✔ | not flagged, listed | not flagged | | | ☐ Pass ☐ Fail | |
| TC-DEP-02 | P2 | allowed ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | |
| TC-DEP-03 | P1 | violation ✔ | flagged + fix | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-04 | P2 | allowed ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | |
| TC-DEP-05 | P1 | violation ✔ | "no allowed dependencies" | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-06 | P1 | IllegalArgumentException ✔ | "does not exist" | **nothing** | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-07 | P2 | IllegalArgumentException ✔ | "Named interface … does not exist" | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-08 | P2 | IllegalArgumentException ✔ | malformed / empty | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-08c | P2 | accepted ✔ | record | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-09 | P2 | accepted ✔ | no error | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-10 | P2 | accepted ✔ | no error | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-11 | P2 | accepted ✔ | no error | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-12 | P2 | accepted ✔ | no crash, record | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-13 | P2 | accepted; invalid sub-module ✔ | id resolves, access flagged | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-DEP-14 | P1 | n/a | item list | **no items** | | | ☐ Pass ☐ Fail | |
| TC-DEP-15 | P2 | n/a | declared entries excluded | no items | | | ☐ Pass ☐ Fail | |
| TC-DEP-16 | P2 | n/a | sibling present, grandchild absent | no items | | | ☐ Pass ☐ Fail | |
| TC-DEP-17 | P1 | n/a | Ctrl+B to package | **no reference** | | | ☐ Pass ☐ Fail | |
| TC-DEP-18 | P2 | IllegalArgumentException ✔ | "does not exist", `stock` offered | Kotlin nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-API-01 | P2 | legal (no bytecode) ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-02 | P1 | violation ✔ | flagged + 3 fixes | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-03 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-04 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-05 | P2 | `is` violation; `as`, local legal ✔ | flagged (all three) | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-06 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-07 | P2 | violation; control legal ✔ | flagged; control not | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-08 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-09 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-10 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-11 | P2 | use violation; declaration legal ✔ | flagged | declaration flagged, use: record | | | ☐ Pass ☐ Fail | temporary |
| TC-API-12 | P1 | violation (`OrderInternalsKt`) ✔ | flagged | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-API-13 | P2 | violation (`OrderInternalsKt`) ✔ | flagged | **not flagged** | | | ☐ Pass ☐ Fail | temporary |
| TC-API-14 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-15 | P2 | violation; control legal ✔ | flagged; control not | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-16 | P2 | violation ✔ | flagged on qualifier | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-17 | P2 | legal ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-18 | P2 | n/a | not flagged | not flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-19 | P2 | legal ✔ | not flagged | not flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-API-20 | P2 | compile error ✔ | Kotlin error, plugin silent | same | | | ☐ Pass ☐ Fail | temporary |
| TC-API-21 | P2 | skipped | consistent with Java | — | | | ☐ Pass ☐ Fail | optional |
| TC-API-22 | P2 | violation ✔ | flagged | flagged | | | ☐ Pass ☐ Fail | temporary |
| TC-QF-01 | P2 | legal (not probed) | moved, error gone | **no-op or exception** | | | ☐ Pass ☐ Fail | temporary |
| TC-QF-02 | P2 | legal ✔ | annotation added | **no-op or exception** | | | ☐ Pass ☐ Fail | temporary |
| TC-QF-03 | P1 | Kotlin OPEN legal; Java files fail ✔ | `type = OPEN` in Kotlin | **`package-info.java` in kotlin dir** | | | ☐ Pass ☐ Fail | temporary |
| TC-QF-03b | P2 | legal ✔ | new Kotlin carrier | **same as QF-03** | | | ☐ Pass ☐ Fail | temporary |
| TC-QF-04 | P1 | legal ✔ | entry appended | **absent or no-op** | | | ☐ Pass ☐ Fail | temporary |
| TC-QF-05 | P1 | result compiles, context starts ✔ | warning + fix | **no warning** | | | ☐ Pass ☐ Fail | |
| TC-QF-06 | P2 | n/a | preview without exception | same | | | ☐ Pass ☐ Fail | |
| TC-QF-07 | P2 | n/a | one undo step | same | | | ☐ Pass ☐ Fail | |
| TC-VIEW-01 | P2 | §3.3 | icon list | **nested and OPEN red** | | | ☐ Pass ☐ Fail | |
| TC-VIEW-02 | P2 | §3.3 | six display names | **none** | | | ☐ Pass ☐ Fail | |
| TC-VIEW-03 | P2 | n/a | stable in both modes | same | | | ☐ Pass ☐ Fail | |
| TC-VIEW-04 | P2 | n/a | same icon and text | record | | | ☐ Pass ☐ Fail | |
| TC-VIEW-05 | P2 | §3.3 | full Structure | **partial** | | | ☐ Pass ☐ Fail | |
| TC-VIEW-06 | P2 | n/a | as VIEW-05 | same | | | ☐ Pass ☐ Fail | temporary |
| TC-VIEW-07 | P2 | n/a | restricted indicator | no indicator | | | ☐ Pass ☐ Fail | temporary |
| TC-VIEW-08 | P2 | `runtime-structure.txt` | identical | **mismatch** | | | ☐ Pass ☐ Fail | |
| TC-EVT-01 | P3 | n/a | not unused | record | | | ☐ Pass ☐ Fail | |
| TC-EVT-02 | P3 | legal ✔ | not flagged | same | | | ☐ Pass ☐ Fail | |
| TC-EVT-03 | P3 | not probed | flagged | same | | | ☐ Pass ☐ Fail | temporary |
| TC-EVT-04 | P3 | n/a | weak warning | **none** | | | ☐ Pass ☐ Fail | |
| TC-EVT-05 | P3 | compile errors ✔ | warning gone, red imports | same | | | ☐ Pass ☐ Fail | temporary |
| TC-EVT-06 | P3 | compiles, starts ✔ | no exception, record | same | | | ☐ Pass ☐ Fail | temporary |
| TC-EVT-07 | P3 | deprecation warning ✔ | no implicit usage, record | same | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-01 | P1 | 2 violations ✔ | both flagged, `Reporting` | **`:10` not flagged** | | | ☐ Pass ☐ Fail | |
| TC-SRC-02 | P3 | test green ✔ | n/a | n/a | | | ☐ Pass ☐ Fail | |
| TC-SRC-03 | P3 | `audit` ✔ | detected | **missing** | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-04 | P1 | all detected ✔ | all detected | **Kotlin dirs missing** | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-05 | P3 | ignored ✔ | ignore or warn | accepted silently | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-06 | P3 | not probed | detected | detected | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-07 | P3 | not probed | as Gradle | same | | | ☐ Pass ☐ Fail | variant project |
| TC-SRC-08 | P3 | not probed | record | missing | | | ☐ Pass ☐ Fail | variant project |
| TC-SRC-09 | P3 | no effect ✔ | empty model, no exception | same | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-10 | P1 | 11 modules, IllegalArgumentException ✔ | same set, unknown `"inventory"` | **zero modules** | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-11 | P3 | yml honored, profile not ✔ | same | record | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-12 | P3 | `other-billing` ✔ | icon + `Other Billing` | record | | | ☐ Pass ☐ Fail | |
| TC-SRC-13 | P3 | IllegalStateException ✔ | no exception | same | | | ☐ Pass ☐ Fail | temporary |
| TC-SRC-14 | P3 | n/a | five starters | same | | | ☐ Pass ☐ Fail | wizard |
| TC-LIVE-01 | P3 | n/a | text updates | no text | | | ☐ Pass ☐ Fail | |
| TC-LIVE-02 | P3 | legal ✔ | error disappears | nothing | | | ☐ Pass ☐ Fail | temporary |
| TC-LIVE-03 | P1 | n/a | becomes nested module | **stays red** | | | ☐ Pass ☐ Fail | temporary |
| TC-LIVE-04 | P3 | n/a | reverts to internal | stays red | | | ☐ Pass ☐ Fail | temporary |
| TC-LIVE-05 | P3 | n/a | icon and error toggle | stays red | | | ☐ Pass ☐ Fail | temporary |
| TC-LIVE-06 | P3 | as SRC-10 ✔ | set changes | zero, then all | | | ☐ Pass ☐ Fail | temporary |
| TC-LIVE-07 | P3 | n/a | previous state | same | | | ☐ Pass ☐ Fail | |
| TC-LIVE-08 | P3 | n/a | no exception | same | | | ☐ Pass ☐ Fail | |
| TC-X-01 | P3 | both placements ✔ | record | — | | | ☐ Pass ☐ Fail | temporary |
| TC-X-02 | P3 | detected ✔ | detected | — | | | ☐ Pass ☐ Fail | temporary |
| TC-X-03 | P3 | no module ✔ | no crash | — | | | ☐ Pass ☐ Fail | temporary |
| TC-X-04 | P3 | honored ✔ | record | — | | | ☐ Pass ☐ Fail | temporary |
| TC-X-05 | P3 | accepted ✔ | inserted correctly | — | | | ☐ Pass ☐ Fail | temporary |
| TC-X-06 | P3 | compile error ✔ | no exception | — | | | ☐ Pass ☐ Fail | temporary |
| TC-X-07 | P3 | not probed | record | — | | | ☐ Pass ☐ Fail | variant project |
| TC-X-08 | P3 | not probed | record | — | | | ☐ Pass ☐ Fail | generated |
| TC-X-09 | P3 | n/a | record | — | | | ☐ Pass ☐ Fail | split mode |
| `./gradlew test` | — | green ✔ (4 tests) | | | | | ☐ Pass ☐ Fail | |

**Exit criteria** (`test.md` D.2): every row whose *IDE expected* equals the runtime passes on the fixed build; every
`NC-*` row shows the error; no exception from `com.intellij.spring.modulith` in `idea.log`; every *record* row has an
observation; every IDE/runtime mismatch has an issue linked to IDEA-373094.
