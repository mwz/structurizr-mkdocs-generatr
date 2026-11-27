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

class ElkSvgTest extends CatsEffectSuite:
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
        _       <- ElkSvg.groupLinks(svg)
        grouped <- contents
        _       <- IO {
          val root: Elem = XML.loadString(grouped)
          val drawing: NodeSeq = root \ "g"
          val links: NodeSeq = (drawing \ "g").filter(node => (node \ "@class").text == "link")
          assertEquals(links.map(node => (node \ "line").size).toList, List(2, 1))
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
        _ <- ElkSvg.groupLinks(svg)
        _ <- assertIO(contents, grouped)
      yield ()
    }
