# Structurizr MkDocs Generatr

**A static [MkDocs](https://www.mkdocs.org) site generator for
[C4 architecture model](https://c4model.com) documentation created with
[Structurizr](https://structurizr.com) DSL.**

It can be used to generate a standalone [MkDocs](https://www.mkdocs.org) site as
well as a [TechDocs](https://backstage.io/docs/features/techdocs) site for
Backstage.

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
- system dependencies view
- svg diagrams with hyperlinks

Missing features (planned for future releases):

- system and container docs
- workspace, system and container adrs
  ([reference](https://docs.structurizr.com/dsl/language#adrs))
- system deployment views
  ([reference](https://docs.structurizr.com/dsl/language#deployment-view))
- image views
  ([reference](https://docs.structurizr.com/dsl/language#image-view))
- copy static images to the output directory

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

   - download the jar from Github Releases and run it:

     ```bash
     java -jar structurizr-mkdocs-generatr.jar
     ```

   - or download the jar from GitHub Packages and run it:

     ```bash
     curl -H "Authorization: token $GITHUB_TOKEN" -L -O \
     https://maven.pkg.github.com/mwz/structurizr-mkdocs-generatr/io/github/mwz/structurizr-mkdocs-generatr/0.1.0/structurizr-mkdocs-generatr-0.1.0.jar

     java -jar structurizr-mkdocs-generatr-0.1.0.jar
     ```

   - or use [Coursier](https://get-coursier.io) to run it (since the artifact is
     published to GitHub Packages, you'll need additional Coursier configuration
     to make it work)

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
- `--docs-dir` - the directory where the documentation markdown files will be
  generated, relative to the `path` argument. Needs to match MkDocs' `docs_dir`
  setting (defaults to `docs`)

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
sbt "run --path example"
```

Alternatively run the project using an IDE of your choice. There is a
`launch.json` configuration provided for VSCode to run the project against the
root and the example directory.

## Contributing

Please feel free to contribute to this project by opening an issue or a pull
request.

If you have any questions, please open a new discussion.

## Acknowledgments

This project is inspired by
[avisi-cloud/structurizr-site-generatr](https://github.com/avisi-cloud/structurizr-site-generatr).
