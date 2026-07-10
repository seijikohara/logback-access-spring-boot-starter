# Examples E2E Test Suite Optimization — Design

- Date: 2026-07-10
- Status: Approved
- Scope: `examples/` modules only (`common`, `tomcat-mvc`, `jetty-mvc`, `tomcat-webflux`, `jetty-webflux`). No production-source changes.

## Motivation

Issue #220 reported a total access-log loss that no example test detected. The root cause (an empty, non-null `include-url-patterns` list dropping every event) was fixed on `main` by PR #208 with a core unit test only. The end-to-end (E2E) suite never exercises the Spring relaxed-binding path that produces the empty list, so the regression class remains unguarded.

An inventory of the suite also found structural debt:

- `UrlFilteringTest` (166 lines) and `SpringProfileTest` (120-126 lines) are copy-pasted across all 4 modules (~1,150 duplicated lines).
- `tomcat-mvc` has 3 tests (path variable, request headers, admin user) that assert starter behavior, not Tomcat behavior, yet no other module runs them.
- Recent behavior fixes #209 (TeeFilter body-capture policy) and #212 (form-content query subtraction) have unit coverage only.
- The `X-Forwarded-*` / `RemoteIpValve` request-attribute path of `TomcatRequestAttributeResolver` has no E2E coverage at all.
- Minor debris: `jetty-mvc/SpringProfileTest` inlines a `config-location` that the shared `application-dev/prod.yml` already provides; `tomcat-mvc` test yml activates a `test` profile that has no matching properties file; `/api/stream` exists in the reactive controller but no test requests it.

## Goals

1. Guard the issue #220 regression class end-to-end in all 4 modules.
2. Close the E2E gaps for #209 and #212 behavior.
3. Cover the `RemoteIpValve` request-attribute path end-to-end.
4. Remove copy-paste duplication by moving test bodies into shared abstract case classes, following the existing `AbstractMalformedRequestAccessLogTest` pattern.
5. Keep the per-module Spring-context count (and CI time) roughly neutral.

## Non-goals

- No changes to starter or core production code.
- No new example modules and no changes to the server/framework matrix.
- No Jetty TeeFilter enablement (Jetty 12 `RequestLog` cannot see Servlet request attributes; the `@Disabled` marker stays).

## Consolidation mechanism (approved: approach A)

Shared abstract case classes live in `examples/common` and hold the `@Test` methods plus abstract accessors (`getLogbackAccessContext()`, `getPort()`). Each module keeps its existing outer test-class name; its `@Nested` inner classes become one-line concrete subclasses of the shared cases and carry their own `@SpringBootTest(properties = ...)` annotation. Context-defining properties therefore stay visible in the module, and the JUnit shape (concrete outer class, annotated nested classes) is unchanged from what runs today.

Rejected alternatives: full `@Nested` inheritance from an abstract outer class (unverified JUnit/Spring interaction risk); one flat abstract class per context (doubles the number of thin subclass files).

## Changes by area

### 1. URL filtering — consolidate and add the #220 case

New shared cases in `examples/common` (package `examples.test.common`):

- `AbstractUrlFilteringCombinedTest` — context: `include-url-patterns=/api/.*`, `exclude-url-patterns=/api/health`. Tests: `/api/hello` is logged (include match, no exclude), `/api/health` is dropped (exclude match), `/nonexistent` (404) is dropped (include miss). This one context covers all four match/miss branch outcomes of `matchesIncludePatterns` × `matchesExcludePatterns`.
- `AbstractUrlFilteringEmptyPatternsTest` — context: `include-url-patterns=` and `exclude-url-patterns=` (empty values). Test: `/api/hello` is logged. This is the issue #220 / PR #208 regression guard: relaxed binding turns the empty value into an empty, non-null list, which v2.0.1 treated as "match nothing".

Each module's `UrlFilteringTest` shrinks to an outer namespace class with two annotated `@Nested` subclasses. The include-only and exclude-only contexts are deleted; their branch coverage is subsumed by the combined context plus the no-filter default asserted by the basic tests. Net contexts per module: 3 → 2.

### 2. Spring profiles — consolidate and normalize

New shared cases `AbstractSpringProfileDevTest` / `AbstractSpringProfileProdTest` (bodies of today's nested classes: active profile activates its appender and 1 event reaches both `commonList` and the profile list; the other profile's appender stays null). Module `SpringProfileTest` keeps dev/prod `@Nested` subclasses with `@ActiveProfiles` only. `jetty-mvc` drops its redundant inline `config-location` property; the shared `application-dev/prod.yml` supplies it, as the other 3 modules already prove. Verify by running the jetty-mvc tests before and after.

### 3. Promote tomcat-mvc-only tests into shared bases

- `getRequestWithPathVariableEmitsAccessEvent` and `eventCapturesRequestHeaders` move into `AbstractBasicAccessLogTest`; `jetty-mvc` gains them. A request-headers test is added to `AbstractReactiveAccessLogTest` for parity (the reactive base already has a path-variable test).
- `adminUserIsLoggedInAccessEvent` moves into `AbstractSecurityTest`; `jetty-mvc` gains it.
- The `tomcat-mvc` subclass bodies become empty like their jetty counterparts.

### 4. TeeFilter × form-urlencoded (tomcat-mvc; E2E for #209 and #212)

- In the existing TeeFilter context (`teefilter` profile), add to `AbstractTeeFilterTest`: a form POST whose body must NOT be captured under the default policy (`requestContent` carries the suppression sentinel, and the posted secret value never appears). Jetty's `@Disabled` subclass inherits it inertly.
- New standalone `TeeFilterFormCaptureTest` in `tomcat-mvc` only — context: `teefilter` profile plus `logback.access.tee-filter.allowed-content-types=application/x-www-form-urlencoded`. Tests: the form body IS captured, and a parameter submitted in the query string does not leak into `requestContent` (#212 subtraction).
- `HttpClientTestUtils` gains `postForm(host, port, path, formBody)` sending `Content-Type: application/x-www-form-urlencoded`.
- Risk: the echo endpoint consumes the body via `@RequestBody`, which may bypass parameter parsing for form posts. Mitigation: target an endpoint without `@RequestBody` (or add a minimal form endpoint to `MvcExampleController`) so `getParameterMap()` sees the body parameters; decide during implementation by observing the failing/passing behavior.

### 5. Forwarded headers / RemoteIpValve (Tomcat modules)

New shared case `AbstractForwardedHeaderTest` — context properties: `server.tomcat.remoteip.remote-ip-header=X-Forwarded-For` (activates `RemoteIpValve`, which `TomcatValve.initInternal` auto-detects to enable request attributes). Test: a GET with `X-Forwarded-For: 203.0.113.7` produces an event whose `remoteAddr` is `203.0.113.7`, not the loopback address. Concrete subclasses in `tomcat-mvc` and `tomcat-webflux` only; the feature is Tomcat-specific (`logback.access.tomcat.request-attributes-enabled`), so Jetty modules get no counterpart.

### 6. Small additions inside existing contexts (no new contexts)

- `AbstractReactiveAccessLogTest`: add a `/api/stream` (Flux) test asserting an event with status 200; extend the existing `delayed` test to assert `elapsedTime > 0` (the endpoint sleeps 100 ms, so this cannot flake).

### 7. Deletions and cleanups

- Remove the inert `spring.profiles.active: test` from `tomcat-mvc/src/test/resources/application.yml`.
- Delete the duplicated standalone bodies of `UrlFilteringTest` and `SpringProfileTest` in all 4 modules (replaced per areas 1-2).
- Delete the 3 promoted tests from the `tomcat-mvc` subclasses (moved per area 3).

## Expected footprint

- Spring-context delta per CI run: -8 (drop include-only and exclude-only contexts in 4 modules) +4 (empty patterns) +1 (form capture) +2 (forwarded) = -1 net; CI time stays neutral or improves slightly.
- Net source delta: ≈ -900 duplicated lines; every remaining per-module file is a thin subclass or an annotated namespace.

## Verification

1. `./gradlew clean build` must pass (Spotless, Detekt, all module tests, apiCheck).
2. The empty-patterns case is a proven regression guard: the same scenario was reproduced failing against published v2.0.1 and passing against `main` (2026-07-10 investigation of issue #220).
3. Jetty-mvc profile normalization (area 2) is validated by its own dev/prod tests passing without the inline property.
4. Report any test that needs behavior-contradicting adjustments instead of adjusting silently.
