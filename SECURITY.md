# Security Policy

## Supported Versions

| Version              | Supported          |
|----------------------|--------------------|
| Latest minor release | :white_check_mark: |
| Older releases       | :x:                |

Security patches are released only against the latest minor version. Upgrade to the latest minor release to stay covered.

## Reporting a Vulnerability

**Do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Submit the report through [GitHub Private Vulnerability Reporting](https://github.com/seijikohara/logback-access-spring-boot-starter/security/advisories/new) so that the issue can be triaged and fixed before public disclosure.

### What to Include

- A description of the vulnerability.
- Steps to reproduce.
- An assessment of the potential impact.
- A suggested fix or workaround, if available.

### Response Timeline

- **Initial response**: within 7 days of the report.
- **Fix development**: best effort, targeting 30 days.
- **Public disclosure**: coordinated, after the fix is released. Maximum 90 days from the initial report.

## Disclosure Policy

The project follows a coordinated disclosure process:

1. The reporter submits the vulnerability via GitHub Private Vulnerability Reporting.
2. The maintainers acknowledge receipt within 7 days.
3. A fix is developed, reviewed, and tested.
4. A new release is published with the fix.
5. The vulnerability is publicly disclosed through a GitHub Security Advisory crediting the reporter (unless they request anonymity).

## Hardening and Logging Sensitive Data

Access logs record attacker-controlled request data. Configure logging defensively:

- **Log injection / forging.** The request line, URI, query string, header values, and cookie
  values are written to the log verbatim. When logging to a line-oriented sink, escape carriage
  returns and line feeds (for example with Logback's `%replace`) or use a structured JSON encoder,
  so an attacker cannot inject forged log lines via control characters in a header or URI.
- **Secrets in patterns.** Avoid `%{Authorization}i`, `%{Cookie}i`, `%{Set-Cookie}o`, individual
  cookie values, and the session ID in production patterns. These can expose bearer tokens,
  credentials, and session identifiers.
- **Request/response bodies.** TeeFilter is disabled by default. Even when enabled, form submissions
  (`application/x-www-form-urlencoded`) and non-empty payloads without a `Content-Type` are suppressed
  unless explicitly added to `allowed-content-types`. Enable it only when body capture is required,
  restrict it with `allowed-content-types` and `include-hosts` / `exclude-hosts`, and note that it
  buffers full bodies in memory regardless of `max-payload-size`.

See the [advanced guide](https://seijikohara.github.io/logback-access-spring-boot-starter/guide/advanced) for body-capture configuration and platform behavior.

## Scope

This policy covers the following Maven Central artifacts:

- `io.github.seijikohara:logback-access-spring-boot-starter-core`
- `io.github.seijikohara:logback-access-spring-boot-starter`

Vulnerabilities in transitive dependencies (Logback Access, Spring Boot, Tomcat, Jetty, etc.) should be reported to the respective projects. Reports that describe how this starter exposes such a vulnerability are in scope.
