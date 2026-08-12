/*
 * Copyright 2024-2026 Michael Wizner and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    val svg = s"../img/${id}.svg"
    val png = s"../img/${id}.png"
    val heading = title.getOrElse(id)
    val desc = description.fold("")(s => s"<center>$s</center>\n")
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
        |<center><img src="${view.getContent()}" alt="${view.getKey()}IMG"></center>
        |${description}""".stripMargin
