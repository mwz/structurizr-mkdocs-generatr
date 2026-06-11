# AGENTS.md

This file provides guidance to AI agents when working with code in this
repository.

## What this is

A CLI tool that reads a Structurizr `workspace.dsl` (C4 architecture model) and
generates a static MkDocs site (optionally a Backstage TechDocs site). It
produces markdown pages, an `mkdocs.yml` config, and SVG/PNG diagrams. Written
in Scala 3 on Cats Effect / fs2, requires **Java 21+** and **Graphviz** (used by
PlantUML for diagram rendering).

## Commands

```bash
# Build the fat jar (sbt-assembly; packageBin is wired to assembly)
sbt assembly                      # -> target/.../structurizr-mkdocs-generatr.jar

# Run against the repo root (expects workspace.dsl + mkdocs.tmpl.yml present)
sbt run

# Run against the bundled example
sbt "run --path example --assets-dir pictures"
mkdocs serve -w example -f example/mkdocs.yml
```

`GITHUB_TOKEN` (or `github.token` git config) is required by sbt to resolve
dependencies from GitHub Packages — see
[githubTokenSource.sbt](githubTokenSource.sbt). CI sets it to empty for read
access. `PLANTUML_LIMIT_SIZE` env/system-property raises PlantUML's 4096px
diagram size cap.

## Verification

```bash
# Tests (munit-cats-effect)
sbt test
sbt "testOnly io.github.mwz.structurizr.mkdocs.MarkdownTest"                     # single suite
sbt 'testOnly *MarkdownTest -- "--tests=generate image tabs"' # single test by name
sbt clean coverage test                                       # tests with coverage (as in CI)

# Lint / format (scalafmt + scalafix DisableSyntax/RemoveUnused)
sbt scalafmtAll
sbt scalafixAll
```

## Architecture

`Main` is the entry point
([IOCaseApp](src/main/scala/io/github/mwz/structurizr/mkdocs/Main.scala)). The pipeline is: parse
workspace → build content models → render to a `Stream[IO, SectionHierarchy]` →
write files + config + diagrams.

The unifying data type is **`SectionHierarchy`**
([MkDocs.scala](src/main/scala/io/github/mwz/structurizr/mkdocs/MkDocs.scala)):

- `Section(title, md, filename)` — one markdown page.
- `SectionGroup(title, sections)` — a nav group, nestable.

A custom Circe `Encoder[SectionHierarchy]` serialises this same tree into the
`nav:` section of `mkdocs.yml`, so the file layout and the navigation always
match. `MkDocs.writeMkDocsConfig` reads `mkdocs.tmpl.yml`, injects `docs_dir`
and the generated `nav`, and writes `mkdocs.yml`.

Content producers each turn part of the Structurizr workspace into sections:

- [Docs.scala](src/main/scala/io/github/mwz/structurizr/mkdocs/Docs.scala) — project-level docs;
  first section becomes `index.md`; resolves `embed:` image refs.
- [SoftwareSystems.scala](src/main/scala/io/github/mwz/structurizr/mkdocs/SoftwareSystems.scala) —
  the bulk of the logic. Builds a `TreeMap`-based group tree of systems, then
  emits About / Context / Container / Component / Dynamic / Deployment /
  Dependencies sections per system. `normaliseName` is the canonical slug
  function used everywhere for filenames and links — links between pages must
  agree with it.
- [Assets.scala](src/main/scala/io/github/mwz/structurizr/mkdocs/Assets.scala) — copies the static
  assets dir into `docs/img`.
- [Diagrams.scala](src/main/scala/io/github/mwz/structurizr/mkdocs/Diagrams.scala) — exports every
  view to SVG + PNG via PlantUML.

**Ordering matters in `Main.exec`:** diagrams are exported _last_ because
diagram export mutates each element's `url` property (set/restored in
`CustomC4PlantUMLExporter.writeWithUrl`). Don't reorder this without
understanding the mutation.

[C4PlantUmlExporter.scala](src/main/scala/io/github/mwz/structurizr/mkdocs/C4PlantUmlExporter.scala)
subclasses Structurizr's `C4PlantUMLExporter` to inject relative hyperlinks into
the generated SVGs (so diagram elements are clickable and navigate to the
matching MkDocs page). It writes a sentinel `https://temp-url/` prefix and
strips it afterwards to produce relative links. The link targets it computes
(`-container-views`, `-context-views`, `-component-views`, about page) must stay
consistent with the filenames `SoftwareSystems` generates.

`markdown.scala` / `string.scala` hold rendering helpers — notably `imgTabs`
(SVG/PNG content tabs) and `escape` (markdown special-char escaping for table
cells etc.).

## Conventions

- Scala 3 syntax, `maxColumn = 120`, aligned style (see
  [.scalafmt.conf](.scalafmt.conf)). tpolecat is enabled, so warnings are
  strict.
- Effects are Cats Effect `IO`; file and stream IO go through
  `fs2.io.file.Files[IO]` and `fs2.Stream`, not `java.nio` directly. There's a
  `fs2Path` extension in
  [Config.scala](src/main/scala/io/github/mwz/structurizr/mkdocs/Config.scala) to bridge
  `java.nio.Path`.
- Structurizr's Java API is interop-heavy: expect `.asScala`, nullable getters
  wrapped with `Option(...)` / mouse's `.toOption`, and `given`-based passing of
  `ViewSet` / `Set[Relationship]` / `SoftwareSystem`.
- Tests use `munit.FunSuite` and build `SWorkspace` instances directly (no DSL
  files).

## Agent Sandbox

In a sandboxed environment that only permits writes under the project dir and
`$TMPDIR`, sbt fails because it writes to `~/.sbt`, `~/.ivy2`, and the system
temp dir. Until the sandbox is granted those write paths directly, run sbt in
**offline mode** with the writes redirected to writable locations (the
git-ignored `project/.sbtboot/` and `project/boot/`), e.g.:

```bash
sbt --batch \
    -Dsbt.offline=true \
    -Dsbt.boot.directory=project/boot/ \
    -Djava.io.tmpdir=$TMPDIR \
    -Dsbt.global.base=project/.sbtboot/global \
    -Dsbt.ivy.home=project/.sbtboot/ivy \
    "scalafixAll --check"
```

Each flag covers a write that the sandbox blocks: `sbt.boot.directory`
(`~/.sbt/boot` launcher lock), `java.io.tmpdir` (the system temp dir, where
sbt's boot server socket lives — the JVM default `/var/folders/...` is _not_ the
sandbox `$TMPDIR`), `sbt.global.base` (`~/.sbt/1.0/staging`), and `sbt.ivy.home`
(`~/.ivy2/.sbt.ivy.lock`).

**Offline mode needs the caches pre-populated.** If a required artifact isn't
already in `~/.ivy2` / `~/Library/Caches/Coursier`, the build fails rather than
fetching it — do a single online warm-up run first, e.g. `sbt update`.
