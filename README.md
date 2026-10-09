# Structurizr MkDocs Generatr

[![Static Badge](https://img.shields.io/badge/version-v0.3.0-blue)](https://github.com/mwz/structurizr-mkdocs-generatr/packages/2196232?version=0.3.0)
[![Coverage Status](https://coveralls.io/repos/github/mwz/structurizr-mkdocs-generatr/badge.svg?branch=main)](https://coveralls.io/github/mwz/structurizr-mkdocs-generatr?branch=main)

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
     (no token required) and run it:

     ```bash
     curl -L -O https://github.com/mwz/structurizr-mkdocs-generatr/releases/latest/download/structurizr-mkdocs-generatr.jar

     java -jar structurizr-mkdocs-generatr.jar
     ```

   - or download the jar from GitHub Packages and run it. GitHub Packages
     requires authentication even for public packages, so `$GITHUB_TOKEN` must
     be a
     [personal access token (classic)](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages)
     with the `read:packages` scope:

     ```bash
     curl -H "Authorization: token $GITHUB_TOKEN" -L -O \
     https://maven.pkg.github.com/mwz/structurizr-mkdocs-generatr/io/github/mwz/structurizr-mkdocs-generatr/0.3.0/structurizr-mkdocs-generatr-0.3.0.jar

     java -jar structurizr-mkdocs-generatr-0.3.0.jar
     ```

   - or use [Coursier](https://get-coursier.io) to run it (since the artifact is
     published to GitHub Packages, you'll need additional Coursier configuration
     with a `read:packages` token to make it work)

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
  [Temurin LTS](https://adoptium.net/temurin/releases/?version=21).
  `.tool-versions` files is provided, so you can used
  [asdf](https://asdf-vm.com/guide/getting-started.html) to install it.

- **Graphviz** The project uses Graphviz to generate diagrams - please install
  Graphviz on your machine. You can download it from
  [here](https://plantuml.com/graphviz-dot).

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
mkdocs serve -w example -f example/mkdocs.yml
```

Alternatively run the project using an IDE of your choice. There is a
`launch.json` configuration provided for VSCode to run the project against the
root and the example directory.

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
