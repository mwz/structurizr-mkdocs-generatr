package io.github.mwz.structurizr.mkdocs

import cats.effect.IO
import cats.effect.kernel.Clock
import cats.effect.testkit.TestControl
import fs2.Stream
import fs2.io.file.Files
import munit.CatsEffectSuite

import java.time.LocalDateTime
import java.time.ZoneOffset
import scala.concurrent.duration.*

class MkDocsTest extends CatsEffectSuite:
  val fixedTime     = LocalDateTime.of(2024, 6, 13, 12, 0, 0)
  val fixedDuration = fixedTime.toEpochSecond(ZoneOffset.UTC).seconds
  val contentSuffix =
    """|
       |
       |<div style="font-size: small; font-style: italic;">
       |13/06/2024 @ 12:00
       |</div>""".stripMargin

  val tmpDir = ResourceTestLocalFixture("tmpDir", Files[IO].tempDirectory)

  override def munitFixtures = List(tmpDir)

  test("write section hierarchy"):
    val dir      = tmpDir()
    val basePath = dir / "mkdocs"

    val sections = SectionGroup(
      Some("Section Group"),
      Seq(Section(Some("Title"), "# Title\n\nContent", "index.md"))
    )
    val prog = MkDocs(basePath.toNioPath, Clock[IO])
      .writeSectionHierarchy(Stream.emit(sections))

    TestControl
      .execute(prog)
      .flatMap(_.advanceAndTick(fixedDuration)) *>
      assertIO(Files[IO].walk(dir).drop(1).compile.toList, List(basePath, basePath / "index.md")) *>
      assertIO(Files[IO].readUtf8(basePath / "index.md").compile.string, "# Title\n\nContent" + contentSuffix)

  test("write MkDocs config"):
    val dir      = tmpDir()
    val basePath = dir / "mkdocs"

    val ymlTemplate =
      """site_name: Test
        |repo_url: https://github.com/test/test
        |edit_uri: edit/main/
        |plugins:
        |  - techdocs-core
        |  - inline-select-svg
        |theme:
        |  name: material
        |  features:
        |    - content.tabs.link""".stripMargin

    val expected =
      s"""site_name: Test
          |repo_url: https://github.com/test/test
          |edit_uri: edit/main/
          |plugins:
          |- techdocs-core
          |- inline-select-svg
          |theme:
          |  name: material
          |  features:
          |  - content.tabs.link
          |docs_dir: ${basePath.toString}
          |nav:
          |- Section Group:
          |  - Title: index.md
          |""".stripMargin

    val sections = SectionGroup(
      Some("Section Group"),
      Seq(Section(Some("Title"), "# Title\n\nContent", "index.md"))
    )

    // write ymlTemplate to a file
    val writeYmlTemplate = Stream
      .emit(ymlTemplate)
      .through(fs2.text.utf8.encode)
      .through(Files[IO].writeAll(dir / "mkdocs.tmpl.yml"))
      .compile
      .drain

    val prog = MkDocs(basePath.toNioPath, Clock[IO])
      .writeMkDocsConfig(Stream.emit(sections))

    writeYmlTemplate *>
      prog *>
      assertIO(Files[IO].readUtf8(dir / "mkdocs.yml").compile.string, expected)
