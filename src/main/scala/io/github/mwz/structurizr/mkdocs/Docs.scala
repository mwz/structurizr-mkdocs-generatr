package io.github.mwz.structurizr.mkdocs

import io.github.mwz.structurizr.mkdocs.markdown.imgTabs
import com.structurizr.documentation.Section as SSection

trait Docs:
  def sections(): Seq[Section]

object Docs:
  def apply(ssections: Seq[SSection]) = new Docs {
    def sections(): Seq[Section] =
      val sections: Seq[Section] =
        ssections.zipWithIndex.map { case (section, i) =>
          // make the first section the index
          val filename: String = if i == 0 then "index.md" else s"docs/${section.getFilename()}"
          val title: Option[String] =
            section
              .getContent()
              .stripLeading()
              .linesIterator
              .find(_.nonEmpty)
              .map(_.replaceAll("#+", "").trim())
          Section(title, resolveEmbedded(section.getContent()), filename)
        }

      // generate empty index if no docs are present
      if sections.isEmpty
      then Seq(Section(Some("Home"), "", "index.md"))
      else sections

    // Replace embedded image references from the docs (embed:ImageId)
    // with the actual svg image
    def resolveEmbedded(md: String): String =
      val embedPattern = """(!\[.*?\])\(embed:(\w+)\)""".r
      embedPattern.replaceSomeIn(
        md,
        m => (Option(m.group(2)).map(imgTabs))
      )
  }
