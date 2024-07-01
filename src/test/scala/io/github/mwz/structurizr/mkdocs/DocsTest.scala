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
