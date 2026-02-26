# Expression Chaining & Kotlin 2.3 Refactoring Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enforce expression chaining throughout all production Kotlin source code and adopt stable Kotlin 2.3 language features where applicable.

**Architecture:** 25 production Kotlin files were audited. 21 are already fully expression-bodied. 4 files have concrete improvement opportunities: when branch cleanup, block-to-expression body conversion, and helper function extraction for complexity management.

**Tech Stack:** Kotlin 2.3, Spring Boot 4.0.2, Gradle Kotlin DSL

**Kotlin 2.3 stable feature applicability:** After exhaustive analysis, no applicable spots were found for guard conditions (no `when(subject)` with extra conditions), multi-dollar interpolation (no `${'$'}` escapes), non-local break/continue (no loops in inline lambdas), or return in expression bodies (simpler patterns suffice). The refactoring focuses on expression body/chaining improvements.

---

### Task 1: BodyCapturePolicy — Remove unnecessary when branch braces

**Files:**
- Modify: `logback-access-spring-boot-starter/src/main/kotlin/.../tee/BodyCapturePolicy.kt:76-98`

**Step 1: Remove braces from when branches in matchesMimePattern()**

Replace lines 76-98:

```kotlin
    private fun matchesMimePattern(
        mimeType: String,
        pattern: String,
    ): Boolean =
        when {
            pattern == mimeType -> true
            pattern.endsWith("/*") -> mimeType.startsWith(pattern.removeSuffix("*"))
            pattern.contains("/*+") ->
                pattern.split("/*+", limit = 2).let { (type, suffix) ->
                    mimeType.startsWith("$type/") && mimeType.endsWith("+$suffix")
                }
            else -> false
        }
```

**Step 2: Run build to verify**

Run: `./gradlew :logback-access-spring-boot-starter:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: TomcatResponseDataExtractor — Convert extractContent() to expression body

**Files:**
- Modify: `logback-access-spring-boot-starter/src/main/kotlin/.../tomcat/TomcatResponseDataExtractor.kt:28-38`

**Step 1: Convert block body with early return to if/else expression body**

Replace lines 28-38:

```kotlin
    fun extractContent(
        request: Request,
        response: Response,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        if (!teeFilterProperties.enabled) null
        else (request.getAttribute(LB_OUTPUT_BUFFER) as? ByteArray)?.let { buffer ->
            BodyCapturePolicy.evaluate(response.contentType, buffer.size.toLong(), teeFilterProperties)
                ?: String(buffer, BodyCapturePolicy.resolveCharset(response.characterEncoding))
        }
```

**Step 2: Run build to verify**

Run: `./gradlew :logback-access-spring-boot-starter:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: TomcatRequestDataExtractor — Extract helpers + expression body conversion

**Files:**
- Modify: `logback-access-spring-boot-starter/src/main/kotlin/.../tomcat/TomcatRequestDataExtractor.kt:53-81`

**Step 1: Extract decodeBufferContent() helper**

Add new private function after `extractAttributes()`:

```kotlin
    private fun decodeBufferContent(
        request: Request,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        (request.getAttribute(LB_INPUT_BUFFER) as? ByteArray)?.let { buffer ->
            BodyCapturePolicy.evaluate(request.contentType, buffer.size.toLong(), teeFilterProperties)
                ?: String(buffer, BodyCapturePolicy.resolveCharset(request.characterEncoding))
        }
```

**Step 2: Extract decodeFormDataContent() helper**

Add new private function:

```kotlin
    private fun decodeFormDataContent(
        request: Request,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        encodeFormDataIfApplicable(request)?.let { formData ->
            val charset = BodyCapturePolicy.resolveCharset(request.characterEncoding)
            BodyCapturePolicy.evaluate(request.contentType, formData.toByteArray(charset).size.toLong(), teeFilterProperties)
                ?: formData
        }
```

**Step 3: Convert extractContent() to expression body using helpers**

Replace extractContent() (lines 53-69):

```kotlin
    fun extractContent(
        request: Request,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        teeFilterProperties
            .takeIf { it.enabled }
            ?.let { decodeBufferContent(request, it) ?: decodeFormDataContent(request, it) }
```

**Step 4: Convert encodeFormDataIfApplicable() to expression body**

Replace encodeFormDataIfApplicable() (lines 71-81):

```kotlin
    private fun encodeFormDataIfApplicable(request: Request): String? =
        BodyCapturePolicy.resolveCharset(request.characterEncoding).name().let { charsetName ->
            request
                .takeIf { isFormUrlEncoded(it) }
                ?.parameterMap
                ?.asSequence()
                ?.flatMap { (key, values) -> values.asSequence().map { key to it } }
                ?.joinToString("&") { (key, value) ->
                    "${encode(key, charsetName)}=${encode(value, charsetName)}"
                }
        }
```

**Step 5: Run build to verify**

Run: `./gradlew :logback-access-spring-boot-starter:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: TeeFilterConfiguration — Scope function improvement

**Files:**
- Modify: `logback-access-spring-boot-starter/src/main/kotlin/.../tee/TeeFilterConfiguration.kt:30-37`

**Step 1: Replace local val with `with()` scope function**

Replace lines 30-37:

```kotlin
    fun logbackAccessTeeFilter(properties: LogbackAccessProperties): FilterRegistrationBean<TeeFilter> =
        FilterRegistrationBean(TeeFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE + ORDER_OFFSET
            addUrlPatterns("/*")
            with(properties.teeFilter) {
                includeHosts?.takeIf { it.isNotBlank() }?.let { addInitParameter(TEE_FILTER_INCLUDES_PARAM, it) }
                excludeHosts?.takeIf { it.isNotBlank() }?.let { addInitParameter(TEE_FILTER_EXCLUDES_PARAM, it) }
            }
        }
```

**Step 2: Run build to verify**

Run: `./gradlew :logback-access-spring-boot-starter:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: Full build + test verification

**Step 1: Run full build with all checks**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL (compile + spotless + detekt + test)

**Step 2: If spotless fails, run auto-fix**

Run: `./gradlew spotlessApply && ./gradlew clean build`
Expected: BUILD SUCCESSFUL
