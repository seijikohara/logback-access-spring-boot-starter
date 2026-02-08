# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| Latest  | :white_check_mark: |
| Older   | :x:                |

Only the latest minor release receives security patches. Users are encouraged to upgrade to the latest version.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, use [GitHub Private Vulnerability Reporting](https://github.com/seijikohara/logback-access-spring-boot-starter/security/advisories/new) to submit a report. This ensures the vulnerability can be assessed and addressed before public disclosure.

### What to Include

- A description of the vulnerability
- Steps to reproduce the issue
- Potential impact assessment
- Suggested fix (if any)

### Response Timeline

- **Initial response**: within 7 days
- **Fix development**: best effort, targeting 30 days
- **Public disclosure**: coordinated disclosure after the fix is released (maximum 90 days)

## Disclosure Policy

This project follows a coordinated disclosure process:

1. The reporter submits a vulnerability via GitHub Private Vulnerability Reporting
2. The maintainers acknowledge receipt within 7 days
3. A fix is developed and tested
4. A new release is published with the fix
5. The vulnerability is publicly disclosed via a GitHub Security Advisory

## Scope

This policy covers the following packages published to Maven Central:

- `io.github.seijikohara:logback-access-spring-boot-starter-core`
- `io.github.seijikohara:logback-access-spring-boot-starter`
