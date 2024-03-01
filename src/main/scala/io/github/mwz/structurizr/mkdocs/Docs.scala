package io.github.mwz.structurizr.mkdocs

import com.structurizr.documentation.Section as SSection

case class Section(title: String, md: String, filename: String)

trait Docs {
  def sections(): Seq[Section]
}

object Docs {
  def apply(ssections: Seq[SSection]) = new Docs {
    def sections(): Seq[Section] =
      ssections.map { section =>
        val title: String =
          section
            .getContent()
            .stripLeading()
            .linesIterator
            .find(_.nonEmpty)
            .map(_.replaceAll("#+", "").trim())
            .getOrElse("Untitled")
        Section(title, section.getContent(), section.getFilename())
      }
  }
}
