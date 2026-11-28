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

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Files
import munit.CatsEffectSuite

import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.XML

class SvgLinksTest extends CatsEffectSuite:
  test("group only complete directed edges and preserve SVG content"):
    val original =
      """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><?plantuml 1.2026.8?>
        |<g>
        |  <g class="entity"><a xlink:href="../a/?x=1&amp;y=2"><line/><polygon/><text>A &amp; B</text></a></g>
        |  <!-- keep this comment --><line/><line/><polygon/>
        |  <line/><polygon/><text>reads &lt;data&gt;</text><text>[HTTP]</text>
        |  <line style="stroke:none"/><text>table</text>
        |  <line/><rect/>
        |  <g class="legend"><line/><polygon/><text>legend</text></g>
        |</g></svg>""".stripMargin
    Files[IO].tempFile.use { svg =>
      val contents: IO[String] = Files[IO].readUtf8(svg).compile.string
      for
        _       <- Stream.emit(original).through(Files[IO].writeUtf8(svg)).compile.drain
        _       <- SvgLinks.process(svg, groupElkLinks = true, linkHitArea = true)
        grouped <- contents
        _       <- IO {
          val root: Elem = XML.loadString(grouped)
          val drawing: NodeSeq = root \ "g"
          val links: NodeSeq = (drawing \ "g").filter(node => (node \ "@class").text == "link")
          assertEquals(links.map(node => (node \ "line").count(_.attribute("class").isEmpty)).toList, List(2, 1))
          assertEquals(
            links.map(node => (node \ "line").count(_.attribute("class").exists(_.text == "link-hit"))).toList,
            List(2, 1)
          )
          assertEquals((root \\ "line").count(_.attribute("class").nonEmpty), 3)
          assertEquals(links.map(node => (node \ "polygon").size).toList, List(1, 1))
          assertEquals(
            links.map(node => (node \ "text").map(_.text).toList).toList,
            List(Nil, List("reads <data>", "[HTTP]"))
          )
          assertEquals(links.map(_.namespace).distinct.toList, List("http://www.w3.org/2000/svg"))
          assertEquals((drawing \ "line").size, 2)
          assertEquals((drawing \ "text").text, "table")
          val anchor: Node = (root \\ "a").head
          assertEquals(anchor.attribute("http://www.w3.org/1999/xlink", "href").map(_.text), Some("../a/?x=1&y=2"))
          assertEquals(anchor.text, "A & B")
          assert(grouped.contains("<!-- keep this comment -->"))
          assert(grouped.contains("<?plantuml 1.2026.8?>"))
        }
        _ <- SvgLinks.process(svg, groupElkLinks = true, linkHitArea = true)
        _ <- assertIO(contents, grouped)
      yield ()
    }

  test("a dot relationship keeps its curved path, link and SVG id") {
    val original =
      """<svg xmlns="http://www.w3.org/2000/svg"><g><g class="link" id="rel">
        |<a href="details"><path id="route" d="M0,0 Q5,10 20,0" transform="translate(2 3)" style="stroke:black"/>
        |<polygon points="19,0 20,0 19,1"/><text>details</text></a>
        |</g></g></svg>""".stripMargin
    Files[IO].tempFile.use { svg =>
      val contents: IO[String] = Files[IO].readUtf8(svg).compile.string
      val process: IO[Unit] = SvgLinks.process(svg, groupElkLinks = false, linkHitArea = true)
      for
        _       <- Stream.emit(original).through(Files[IO].writeUtf8(svg)).compile.drain
        _       <- process
        updated <- contents
        _       <- IO {
          val root: Elem = XML.loadString(updated)
          val anchor: Node = (root \\ "a").head
          assertEquals((anchor \ "@href").text, "details")
          val paths: NodeSeq = anchor \ "path"
          assertEquals(paths.size, 2)
          val hit = paths.head
          val visible = paths.last
          assertEquals((visible \ "@id").text, "route")
          assert(hit.attribute("id").isEmpty)
          assertEquals(hit.attribute("d"), visible.attribute("d"))
          assertEquals(hit.attribute("transform"), visible.attribute("transform"))
          assertEquals((hit \ "@class").text, "link-hit")
          assertEquals((hit \ "@stroke-width").text, "6")
          assertEquals((hit \ "@pointer-events").text, "stroke")
          assertEquals((hit \ "@opacity").text, "0")
          assertEquals((anchor \ "polygon").size, 1)
          assertEquals((anchor \ "text").size, 1)
        }
        _ <- process
        _ <- assertIO(contents, updated)
      yield ()
    }
  }
