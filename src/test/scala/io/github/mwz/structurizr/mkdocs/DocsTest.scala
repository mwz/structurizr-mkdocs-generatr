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

import com.structurizr.documentation.Format
import com.structurizr.documentation.Section as SSection
import munit.FunSuite

case class TestSection(content: String, filename: String)

def testSsections(sections: Seq[TestSection]): Seq[SSection] =
  for s: TestSection <- sections yield
    val section = SSection(Format.Markdown, s.content)
    section.setFilename(s.filename)
    section

class DocsTest extends FunSuite:
  test("turn first section into index if no index present"):
    val ssections = testSsections(Seq(TestSection("# Title\n\nContent", "filename.md")))
    assertEquals(
      Docs(ssections).sections(),
      Seq(
        Section(Some("Title"), "# Title\n\nContent", "index.md")
      )
    )

  test("generate empty index if no docs available"):
    assertEquals(
      Docs(Seq.empty).sections(),
      Seq(
        Section(Some("Home"), "", "index.md")
      )
    )

  test("convert sections into internal representation"):
    val ssections = testSsections(
      Seq(
        TestSection("# Home\n\nContent", "index.md"),
        TestSection("# Title\n\nContent", "filename.md")
      )
    )
    assertEquals(
      Docs(ssections).sections(),
      Seq(
        Section(Some("Home"), "# Home\n\nContent", "index.md"),
        Section(Some("Title"), "# Title\n\nContent", "docs/filename.md")
      )
    )

  test("don't resolve embedded images in markdown code block"):
    val ssections = testSsections(
      Seq(TestSection("# Hello\n\nContent\n```markdown\n![](embed:Diagram)\n```", "index.md"))
    )

    val content = """# Hello
                    |
                    |Content
                    |```markdown
                    |![](embed:Diagram)
                    |```""".stripMargin
    assertEquals(
      Docs(ssections).sections(),
      Seq(
        Section(Some("Hello"), content, "index.md")
      )
    )

  test("resolve embedded images"):
    assertEquals(
      Docs.resolveEmbedded("![](embed:my-Diagram_2)"),
      """my-Diagram_2
        |=== "SVG"
        |    <center>![my-Diagram_2SVG](../img/my-Diagram_2.svg)</center>
        |=== "PNG"
        |    <center>![my-Diagram_2PNG](../img/my-Diagram_2.png)</center>
        |""".stripMargin
    )

  test("resolve embedded images in markdown"):
    val ssections = testSsections(
      Seq(TestSection("# Hello\n\nContent\n![](embed:Diagram)", "index.md"))
    )

    val content = """# Hello
                    |
                    |Content
                    |Diagram
                    |=== "SVG"
                    |    <center>![DiagramSVG](../img/Diagram.svg)</center>
                    |=== "PNG"
                    |    <center>![DiagramPNG](../img/Diagram.png)</center>
                    |""".stripMargin
    assertEquals(
      Docs(ssections).sections(),
      Seq(
        Section(Some("Hello"), content, "index.md")
      )
    )
