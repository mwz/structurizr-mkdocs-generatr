package io.github.mwz.structurizr.mkdocs

import com.structurizr.view.ImageView

object markdown:

  /**
   * Escape the following literal characters:
   *   - \ backslash
   *   - ` backtick (see also escaping backticks in code)
   *   - * asterisk
   *   - _ underscore
   *   - { } curly braces
   *   - [ ] brackets
   *   - < > angle brackets
   *   - ( ) parentheses
   *   - # hash sign
   *   - + plus sign
   *   - \- minus sign (hyphen)
   *   - . dot
   *   - ! exclamation mark
   *   - \| pipe
   */
  def escape(s: String): String =
    // regex with a group to capture the escapable characters
    raw"""([\\`*_\{\}\[\]\<\>\(\)#+\-\.\!\|]{1})""".r.replaceSomeIn(
      s,
      m =>
        Option(m.group(1)).map { s =>
          if s == "\\" then """\\\\"""
          else s"\\\\$s"
        }
    )

  // generate content tabs with images
  def imgTabs(id: String, title: Option[String] = None, description: Option[String] = None): String =
    val svg     = s"../img/${id}.svg"
    val png     = s"../img/${id}.png"
    val heading = title.getOrElse(id)
    val desc    = description.fold("")(s => s"<center>$s</center>\n")
    s"""${heading}
        |=== "SVG"
        |    <center>![${id}SVG](${svg})</center>
        |=== "PNG"
        |    <center>![${id}PNG](${png})</center>
        |${desc}""".stripMargin

  // generate an embedded image from an ImageView
  def imageView(view: ImageView): String =
    val description = view
      .getDescription()
      .toOption
      .fold("")(s => s"<center>${s}</center>\n\n")
    val heading = view.getTitle().toOption.getOrElse(view.getKey())
    s"""<center>${heading}</center>
        |<center>![${view.getKey()}IMG](${view.getContent()})</center>
        |${description}""".stripMargin
