# Examples E2E Test Suite Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the issue #220 E2E coverage gap, add E2E coverage for the #209/#212 behavior fixes and the RemoteIpValve attribute path, and de-duplicate the examples test suite per the approved spec (`docs/plans/2026-07-10-examples-e2e-test-optimization.md`).

**Architecture:** Shared abstract "case" classes in `examples/common` hold `@Test` methods and abstract accessors; each example module keeps its existing outer test-class names whose `@Nested` (or top-level) concrete subclasses carry the module's `@SpringBootTest` annotation. This is the pattern `AbstractMalformedRequestAccessLogTest` already uses.

**Tech Stack:** Java 21, JUnit 5, AssertJ, Spring Boot 4.1 `@SpringBootTest(webEnvironment = RANDOM_PORT)`, Gradle.

## Global Constraints

- Work in the worktree on branch `worktree-test-examples-e2e-optimization`; run all commands from the worktree root.
- Examples are Java sources compiled with Error Prone + NullAway (`annotatedPackages = examples`, jspecify mode). Follow the existing test classes' style exactly (4-space indent, `final var` locals, one blank line between arrange/act/assert blocks).
- Integration tests use JUnit 5 + AssertJ only — no Kotest, no MockK (those are for the library modules).
- Every `@BeforeEach` method in a shared abstract class needs a unique name (e.g. `setUpUrlFilteringAppender`) so subclass hierarchies never collide; this matches the existing bases.
- Commit messages follow Conventional Commits. Do NOT add any Claude attribution or `Claude-Session` trailers.
- All new code, comments, and Javadoc are in English.
- The `list` appender comes from `examples/common/src/main/resources/logback-access-test.xml` and is fetched via `AccessEventTestUtils.getListAppender(context, "list")`.
- Existing signatures used throughout (from `examples/common`):
  - `AccessEventTestUtils.reset(ListAppender<IAccessEvent>)`, `awaitEvents(appender)` → `List<IAccessEvent>`, `awaitEvents(appender, int count)`, `awaitNoEvents(appender)`
  - `HttpClientTestUtils.get(String url)` / `post(String url, String jsonBody)` / `getWithBasicAuth(String url, String user, String pass)` → `HttpResponse<String>`

---

### Task 1: Consolidate URL filtering tests and add the empty-patterns guard (#220)

**Files:**
- Create: `examples/common/src/main/java/examples/test/common/AbstractUrlFilteringCombinedTest.java`
- Create: `examples/common/src/main/java/examples/test/common/AbstractUrlFilteringEmptyPatternsTest.java`
- Rewrite: `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/UrlFilteringTest.java`
- Rewrite: `examples/jetty-mvc/src/test/java/examples/jettymvc/UrlFilteringTest.java`
- Rewrite: `examples/tomcat-webflux/src/test/java/examples/tomcatwebflux/UrlFilteringTest.java`
- Rewrite: `examples/jetty-webflux/src/test/java/examples/jettywebflux/UrlFilteringTest.java`

**Interfaces:**
- Produces: `AbstractUrlFilteringCombinedTest` and `AbstractUrlFilteringEmptyPatternsTest`, each with abstract `protected LogbackAccessContext getLogbackAccessContext()` and `protected String getBaseUrl()`. No other task depends on them.

- [ ] **Step 1: Create `AbstractUrlFilteringCombinedTest.java`**

```java
package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for combined URL include/exclude filtering tests.
 * <p>
 * One context configured with {@code include-url-patterns=/api/.*} and
 * {@code exclude-url-patterns=/api/health} covers all four branch outcomes of the
 * include/exclude matching: include hit, include miss, exclude hit, and exclude miss.
 * The no-filter default (both properties unset) is covered by the basic access log tests.
 * <p>
 * Subclasses bind the filter properties via {@code @SpringBootTest} and implement the
 * abstract accessors.
 */
public abstract class AbstractUrlFilteringCombinedTest {

    private ListAppender<IAccessEvent> listAppender;

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    /**
     * Returns the base URL for the test server.
     *
     * @return the base URL (e.g., "http://localhost:8080")
     */
    protected abstract String getBaseUrl();

    @BeforeEach
    void setUpUrlFilteringAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
    }

    @Test
    void includedButNotExcludedUrlEmitsEvent() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRequestURI()).isEqualTo("/api/hello");
    }

    @Test
    void excludedUrlDoesNotEmitEvent() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/health");

        AccessEventTestUtils.awaitNoEvents(listAppender);
    }

    @Test
    void urlOutsideIncludePatternsDoesNotEmitEvent() throws Exception {
        // A 404 response emits an access event by default (see the basic tests), so an
        // absent event here proves the include-pattern miss suppressed it.
        HttpClientTestUtils.get(getBaseUrl() + "/nonexistent");

        AccessEventTestUtils.awaitNoEvents(listAppender);
    }
}
```

- [ ] **Step 2: Create `AbstractUrlFilteringEmptyPatternsTest.java`**

```java
package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for the empty URL pattern regression guard (issue #220, PR #208).
 * <p>
 * Spring's relaxed binding turns an empty property value (for example
 * {@code logback.access.filter.include-url-patterns=}, or an unset placeholder such as
 * {@code ${PATTERNS:}}) into an empty, non-null list. Starter versions up to 2.0.1
 * treated the empty include list as "match nothing" and silently dropped every access
 * event. This guards that all requests are still logged when both pattern lists bind
 * to empty lists.
 */
public abstract class AbstractUrlFilteringEmptyPatternsTest {

    private ListAppender<IAccessEvent> listAppender;

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    /**
     * Returns the base URL for the test server.
     *
     * @return the base URL (e.g., "http://localhost:8080")
     */
    protected abstract String getBaseUrl();

    @BeforeEach
    void setUpEmptyPatternsAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
    }

    @Test
    void emptyPatternListsLogAllRequests() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRequestURI()).isEqualTo("/api/hello");
    }
}
```

- [ ] **Step 3: Rewrite the four module `UrlFilteringTest` classes**

Full content for `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/UrlFilteringTest.java`:

```java
package examples.tomcatmvc;

import examples.test.common.AbstractUrlFilteringCombinedTest;
import examples.test.common.AbstractUrlFilteringEmptyPatternsTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests for URL pattern filtering configuration.
 */
class UrlFilteringTest {

    /**
     * Combined include and exclude patterns in one context.
     */
    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = {
                    "logback.access.filter.include-url-patterns=/api/.*",
                    "logback.access.filter.exclude-url-patterns=/api/health"
            }
    )
    class CombinedPatternTest extends AbstractUrlFilteringCombinedTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        @Override
        protected LogbackAccessContext getLogbackAccessContext() {
            return logbackAccessContext;
        }

        @Override
        protected String getBaseUrl() {
            return "http://localhost:" + port;
        }
    }

    /**
     * Empty pattern values bind to empty lists; every request must still be logged
     * (issue #220 regression guard).
     */
    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = {
                    "logback.access.filter.include-url-patterns=",
                    "logback.access.filter.exclude-url-patterns="
            }
    )
    class EmptyPatternsTest extends AbstractUrlFilteringEmptyPatternsTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        @Override
        protected LogbackAccessContext getLogbackAccessContext() {
            return logbackAccessContext;
        }

        @Override
        protected String getBaseUrl() {
            return "http://localhost:" + port;
        }
    }
}
```

The other three module files are identical to the above except for two lines:

| Module file | Package line | Class Javadoc line |
|---|---|---|
| `examples/jetty-mvc/.../jettymvc/UrlFilteringTest.java` | `package examples.jettymvc;` | `* Tests for URL pattern filtering configuration on Jetty.` |
| `examples/tomcat-webflux/.../tomcatwebflux/UrlFilteringTest.java` | `package examples.tomcatwebflux;` | `* Tests for URL pattern filtering configuration on reactive Tomcat.` |
| `examples/jetty-webflux/.../jettywebflux/UrlFilteringTest.java` | `package examples.jettywebflux;` | `* Tests for URL pattern filtering configuration on reactive Jetty.` |

- [ ] **Step 4: Run the URL filtering tests in all four modules**

Run:

```bash
./gradlew :examples:tomcat-mvc:test :examples:jetty-mvc:test :examples:tomcat-webflux:test :examples:jetty-webflux:test --tests '*UrlFilteringTest*'
```

Expected: `BUILD SUCCESSFUL`; each module runs 4 tests (3 combined + 1 empty-patterns), 16 total, 0 failures.

- [ ] **Step 5: Commit**

```bash
git add examples/common/src/main/java/examples/test/common/AbstractUrlFilteringCombinedTest.java \
        examples/common/src/main/java/examples/test/common/AbstractUrlFilteringEmptyPatternsTest.java \
        examples/tomcat-mvc/src/test/java/examples/tomcatmvc/UrlFilteringTest.java \
        examples/jetty-mvc/src/test/java/examples/jettymvc/UrlFilteringTest.java \
        examples/tomcat-webflux/src/test/java/examples/tomcatwebflux/UrlFilteringTest.java \
        examples/jetty-webflux/src/test/java/examples/jettywebflux/UrlFilteringTest.java
git commit -m "test(examples): consolidate URL filtering tests and guard empty patterns

Move the duplicated per-module UrlFilteringTest bodies into shared
abstract cases. Merge the include-only and exclude-only contexts into
the combined context, which covers the same match/miss branches with
one fewer Spring context per module.

Add an empty-pattern context: relaxed binding turns an empty property
value into an empty non-null list, which starter 2.0.1 treated as
'match nothing' and silently dropped every event (issue #220, fixed by
PR #208). No E2E test covered that binding path before."
```

---

### Task 2: Share Spring profile test cases and normalize jetty-mvc

**Files:**
- Create: `examples/common/src/main/java/examples/test/common/AbstractSpringProfileDevTest.java`
- Create: `examples/common/src/main/java/examples/test/common/AbstractSpringProfileProdTest.java`
- Rewrite: `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/SpringProfileTest.java`
- Rewrite: `examples/jetty-mvc/src/test/java/examples/jettymvc/SpringProfileTest.java`
- Rewrite: `examples/tomcat-webflux/src/test/java/examples/tomcatwebflux/SpringProfileTest.java`
- Rewrite: `examples/jetty-webflux/src/test/java/examples/jettywebflux/SpringProfileTest.java`

**Interfaces:**
- Consumes: `application-dev.yml` / `application-prod.yml` in `examples/common/src/main/resources` set `logback.access.config-location=classpath:logback-access-spring-profile.xml`; that config defines appenders `commonList`, `devList` (dev profile), `prodList` (prod profile).
- Produces: `AbstractSpringProfileDevTest` / `AbstractSpringProfileProdTest` with abstract `getLogbackAccessContext()` and `getBaseUrl()`. No other task depends on them.

- [ ] **Step 1: Create `AbstractSpringProfileDevTest.java`**

```java
package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@code <springProfile name="dev">} activation tests.
 * <p>
 * The shared {@code application-dev.yml} points {@code logback.access.config-location}
 * at {@code logback-access-spring-profile.xml}, which defines {@code commonList}
 * unconditionally, {@code devList} inside the dev profile block, and {@code prodList}
 * inside the prod profile block. Subclasses activate the dev profile via
 * {@code @ActiveProfiles("dev")}.
 */
public abstract class AbstractSpringProfileDevTest {

    private ListAppender<IAccessEvent> commonListAppender;
    private ListAppender<IAccessEvent> devListAppender;

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    /**
     * Returns the base URL for the test server.
     *
     * @return the base URL (e.g., "http://localhost:8080")
     */
    protected abstract String getBaseUrl();

    @BeforeEach
    void setUpDevProfileAppenders() {
        commonListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "commonList");
        devListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "devList");
        AccessEventTestUtils.reset(commonListAppender);
        AccessEventTestUtils.reset(devListAppender);
    }

    @Test
    void devProfileActivatesDevAppender() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var commonEvents = AccessEventTestUtils.awaitEvents(commonListAppender);
        final var devEvents = AccessEventTestUtils.awaitEvents(devListAppender);

        assertThat(commonEvents).hasSize(1);
        assertThat(devEvents).hasSize(1);
    }

    @Test
    void devProfileDoesNotActivateProdAppender() {
        final var prodAppender = getLogbackAccessContext().getAccessContext().getAppender("prodList");

        assertThat(prodAppender).isNull();
    }
}
```

- [ ] **Step 2: Create `AbstractSpringProfileProdTest.java`**

Same as Step 1 with dev/prod mirrored. Full content:

```java
package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@code <springProfile name="prod">} activation tests.
 * <p>
 * Mirror of {@link AbstractSpringProfileDevTest} for the prod profile: {@code prodList}
 * must be active and {@code devList} absent. Subclasses activate the prod profile via
 * {@code @ActiveProfiles("prod")}.
 */
public abstract class AbstractSpringProfileProdTest {

    private ListAppender<IAccessEvent> commonListAppender;
    private ListAppender<IAccessEvent> prodListAppender;

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    /**
     * Returns the base URL for the test server.
     *
     * @return the base URL (e.g., "http://localhost:8080")
     */
    protected abstract String getBaseUrl();

    @BeforeEach
    void setUpProdProfileAppenders() {
        commonListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "commonList");
        prodListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "prodList");
        AccessEventTestUtils.reset(commonListAppender);
        AccessEventTestUtils.reset(prodListAppender);
    }

    @Test
    void prodProfileActivatesProdAppender() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var commonEvents = AccessEventTestUtils.awaitEvents(commonListAppender);
        final var prodEvents = AccessEventTestUtils.awaitEvents(prodListAppender);

        assertThat(commonEvents).hasSize(1);
        assertThat(prodEvents).hasSize(1);
    }

    @Test
    void prodProfileDoesNotActivateDevAppender() {
        final var devAppender = getLogbackAccessContext().getAccessContext().getAppender("devList");

        assertThat(devAppender).isNull();
    }
}
```

- [ ] **Step 3: Rewrite the four module `SpringProfileTest` classes**

Full content for `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/SpringProfileTest.java`. Note there is NO inline `config-location` property in any module — the shared `application-dev.yml` / `application-prod.yml` supply it. This intentionally removes the redundant inline property that only `jetty-mvc` had.

```java
package examples.tomcatmvc;

import examples.test.common.AbstractSpringProfileDevTest;
import examples.test.common.AbstractSpringProfileProdTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for Spring Profile integration with logback-access configuration.
 * Uses logback-access-spring-profile.xml which defines profile-specific appenders.
 */
class SpringProfileTest {

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dev")
    class DevProfileTest extends AbstractSpringProfileDevTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        @Override
        protected LogbackAccessContext getLogbackAccessContext() {
            return logbackAccessContext;
        }

        @Override
        protected String getBaseUrl() {
            return "http://localhost:" + port;
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("prod")
    class ProdProfileTest extends AbstractSpringProfileProdTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        @Override
        protected LogbackAccessContext getLogbackAccessContext() {
            return logbackAccessContext;
        }

        @Override
        protected String getBaseUrl() {
            return "http://localhost:" + port;
        }
    }
}
```

The other three module files are identical except for two lines:

| Module file | Package line | Class Javadoc first line |
|---|---|---|
| `examples/jetty-mvc/.../jettymvc/SpringProfileTest.java` | `package examples.jettymvc;` | `* Tests for Spring Profile integration with logback-access configuration on Jetty.` |
| `examples/tomcat-webflux/.../tomcatwebflux/SpringProfileTest.java` | `package examples.tomcatwebflux;` | `* Tests for Spring Profile integration with logback-access configuration on reactive Tomcat.` |
| `examples/jetty-webflux/.../jettywebflux/SpringProfileTest.java` | `package examples.jettywebflux;` | `* Tests for Spring Profile integration with logback-access configuration on reactive Jetty.` |

- [ ] **Step 4: Run the Spring profile tests in all four modules**

Run:

```bash
./gradlew :examples:tomcat-mvc:test :examples:jetty-mvc:test :examples:tomcat-webflux:test :examples:jetty-webflux:test --tests '*SpringProfileTest*'
```

Expected: `BUILD SUCCESSFUL`; each module runs 4 tests, 16 total, 0 failures. jetty-mvc passing without the inline `config-location` property proves the shared profile YAMLs reach its classpath.

- [ ] **Step 5: Commit**

```bash
git add examples/common/src/main/java/examples/test/common/AbstractSpringProfileDevTest.java \
        examples/common/src/main/java/examples/test/common/AbstractSpringProfileProdTest.java \
        examples/tomcat-mvc/src/test/java/examples/tomcatmvc/SpringProfileTest.java \
        examples/jetty-mvc/src/test/java/examples/jettymvc/SpringProfileTest.java \
        examples/tomcat-webflux/src/test/java/examples/tomcatwebflux/SpringProfileTest.java \
        examples/jetty-webflux/src/test/java/examples/jettywebflux/SpringProfileTest.java
git commit -m "test(examples): share Spring profile test cases across modules

Move the duplicated per-module SpringProfileTest bodies into shared
abstract dev/prod cases. Drop jetty-mvc's inline config-location
property: the shared application-dev/prod.yml already supplies it, as
the other three modules prove."
```

---

### Task 3: Promote tomcat-mvc-only assertions into the shared bases

**Files:**
- Modify: `examples/common/src/main/java/examples/AbstractBasicAccessLogTest.java` (append 2 tests before the closing brace)
- Modify: `examples/common/src/main/java/examples/AbstractSecurityTest.java` (append 1 test before the closing brace)
- Modify: `examples/common/src/main/java/examples/test/webflux/AbstractReactiveAccessLogTest.java` (append 1 test before the closing brace)
- Rewrite: `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/BasicAccessLogTest.java` (drop the promoted tests)
- Rewrite: `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/SecurityIntegrationTest.java` (drop the promoted test)

**Interfaces:**
- Consumes: `MvcSecurityConfig` (examples/common) defines in-memory users `user/password` and `admin/admin`; `MvcExampleController` serves `/api/items/{id}`; `ReactiveExampleController` serves `/api/hello`.
- Produces: nothing another task depends on.

- [ ] **Step 1: Append two tests to `AbstractBasicAccessLogTest.java`**

Insert before the final closing brace of the class (after `notFoundResponseProducesEventWith404Status`):

```java
    @Test
    void getRequestWithPathVariableEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/items/42");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRequestURI()).isEqualTo("/api/items/42");
    }

    @Test
    void eventCapturesRequestHeaders() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        // Compare header names case-insensitively: servers differ in the casing they
        // report for header names.
        assertThat(events.get(0).getRequestHeaderMap().keySet())
                .anySatisfy(name -> assertThat(name).isEqualToIgnoringCase("Host"));
    }
```

- [ ] **Step 2: Append one test to `AbstractSecurityTest.java`**

Insert before the final closing brace of the class (after `invalidCredentialsReturns401`):

```java
    @Test
    void adminUserIsLoggedInAccessEvent() throws Exception {
        // Credentials come from the shared MvcSecurityConfig in-memory user store.
        final var response = HttpClientTestUtils.getWithBasicAuth(
                getBaseUrl() + "/api/secure",
                "admin",
                "admin");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRemoteUser()).isEqualTo("admin");
    }
```

- [ ] **Step 3: Append one test to `AbstractReactiveAccessLogTest.java`**

Insert before the final closing brace of the class (after `remoteUserRendersAsNotAvailableOnReactiveStacks`):

```java
    @Test
    void reactiveEventCapturesRequestHeaders() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        // Compare header names case-insensitively: servers differ in the casing they
        // report for header names.
        assertThat(events.get(0).getRequestHeaderMap().keySet())
                .anySatisfy(name -> assertThat(name).isEqualToIgnoringCase("Host"));
    }
```

- [ ] **Step 4: Slim the two tomcat-mvc subclasses**

Full content for `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/BasicAccessLogTest.java`:

```java
package examples.tomcatmvc;

import examples.AbstractBasicAccessLogTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Basic access log tests for Tomcat + MVC.
 * Extends AbstractBasicAccessLogTest for common HTTP method tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BasicAccessLogTest extends AbstractBasicAccessLogTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @LocalServerPort
    int port;

    @Override
    protected LogbackAccessContext getLogbackAccessContext() {
        return logbackAccessContext;
    }

    @Override
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
```

Full content for `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/SecurityIntegrationTest.java`:

```java
package examples.tomcatmvc;

import examples.AbstractSecurityTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests for Spring Security integration with access logging on Tomcat.
 * Extends AbstractSecurityTest for common security tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest extends AbstractSecurityTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @LocalServerPort
    int port;

    @Override
    protected LogbackAccessContext getLogbackAccessContext() {
        return logbackAccessContext;
    }

    @Override
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Override
    protected String getAuthenticatedUsername() {
        return "user";
    }

    @Override
    protected String getAuthenticatedPassword() {
        return "password";
    }
}
```

- [ ] **Step 5: Run the affected tests**

Run:

```bash
./gradlew :examples:tomcat-mvc:test :examples:jetty-mvc:test --tests '*BasicAccessLogTest*' --tests '*SecurityIntegrationTest*'
./gradlew :examples:tomcat-webflux:test :examples:jetty-webflux:test --tests '*ReactiveAccessLogTest*'
```

Expected: `BUILD SUCCESSFUL` for both. Per MVC module: 9 basic tests (7 + 2 promoted) and 5 security tests (4 + 1 promoted). Per WebFlux module: 11 reactive tests (10 + 1 new). jetty-mvc now runs the promoted tests for the first time — if a jetty run fails on a promoted assertion, report the output instead of weakening the assertion.

- [ ] **Step 6: Commit**

```bash
git add examples/common/src/main/java/examples/AbstractBasicAccessLogTest.java \
        examples/common/src/main/java/examples/AbstractSecurityTest.java \
        examples/common/src/main/java/examples/test/webflux/AbstractReactiveAccessLogTest.java \
        examples/tomcat-mvc/src/test/java/examples/tomcatmvc/BasicAccessLogTest.java \
        examples/tomcat-mvc/src/test/java/examples/tomcatmvc/SecurityIntegrationTest.java
git commit -m "test(examples): promote tomcat-only assertions into shared bases

The path-variable, request-header, and admin-user tests asserted
starter behavior, not Tomcat behavior, yet only tomcat-mvc ran them.
Move them into the shared bases so jetty-mvc gains them, and add the
request-header assertion to the reactive base for parity. Header names
are compared case-insensitively because servers differ in reported
header-name casing."
```

---

### Task 4: Cover the streaming endpoint and delayed elapsed time (WebFlux)

**Files:**
- Modify: `examples/common/src/main/java/examples/test/webflux/AbstractReactiveAccessLogTest.java`

**Interfaces:**
- Consumes: `ReactiveExampleController` `/api/stream` (Flux of 5 items at 100 ms intervals) and `/api/delayed` (100 ms delayed Mono).
- Produces: nothing another task depends on.

- [ ] **Step 1: Replace the `delayedResponseEmitsAccessEvent` test**

Replace the existing method (currently asserting only the URI) with:

```java
    @Test
    void delayedResponseEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/delayed");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getRequestURI()).isEqualTo("/api/delayed");
            // The endpoint delays 100 ms, so a correctly measured elapsed time cannot be
            // 0 (guards the AccessLog contract's "0 nanos means unknown" fallback, PR #212).
            softly.assertThat(event.getElapsedTime()).isGreaterThan(0L);
        });
    }
```

- [ ] **Step 2: Append the streaming test**

Insert after `delayedResponseEmitsAccessEvent`:

```java
    @Test
    void streamingResponseEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/stream");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getRequestURI()).isEqualTo("/api/stream");
            softly.assertThat(event.getStatusCode()).isEqualTo(200);
        });
    }
```

- [ ] **Step 3: Run the reactive tests**

Run:

```bash
./gradlew :examples:tomcat-webflux:test :examples:jetty-webflux:test --tests '*ReactiveAccessLogTest*'
```

Expected: `BUILD SUCCESSFUL`; 12 tests per module (11 from Task 3 state + 1 streaming), 0 failures.

- [ ] **Step 4: Commit**

```bash
git add examples/common/src/main/java/examples/test/webflux/AbstractReactiveAccessLogTest.java
git commit -m "test(examples): cover streaming responses and delayed elapsed time

The /api/stream endpoint existed without any test, and elapsed time was
only asserted as >= 0. The delayed endpoint sleeps 100 ms, so its event
must report a positive elapsed time (guards the AccessLog contract's
'0 nanos means unknown' fallback from PR #212)."
```

---

### Task 5: TeeFilter form-urlencoded policy end-to-end (#209, #212)

**Files:**
- Modify: `examples/common/src/main/java/examples/mvc/MvcExampleController.java` (add `/api/form` endpoint)
- Modify: `examples/common/src/main/java/examples/HttpClientTestUtils.java` (add `postForm`)
- Modify: `examples/common/src/main/java/examples/test/mvc/AbstractTeeFilterTest.java` (add default-policy suppression test)
- Create: `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/TeeFilterFormCaptureTest.java`

**Interfaces:**
- Consumes: `teefilter` profile (`examples/tomcat-mvc/src/test/resources/application-teefilter.yml` sets `logback.access.tee-filter.enabled=true`).
- Produces: `HttpClientTestUtils.postForm(String url, String body)` → `HttpResponse<String>`; `POST /api/form` echoing parameters as JSON. Task 5 is their only consumer.

Background for the implementer: the starter's `TomcatRequestDataExtractor` does not buffer form-urlencoded bodies; it reconstructs them from `getParameterMap()` and removes pairs that also appear in the query string (PR #212). `BodyCapturePolicy` suppresses the reconstructed body unless `application/x-www-form-urlencoded` is listed in `allowed-content-types` (PR #209). The `/api/form` endpoint uses `@RequestParam` (not `@RequestBody`) so Tomcat parses the parameters and `getParameterMap()` sees the body.

- [ ] **Step 1: Add the form endpoint to `MvcExampleController.java`**

Add import `org.springframework.http.MediaType;` (keep imports alphabetical) and insert after the `echo` method:

```java
    @PostMapping(path = "/form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, String> form(@RequestParam Map<String, String> params) {
        return params;
    }
```

- [ ] **Step 2: Add `postForm` to `HttpClientTestUtils.java`**

Insert after the `post` method:

```java
    /**
     * Sends a POST request with an application/x-www-form-urlencoded body.
     *
     * @param url  the URL to send the request to
     * @param body the URL-encoded form body (for example, "a=1&b=2")
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> postForm(final String url, final String body) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }
```

- [ ] **Step 3: Add the default-policy suppression test to `AbstractTeeFilterTest.java`**

Insert before the final closing brace of the class (after `getRequestHasNoRequestBody`):

```java
    @Test
    void formPostBodyIsNotCapturedByDefault() throws Exception {
        // The default allowlist deliberately excludes application/x-www-form-urlencoded
        // so form-login credential submissions never reach the access log (PR #209).
        final var response = HttpClientTestUtils.postForm(
                getBaseUrl() + "/api/form",
                "username=alice&password=hunter2");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRequestContent()).satisfiesAnyOf(
                content -> assertThat(content).isNull(),
                content -> assertThat(content).doesNotContain("hunter2")
        );
    }
```

- [ ] **Step 4: Create `TeeFilterFormCaptureTest.java` in tomcat-mvc**

```java
package examples.tomcatmvc;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests form-urlencoded body capture when the operator opts in via
 * {@code allowed-content-types}. Tomcat-only, like the other TeeFilter tests.
 * <p>
 * Capturing form bodies is opt-in because the default policy suppresses them to keep
 * credentials out of access logs (PR #209). The captured body is reconstructed from
 * {@code getParameterMap()} with query-string pairs removed, because the query string is
 * already logged separately (PR #212).
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "logback.access.tee-filter.allowed-content-types=application/x-www-form-urlencoded"
)
@ActiveProfiles("teefilter")
class TeeFilterFormCaptureTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @LocalServerPort
    int port;

    ListAppender<IAccessEvent> listAppender;

    @BeforeEach
    void setUp() {
        listAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "list");
        AccessEventTestUtils.reset(listAppender);
    }

    String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void formBodyIsCapturedWhenContentTypeIsAllowed() throws Exception {
        final var response = HttpClientTestUtils.postForm(
                baseUrl() + "/api/form",
                "username=alice&password=hunter2");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRequestContent())
                .contains("username=alice")
                .contains("password=hunter2");
    }

    @Test
    void queryStringParametersAreNotAttributedToBody() throws Exception {
        final var response = HttpClientTestUtils.postForm(
                baseUrl() + "/api/form?source=query",
                "field=bodyvalue");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRequestContent())
                .contains("field=bodyvalue")
                .doesNotContain("source");
    }
}
```

- [ ] **Step 5: Run the TeeFilter tests**

Run:

```bash
./gradlew :examples:tomcat-mvc:test --tests '*TeeFilter*'
```

Expected: `BUILD SUCCESSFUL`; `TeeFilterTest` runs 5 tests (4 + suppression) and `TeeFilterFormCaptureTest` runs 2, 0 failures. The jetty-mvc `TeeFilterTest` stays `@Disabled`. If `queryStringParametersAreNotAttributedToBody` fails because the captured content is empty or the request returns 415, report the actual output — do not weaken assertions.

- [ ] **Step 6: Commit**

```bash
git add examples/common/src/main/java/examples/mvc/MvcExampleController.java \
        examples/common/src/main/java/examples/HttpClientTestUtils.java \
        examples/common/src/main/java/examples/test/mvc/AbstractTeeFilterTest.java \
        examples/tomcat-mvc/src/test/java/examples/tomcatmvc/TeeFilterFormCaptureTest.java
git commit -m "test(examples): cover TeeFilter form-urlencoded policy end-to-end

PR #209 removed form-urlencoded from the default body-capture allowlist
and PR #212 stopped attributing query-string parameters to the body;
both had unit coverage only. Assert the default suppression in the
shared TeeFilter base and add an opt-in capture test covering the
allowed-content-types override and the query subtraction."
```

---

### Task 6: RemoteIpValve forwarded-header attributes end-to-end (Tomcat)

**Files:**
- Modify: `examples/common/src/main/java/examples/HttpClientTestUtils.java` (add `getWithHeader`)
- Create: `examples/common/src/main/java/examples/test/common/AbstractForwardedHeaderTest.java`
- Create: `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/ForwardedHeaderTest.java`
- Create: `examples/tomcat-webflux/src/test/java/examples/tomcatwebflux/ForwardedHeaderTest.java`

**Interfaces:**
- Produces: `HttpClientTestUtils.getWithHeader(String url, String headerName, String headerValue)` → `HttpResponse<String>`. Task 6 is its only consumer.

Background for the implementer: `RemoteIpValve` rewrites the request's remote address while the request is processed and restores the original before access logging runs; it publishes the forwarded values as request attributes instead. The starter's `TomcatValve.initInternal` auto-enables request-attribute resolution when `RemoteIpValve` is present in the engine pipeline, so the logged event must carry the forwarded address. This is the only E2E coverage of `TomcatRequestAttributeResolver`'s attribute path. The feature is Tomcat-specific; Jetty modules get no counterpart.

- [ ] **Step 1: Add `getWithHeader` to `HttpClientTestUtils.java`**

Insert after the `getWithQuery` method:

```java
    /**
     * Sends a GET request with a single extra header.
     *
     * @param url         the URL to send the request to
     * @param headerName  the header name
     * @param headerValue the header value
     * @return the HTTP response
     * @throws Exception if an error occurs
     */
    public static HttpResponse<String> getWithHeader(
            final String url,
            final String headerName,
            final String headerValue) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(url))
                .header(headerName, headerValue)
                .GET()
                .build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }
```

- [ ] **Step 2: Create `AbstractForwardedHeaderTest.java`**

```java
package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for RemoteIpValve forwarded-header tests (Tomcat only).
 * <p>
 * RemoteIpValve rewrites the remote address during request processing and restores the
 * original before access logging runs, publishing the forwarded values as request
 * attributes instead. The starter auto-enables request-attribute resolution when it
 * detects RemoteIpValve, so the logged event must report the forwarded address. This is
 * the end-to-end coverage of the request-attribute resolution path.
 * <p>
 * Subclasses must set {@code server.tomcat.remoteip.remote-ip-header} and an
 * {@code internal-proxies} pattern that trusts the test client's loopback address.
 */
public abstract class AbstractForwardedHeaderTest {

    private ListAppender<IAccessEvent> listAppender;

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    /**
     * Returns the base URL for the test server.
     *
     * @return the base URL (e.g., "http://localhost:8080")
     */
    protected abstract String getBaseUrl();

    @BeforeEach
    void setUpForwardedHeaderAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
    }

    @Test
    void forwardedForHeaderOverridesRemoteAddress() throws Exception {
        final var response = HttpClientTestUtils.getWithHeader(
                getBaseUrl() + "/api/hello",
                "X-Forwarded-For",
                "203.0.113.7");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRemoteAddr()).isEqualTo("203.0.113.7");
    }
}
```

- [ ] **Step 3: Create the two module subclasses**

Full content for `examples/tomcat-mvc/src/test/java/examples/tomcatmvc/ForwardedHeaderTest.java`:

```java
package examples.tomcatmvc;

import examples.test.common.AbstractForwardedHeaderTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Forwarded-header (RemoteIpValve) tests for Tomcat + MVC.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "server.tomcat.remoteip.remote-ip-header=x-forwarded-for",
                // Trust both loopback forms so the JDK client's connection is accepted
                // as coming from an internal proxy regardless of IPv4/IPv6 resolution.
                "server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1|0:0:0:0:0:0:0:1"
        }
)
class ForwardedHeaderTest extends AbstractForwardedHeaderTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @LocalServerPort
    int port;

    @Override
    protected LogbackAccessContext getLogbackAccessContext() {
        return logbackAccessContext;
    }

    @Override
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
```

`examples/tomcat-webflux/src/test/java/examples/tomcatwebflux/ForwardedHeaderTest.java` is identical except for two lines: `package examples.tomcatwebflux;` and Javadoc `* Forwarded-header (RemoteIpValve) tests for Tomcat + WebFlux.`

- [ ] **Step 4: Run the forwarded-header tests**

Run:

```bash
./gradlew :examples:tomcat-mvc:test :examples:tomcat-webflux:test --tests '*ForwardedHeaderTest*'
```

Expected: `BUILD SUCCESSFUL`; 1 test per module, 0 failures. If the event reports the loopback address instead of `203.0.113.7`, the valve did not trust the client connection — report the actual `getRemoteAddr()` value; do not delete the internal-proxies property to make it pass.

- [ ] **Step 5: Commit**

```bash
git add examples/common/src/main/java/examples/HttpClientTestUtils.java \
        examples/common/src/main/java/examples/test/common/AbstractForwardedHeaderTest.java \
        examples/tomcat-mvc/src/test/java/examples/tomcatmvc/ForwardedHeaderTest.java \
        examples/tomcat-webflux/src/test/java/examples/tomcatwebflux/ForwardedHeaderTest.java
git commit -m "test(examples): cover RemoteIpValve forwarded-header attributes

RemoteIpValve restores the original remote address before access
logging and publishes forwarded values as request attributes, which the
starter resolves when it auto-detects the valve. That attribute path
had no E2E coverage. Assert that an X-Forwarded-For value reaches the
logged event's remote address on both Tomcat modules."
```

---

### Task 7: Cleanup and full-suite verification

**Files:**
- Modify: `examples/tomcat-mvc/src/test/resources/application.yml`

**Interfaces:** none.

- [ ] **Step 1: Remove the inert profile activation**

Replace the full content of `examples/tomcat-mvc/src/test/resources/application.yml` with:

```yaml
spring:
  application:
    name: tomcat-mvc-test
```

(The removed `spring.profiles.active: test` referenced a profile with no `application-test.yml` and no matching `<springProfile>` block anywhere, so it configured nothing.)

- [ ] **Step 2: Run the full build**

Run:

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL` — Spotless, Detekt, unit tests, apiCheck, and all example-module integration tests pass. If any test fails, report the failing test and output; do not adjust assertions to force green.

- [ ] **Step 3: Commit**

```bash
git add examples/tomcat-mvc/src/test/resources/application.yml
git commit -m "chore(examples): remove inert test profile activation

The tomcat-mvc test application.yml activated a 'test' profile that has
no matching properties file or springProfile block, so it configured
nothing and only made the module diverge from its siblings."
```

---

## Completion checklist

- All 7 tasks committed on `worktree-test-examples-e2e-optimization`.
- `./gradlew clean build` green (Task 7 Step 2).
- Expected suite shape afterwards: 4 modules × (UrlFilteringTest 4 tests / SpringProfileTest 4 tests) via shared cases; MVC modules run 9 basic + 5 security tests; WebFlux modules run 12 reactive tests; tomcat-mvc runs 5 + 2 TeeFilter tests and 1 forwarded-header test; tomcat-webflux runs 1 forwarded-header test.
- No remaining per-module copy of the URL filtering or Spring profile test bodies.
