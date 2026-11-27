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
import fs2.data.xml.dom
import fs2.data.xml.events
import fs2.data.xml.referenceResolver
import fs2.data.xml.scalaXml.ScalaXmlBuilder
import fs2.io.file.Files
import fs2.io.file.Path

import scala.annotation.tailrec
import scala.xml.Document
import scala.xml.Elem
import scala.xml.Node
import scala.xml.Text

private[mkdocs] object ElkSvg:
  def groupLinks(staged: Path): IO[Unit] =
    Files[IO]
      .readUtf8(staged)
      .through(events[IO, String](includeComments = true))
      .through(referenceResolver[IO]())
      .through(dom.documents[IO, Document])
      .compile
      .onlyOrError
      .flatMap { document =>
        val updated: Seq[Node] = document.children.map {
          case svg: Elem if svg.label == "svg" =>
            svg.copy(child = svg.child.map {
              case drawing: Elem if drawing.label == "g" && drawing.attribute("class").isEmpty =>
                drawing.copy(child = groupEdges(drawing.child.toList))
              case other => other
            })
          case other => other
        }
        // The input is fully read and this file is private to the staged render.
        if (updated == document.children) IO.unit
        else Stream.emits(updated).map(_.toString).through(Files[IO].writeUtf8(staged)).compile.drain
      }

  private def visibleLine(node: Node): Boolean =
    node.label == "line" && !node.attribute("style").exists(_.text.contains("stroke:none"))

  private def whitespace(node: Node): Boolean = node match
    case Text(value) => value.isBlank
    case _           => false

  // PlantUML draws all segments, then the arrowhead, then zero or more label lines.
  // The arrowhead separates adjacent relationships even when the first has no label.
  @tailrec
  private def groupEdges(nodes: List[Node], reversed: List[Node] = Nil): List[Node] = nodes match
    case first :: _ if visibleLine(first) =>
      val (lines, rest) = nodes.span(node => visibleLine(node) || whitespace(node))
      rest match
        case arrow :: tail if arrow.label == "polygon" =>
          val (labels, next) = tail.span(node => node.label == "text" || whitespace(node))
          val group = <g class="link">{lines}{arrow}{labels}</g>
          groupEdges(next, group :: reversed)
        case _ => groupEdges(rest, lines.reverse ::: reversed)
    case head :: tail => groupEdges(tail, head :: reversed)
    case Nil          => reversed.reverse
