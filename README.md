# Structurizr MkDocs Generatr

**A static [MkDocs](https://www.mkdocs.org) site generator for
[C4 architecture model](https://c4model.com) documentation created with
[Structurizr](https://structurizr.com) DSL.**

It can be used to generate a standalone [MkDocs](https://www.mkdocs.org) site as
well as a [TechDocs](https://backstage.io/docs/features/techdocs) site for
Backstage.

## Features

These are the features that are currently supported by this project:

- project-level docs
- systems overview page
- system context views
- system container views
- system component views
- system dynamic views
- system dependencies
- svg diagrams with hyperlinks

Missing features:

- system and container docs
- system and container adrs
- system deployment views

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
   `mkdocs.tmpl.yml` files are located. It will create `mkdocs` directory with
   all the static content and `mkdocs.yml` config.

   - download the jar from Github Releases and run it:

     ```bash
     java -jar structurizr-mkdocs-generatr.jar
     ```

   - download the jar from GitHub Packages and run it:

     ```bash
     curl -H "Authorization: token $GITHUB_TOKEN" -L -O \
     https://maven.pkg.github.com/mwz/structurizr-mkdocs-generatr/io/github/mwz/structurizr-mkdocs-generatr/0.1.0/structurizr-mkdocs-generatr-0.1.0.jar

     java -jar structurizr-mkdocs-generatr-0.1.0.jar
     ```

   - use [Coursier](https://get-coursier.io) to run it (since the artifact is
     published to GitHub Packages, you'll need additional Coursier configuration
     to make it work)

## Requirements

- **Java 21+**  
  The project is written in Scala and requires Java 21 or higher to run. I
  recommend using [Temurin](https://adoptium.net/temurin/releases).

- **svg inlining**  
  The project generates diagrams in png and svg formats. The svg images need to
  be inlined to support hyperlinks for navigation. Please use an svg inline
  plugin of your choice, I recommend using
  [mkdocs-plugin-inline-svg](https://pypi.org/project/mkdocs-plugin-inline-ext-svg).

  ```bash
  pip install mkdocs-plugin-inline-svg
  ```

- **TechDocs**  
  To generate a TechDocs site, you need to include the `techdocs-core` plugin in
  the `mkdocs.tmpl.yml` file. This plugin is not needed for a standalone MkDocs
  site.

  ```bash
  pip install mkdocs-techdocs-core
  ```

## Contributing

Please feel free to contribute to this project by opening an issue or a pull
request.

If you have any questions, please open a new discussion thread.

## Credits

This project is inspired by
[structurizr-site-generatr](https://github.com/avisi-cloud/structurizr-site-generatr).
