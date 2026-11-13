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

import io.github.mwz.structurizr.mkdocs.markdown.imgTabs
import com.structurizr.documentation.Section as SSection

trait Docs:
  def sections(): Seq[Section]

object Docs:
  def apply(ssections: Seq[SSection]) = new Docs:
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
  // with the actual svg image.
  // A-Z, a-z, 0-9, _ and - are supported in the image id.
  // Also exclude code blocks with markdown example of the embed syntax.
  def resolveEmbedded(md: String): String =
    val embedPattern = """(?<!```md\s|```markdown\s)(!\[.*?\])\(embed:([\w-]+)\)(?!\s```)""".r
    embedPattern.replaceSomeIn(
      md,
      m => (Option(m.group(2)).map(imgTabs(_)))
    )
