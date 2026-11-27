# Structurizr MkDocs Generatr

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mwz/structurizr-mkdocs-generatr)](https://central.sonatype.com/artifact/io.github.mwz/structurizr-mkdocs-generatr)
[![Sonatype snapshot](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fmwz%2Fstructurizr-mkdocs-generatr%2Fbadges%2Fsnapshot.json)](#snapshots)
[![Codecov](https://codecov.io/gh/mwz/structurizr-mkdocs-generatr/branch/main/graph/badge.svg)](https://app.codecov.io/gh/mwz/structurizr-mkdocs-generatr)

**A static [MkDocs](https://www.mkdocs.org) site generator for
[C4 architecture model](https://c4model.com) documentation created with
[Structurizr](https://structurizr.com) DSL.**

It can be used to generate a standalone [MkDocs](https://www.mkdocs.org) site as
well as a [TechDocs](https://backstage.io/docs/features/techdocs) site for
Backstage.

See the demo of a standalone MkDocs site
[here](https://mwz.github.io/structurizr-mkdocs-generatr).

## Features

This project is in development and not all Structurizr features are supported
yet. Here's a list of features that are currently supported:

- project-level docs
  ([reference](https://docs.structurizr.com/dsl/language#docs))
- systems overview page
- system context views
  ([reference](https://docs.structurizr.com/dsl/language#systemcontext-view))
- system container views
  ([reference](https://docs.structurizr.com/dsl/language#container-view))
- system component views
  ([reference](https://docs.structurizr.com/dsl/language#component-view))
- system dynamic views
  ([reference](https://docs.structurizr.com/dsl/language#dynamic-view))
- system deployment views
  ([reference](https://docs.structurizr.com/dsl/language#deployment-view))
- system container and component image views
  ([reference](https://docs.structurizr.com/dsl/language#image-view))
- system dependencies view
- svg diagrams with hyperlinks
- automatically copy static assets to the output directory
- extra CSS support (MkDocs only,
  [reference](https://www.mkdocs.org/user-guide/configuration/#extra_css))
- groovy scripting support
  ([reference](https://docs.structurizr.com/dsl/scripts))

Missing features:

- system and container docs
- workspace, system and container adrs
  ([reference](https://docs.structurizr.com/dsl/language#adrs))
- support for other scripting languages (Kotlin, Ruby, JavaScript)

## How to use

1. Create a template of MkDocs configuration file, which will be used to
   generate the mkdocs.yml config. Place it where you want to generate
   mkdocs.yml and name it `mkdocs.tmpl.yml`.

   **mkdocs.tmpl.yml**:

   ```yaml
   site_name: "My Docs"
   repo_url: https://github.com/org/repo
   edit_uri: edit/main/
   plugins:
     - inline-select-svg
     - glightbox
   theme:
     name: material
     features:
       - content.tabs.link
   ```

   For TechDocs, you'll need to include the TechDocs plugin in the plugins
   section:

   ```yaml
   site_name: "My Docs"
   repo_url: https://github.com/org/repo
   edit_uri: edit/main/
   plugins:
     - techdocs-core
     - inline-select-svg
   theme:
     name: material
     features:
       - content.tabs.link
   ```

2. Run the generator in the directory where the Structurizr `workspace.dsl` and
   `mkdocs.tmpl.yml` files are located. It will create `docs` directory with all
   the static content and `mkdocs.yml` config.
   - download the jar from
     [GitHub Releases](https://github.com/mwz/structurizr-mkdocs-generatr/releases)
     and run it:

     ```bash
     curl -L -O https://github.com/mwz/structurizr-mkdocs-generatr/releases/latest/download/structurizr-mkdocs-generatr.jar

     java -jar structurizr-mkdocs-generatr.jar
     ```

   - or use [Coursier](https://get-coursier.io) to run it straight from
     [Maven Central](https://central.sonatype.com/artifact/io.github.mwz/structurizr-mkdocs-generatr):

     ```bash
     cs launch 'io.github.mwz:structurizr-mkdocs-generatr:<version>'
     ```

3. Preview the generated site:
   - MkDocs ([installation](https://www.mkdocs.org/getting-started))
     ```bash
     mkdocs serve -w docs
     ```
   - TechDocs CLI
     ([installation](https://backstage.io/docs/features/techdocs/cli))
     ```bash
     techdocs-cli serve --no-docker -v
     ```

### Snapshots

Snapshots are unreleased builds published from `main` to
[Sonatype's snapshot repository](https://central.sonatype.org/publish/publish-portal-snapshots/).
The snapshot badge above shows the latest published snapshot version. Each
snapshot has its own version.

Replace `<snapshot-version>` with the full version from the badge, including
`-SNAPSHOT`, and run it with Coursier:

```bash
cs launch \
  --repository https://central.sonatype.com/repository/maven-snapshots/ \
  --ttl 0s \
  'io.github.mwz:structurizr-mkdocs-generatr:<snapshot-version>'
```

The `--ttl 0s` option makes Coursier check for snapshot updates on each run.
Append generator arguments after `--`, for example
`-- --path path/to/project --assets-dir pictures`.

### Arguments

The generator accepts the following arguments:

- `--path` - path to the root of the project containing the Structurizr
  `workspace.dsl` and the `mkdocs.tmpl.yml` files (defaults to current working
  directory `./`)
- `--assets-dir` - the directory where the static assets are located, relative
  to the `path` argument (defaults to `img`)
- `--docs-dir` - the directory where the documentation markdown files will be
  generated, relative to the `path` argument. Needs to match MkDocs' `docs_dir`
  setting (defaults to `docs`)
- `--force-diagrams` - regenerate every diagram
- `--plantuml-layout` - default layout engine for graph views (`dot` or `elk`,
  defaults to `dot`)

### Diagram layout

PlantUML uses Graphviz dot by default. To use the bundled Eclipse Layout Kernel
(ELK) for graph views, run the generator with `--plantuml-layout elk`. The
selected engine draws both the SVG and PNG. Sequence diagrams use PlantUML's
sequence layout.

You can also set the engine in `workspace.dsl`. The following example selects
ELK for the workspace and dot for one container view:

```dsl
views {
    properties {
        "generatr.plantuml.layout" "elk"
    }

    container internetBankingSystem "Containers" {
        include *
        properties {
            "generatr.plantuml.layout" "dot"
        }
    }
}
```

A view property takes precedence over the workspace property, which takes
precedence over the CLI default. To enable ELK for just one view, set
`"generatr.plantuml.layout" "elk"` in that view's properties.

If you already use a PlantUML include file, you can put `!pragma layout elk` in
it and reference it with `"plantuml.includes" "layout-elk.puml"` in the view's
properties. PlantUML resolves the relative path from the directory where you
start the generator. An explicit ELK pragma selects ELK even when the
generator's default is dot.

In an inline SVG, `g.link:hover` targets the relationship's segments, arrowhead
and label together. The example stylesheet styles both ELK's `line` elements and
dot's `path` elements. The example uses ELK for its
[system context view](example/workspace.dsl).

### Diagram exports

The generator reuses a view's cached SVG and PNG when the PlantUML definition
and renderer configuration match. It restores the images if the output files are
missing or have changed. Views with custom PlantUML includes, external images or
inline PlantUML function calls such as `%date()` render on every run.

For the default output directory `docs`, the generator stores rendered images in
`.docs.generatr-cache` beside it. Restore this directory before running the
generator in CI. A fresh `docs` directory receives only the current images.
After a successful export, the generator removes cache entries for views that
have been renamed or deleted.

For an example of saving the cache per commit and using `restore-keys` to reuse
diagrams from earlier commits, see the
[Cache rendered diagrams step](.github/workflows/build-example.yml).

The generator records the diagram files it writes to `docs/img` in
`.docs.generatr-state.json`. On a rerun, it removes obsolete diagram files that
still match their recorded checksums. It keeps current static assets and files
that were changed by hand. The parent of the docs directory must be writable
when the generator needs to update the state file.

To refresh every image, for example after a font change, run the generator with
`--force-diagrams`.

### Extra CSS

You can add custom styles by declaring an `extra_css` list in your
`mkdocs.tmpl.yml` template:

```yaml
extra_css:
  - css/extra.css
```

The setting is carried over to the generated `mkdocs.yml`, and each referenced
file is copied into the docs directory, preserving any nested folder structure.
Paths are resolved relative to the `mkdocs.tmpl.yml` file (e.g. `css/extra.css`
→ `<path>/css/extra.css`), and MkDocs in turn resolves them relative to
`docs_dir`, so the same relative path keeps working after the copy. If a
referenced file doesn't exist, the setting is still written to `mkdocs.yml` but
a warning is printed and nothing is copied.

See the example [extra.css](example/css/extra.css) file which adds the following
MkDocs customisations:

- highlights the relationship arrows on mouse-over - see the
  [website](https://mwz.github.io/structurizr-mkdocs-generatr/software-systems/internet-banking-system-container-views)
  for a demo
- maximises the content space by hiding the right-hand on-page Table of Contents
  and widening the overall layout

> **Note:** this only works when serving/building with MkDocs. It is not
> supported for Backstage TechDocs.

## Requirements

- **Java 21+** The project is written in Scala and requires Java 21 or higher to
  run. I recommend using
  [Temurin LTS](https://adoptium.net/temurin/releases/?version=21). A
  `.tool-versions` file is provided, so you can use
  [mise](https://mise.jdx.dev/getting-started.html) to install it.

- **Graphviz** Install Graphviz for views that use the default dot engine. You
  can download it from [here](https://plantuml.com/graphviz-dot).

- **svg inlining** The project generates diagrams in png and svg formats. The
  svg images need to be inlined to support hyperlinks for navigation. Please use
  an svg inline plugin of your choice, I recommend using
  [inline-select-svg](https://pypi.org/project/mkdocs-inline-select-svg-plugin/).

  ```bash
  pip install mkdocs-inline-select-svg-plugin
  ```

  mkdocs.tmpl.yml:

  ```yaml
  plugins:
    - inline-select-svg
  ```

- **MkDocs GLightbox** (optional) plugin to support image lightbox with
  GLightbox - makes the experience of viewing images in mkdocs a bit nicer

  ```bash
  pip install mkdocs-glightbox
  ```

  mkdocs.tmpl.yml:

  ```yaml
  plugins:
    - glightbox
  ```

- **TechDocs** To generate a TechDocs site, you need to include the
  `techdocs-core` plugin in the `mkdocs.tmpl.yml` file. This plugin is not
  needed for a standalone MkDocs site.

  ```bash
  pip install mkdocs-techdocs-core
  ```

- **MkDocs or TechDocs CLI** (to preview) Either MkDocs or TechDocs CLI is
  required to preview the generated site. See the docs on how to install those
  tools:
  - [MkDocs](https://www.mkdocs.org/getting-started)
  - [TechDocs](https://backstage.io/docs/features/techdocs/cli)

## Development

To run this project locally, you need to have JDK 21+ installed on your machine.
To build the example site, you also need [uv](https://docs.astral.sh/uv/). The
`.tool-versions` file pins Java, Python and uv, so `mise install` installs all
of them.

To run using sbt:

```bash
sbt run
```

You'll need to place your `workspace.dsl` and other Structurizr files in the
root of this project and create a `mkdocs.tmpl.yml` file as described above.

Once executed, you can preview the website using the following command:

```bash
mkdocs serve -w docs
```

or for TechDocs:

```bash
techdocs-cli serve --no-docker -v
```

To run the the project against the `example` directory execute the following:

```bash
sbt "run --path example --assets-dir pictures"
uv run --project example mkdocs serve -w example -f example/mkdocs.yml
```

The MkDocs dependencies for the example are defined in `example/pyproject.toml`
and locked in `example/uv.lock`. `uv run` creates the environment on first use.

Alternatively run the project using an IDE of your choice. There is a
`launch.json` configuration provided for VSCode to run the project against the
root and the example directory.

### Test coverage

To run the tests and generate a coverage report, run:

```bash
sbt clean coverage test coverageReport
```

Open `target/scoverage-report/index.html` to view the report locally. GitHub
Actions uploads the XML report to
[Codecov](https://app.codecov.io/gh/mwz/structurizr-mkdocs-generatr) for pushes
and pull requests to `main`.

## Contributing

Please feel free to contribute to this project by opening an issue or a pull
request.

If you have any questions, please open a new discussion.

## FAQ

##### My png diagrams are cropped, how can I fix it?

This project uses PlantUML to generate diagrams. PlantUML limits image width and
height to 4096. There is an environment variable that you can set to override
this limit - `PLANTUML_LIMIT_SIZE`. You have to define this variable before
launching structurizr-mkdocs-generatr, e.g.:

```bash
set PLANTUML_LIMIT_SIZE=8192
```

or

```bash
setenv PLANTUML_LIMIT_SIZE 8192
```

Another way is using a java system property:

```bash
java -DPLANTUML_LIMIT_SIZE=8192 -jar structurizr-mkdocs-generatr.jar
```

See more details on the PlantUML [website](https://plantuml.com/faq).

## Acknowledgments

This project is inspired by
[avisi-cloud/structurizr-site-generatr](https://github.com/avisi-cloud/structurizr-site-generatr).
