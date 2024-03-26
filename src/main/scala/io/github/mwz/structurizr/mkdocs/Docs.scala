package io.github.mwz.structurizr.mkdocs

import com.structurizr.documentation.Section as SSection

trait Docs:
  def sections(): Seq[Section]

object Docs:
  def apply(ssections: Seq[SSection]) = new Docs {

    // TODO: generate empty index if no docs are present
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
        Section(title, resolveEmbedded(section.getContent()), filename)
      }

    // Replace embedded image references from the docs (embed:ImageId)
    // with the actual svg image
    def resolveEmbedded(md: String): String =
      val embedPattern = """\(embed:([a-zA-z0-9]+)\)""".r
      embedPattern.replaceSomeIn(
        md,
        m => {
          Option(m.group(1)).map(imgId =>
            val svg = s"img/${imgId}.svg"
            s"(${svg}) <center>${imgId} ([svg](${svg}))</center>"
          )
        }
      )
  }
