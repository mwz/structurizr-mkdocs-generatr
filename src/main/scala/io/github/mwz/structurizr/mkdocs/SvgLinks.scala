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
import scala.collection.Seq
import scala.xml.Document
import scala.xml.Elem
import scala.xml.MetaData
import scala.xml.Node
import scala.xml.Text

private[mkdocs] object SvgLinks:
  val hitWidth = "6"

  def process(staged: Path, groupElkLinks: Boolean, linkHitArea: Boolean): IO[Unit] =
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
                val children: Seq[Node] = if (groupElkLinks) groupEdges(drawing.child.toList) else drawing.child
                drawing.copy(child = children.map {
                  case link: Elem if linkHitArea && link.label == "g" && hasClass(link, "link") => withHitAreas(link)
                  case other                                                                    => other
                })
              case other => other
            })
          case other => other
        }
        // The input is fully read and this file is private to the staged render.
        if (updated == document.children) IO.unit
        else Stream.emits(updated).map(_.toString).through(Files[IO].writeUtf8(staged)).compile.drain
      }

  private def hasClass(node: Node, name: String): Boolean =
    node.attribute("class").exists(_.text.split("\\s+").contains(name))

  private val geometry = Set("d", "x1", "y1", "x2", "y2", "transform")
  private val hitAttributes: MetaData =
    <g class="link-hit" opacity="0" fill="none" stroke="transparent" stroke-width={hitWidth}
       pointer-events="stroke" aria-hidden="true"/>.attributes

  private def withHitAreas(group: Elem): Elem =
    if (group.child.exists(hasClass(_, "link-hit"))) group
    else
      val hits: Seq[Elem] = group.child.collect {
        case segment: Elem if segment.label == "path" || visibleLine(segment) =>
          val coordinates: MetaData = segment.attributes.filter(attr => geometry.contains(attr.key))
          segment.copy(attributes = hitAttributes.append(coordinates), child = Nil)
      }
      // Keep linked strokes inside their original anchor so the wider target follows the same URL.
      val children: Seq[Node] = group.child.map {
        case anchor: Elem if anchor.label == "a" => withHitAreas(anchor)
        case other                               => other
      }
      group.copy(child = hits ++ children)

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
