package io.github.mwz.structurizr.mkdocs

import caseapp.*

import java.nio.file.Path

object cli:
  case class CommandLineArgs(
      @HelpMessage(
        "Path to the root of the project containing the Structurizr workspace.dsl and the mkdocs.tmpl.yml files (defaults to './')"
      )
      path: Path = Path.of("./"),
      @HelpMessage(
        "The directory where the documentation markdown files will be generated, relative to the 'path' argument. Needs to match MkDocs' 'docs_dir' setting (defaults to 'docs')"
      )
      docsDir: String = "docs"
  )
