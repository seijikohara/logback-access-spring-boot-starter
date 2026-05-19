---
paths:
  - "**/*.md"
  - "docs/**/*.ts"
---

# Documentation Rules

Project-specific conventions for Markdown content under `README.md`, `examples/README.md`, `CONTRIBUTING.md`, `SECURITY.md`, and the VitePress site under `docs/`. The general technical-writing style (active voice, imperative form, present tense, 1 idea per sentence) is defined in the user-level `~/.claude/CLAUDE.md` and applies here unconditionally — this file adds the project-specific overlay.

## Pattern Variable Notation

Use the `%{name}<flag>` form for Logback Access conversion words throughout all documentation:

- ✓ `%{Referer}i`, `%{User-Agent}i`, `%{name}r`, `%{name}c`, `%{name}o`
- ✗ `%i{Referer}`, `%i{User-Agent}` (legacy form — do not use in docs even though Logback Access accepts both)

The notation in the table is the same as in the example pattern strings — they must not diverge.

## VitePress Documentation (`docs/`)

### File layout

```
docs/
├── index.md                       # English landing page
├── ja/index.md                    # Japanese landing page (must mirror docs/index.md)
├── guide/{page}.md                # English guide pages
└── ja/guide/{page}.md             # Japanese counterpart (must mirror docs/guide/{page}.md)
```

Locale routing and sidebar entries are configured in `docs/.vitepress/config.ts`. Adding a new guide page requires updating both the English (`themeConfig.sidebar`) and Japanese (`locales.ja.themeConfig.sidebar`) entries.

### English / Japanese parity

- Every English page under `docs/guide/` has a counterpart under `docs/ja/guide/` with **identical section structure**. When a heading is added, renamed, or removed on one side, mirror the change on the other.
- Cross-page links use locale-aware paths: English pages link to `/guide/...`, Japanese pages link to `/ja/guide/...`. Never link from a Japanese page to an English page (or vice versa).
- Verify the structure with:
  ```bash
  diff <(grep -n '^## \|^### ' docs/guide/foo.md | sed 's/[^#]*//') \
       <(grep -n '^## \|^### ' docs/ja/guide/foo.md | sed 's/[^#]*//')
  ```

### Build verification

After substantive Markdown changes under `docs/`, run the VitePress build to catch broken links and Mermaid parse errors:

```bash
cd docs && npx vitepress build
```

## Mermaid Diagrams

Use the explicit `id["label"]` form when a node or subgraph label contains spaces, slashes, or other punctuation. GitHub's Mermaid parser fails on unquoted multi-word labels.

```
flowchart TB
    subgraph app["Spring Boot Application"]   # ✓ quoted with id
        A[HTTP Request] --> B{Embedded Server}
        G -->|JSON| J["Logstash/ELK"]          # ✓ quoted (contains "/")
    end
```

- Avoid `<br/>` inside flowchart node labels. If a line break is genuinely needed, quote the label and use `\n` inside the quotes.
- In `sequenceDiagram`, prefer short participant aliases over multi-word raw names. Use `participant X as Some Long Name` when a display label is required.

## Table Style

- Description columns: end every cell with a period.
- The first column (key / property / name) does not take a trailing period; it is a label.
- Use em dash (—) or `—` for "not applicable" rather than `-` or `N/A`.
- Use ✓ for supported, ✗ for unsupported, — for not applicable.

## List Style

- Bullet items in a list end with a period when each item is a complete sentence or descriptive phrase. Single-word labels do not.
- Mixed lists (sentence + label) within one list are not allowed — split into two lists if needed.

## Code Block Languages

Always tag code blocks with their language so VitePress and GitHub render syntax highlighting correctly:

- `kotlin`, `java`, `groovy` for source code
- `xml`, `yaml`, `json`, `toml` for configuration
- `bash` for shell commands (do not use `sh` or no tag)
- `mermaid` for diagrams

## Heading Style

- README sub-sections under `## Section` use Title Case (`### Step 1: Add the Dependency`).
- The VitePress guide pages also use Title Case for English headings.
- Japanese pages use natural Japanese phrasing; do not force English-style capitalization.
- Never embed pattern variables (e.g., `%h`) inside headings; reference them in the body instead.

## Cross-References

- Inside `docs/` (VitePress), link via site-relative paths: `[Configuration](/guide/configuration)`.
- Inside top-level Markdown files (README, CONTRIBUTING, etc.), link to the hosted docs site for guide content: `https://seijikohara.github.io/logback-access-spring-boot-starter/guide/...`.
- Link to source files only when the path is stable (data models, configuration entry points) — avoid linking to line numbers, which rot quickly.

## What Not to Document

- Do not document static test counts (`39 tests`, `Total: 135 active tests`). These drift. Describe the coverage in prose instead.
- Do not document the exact list of `application/*+json` content types in two places; reference the canonical list in `docs/guide/advanced.md`.
- Do not promise behavior that is not implemented. Verify against `LogbackAccessProperties.kt`, `BodyCapturePolicy.kt`, `SecurityFilter.kt`, etc. before adding new claims.
