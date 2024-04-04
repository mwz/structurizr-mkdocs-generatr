package io.github.mwz.structurizr.mkdocs

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
  def imgTabs(id: String): String =
    val svg = s"../img/${id}.svg"
    val png = s"../img/${id}.png"
    s"""${id}
        |=== "SVG"
        |    <center>![${id}SVG](${svg})</center>
        |=== "PNG"
        |    <center>![${id}PNG](${png})</center>\n""".stripMargin
