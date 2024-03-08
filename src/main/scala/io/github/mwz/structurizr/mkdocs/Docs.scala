package io.github.mwz.structurizr.mkdocs

import com.structurizr.documentation.Section as SSection

trait Docs:
  def sections(): Seq[Section]

object Docs:
  def apply(ssections: Seq[SSection]) = new Docs {
    def sections(): Seq[Section] =
      ssections.zipWithIndex.map { case (section, i) =>
        // make the first section the index
        val filename: String = if i == 0 then "index.md" else section.getFilename()
        val title: Option[String] =
          section
            .getContent()
            .stripLeading()
            .linesIterator
            .find(_.nonEmpty)
            .map(_.replaceAll("#+", "").trim())
        Section(title, section.getContent(), filename)
      }
  }
