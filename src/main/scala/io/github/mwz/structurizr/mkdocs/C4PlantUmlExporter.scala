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

import com.structurizr.Workspace as SWorkspace
import com.structurizr.`export`.IndentingWriter
import com.structurizr.`export`.plantuml.C4PlantUMLExporter
import com.structurizr.model.Container
import com.structurizr.model.Element
import com.structurizr.model.SoftwareSystem
import com.structurizr.view.ModelView

import scala.jdk.CollectionConverters.*

trait CustomC4PlantUMLExporter extends C4PlantUMLExporter

// Adds custom URLs for Software System and Container elements
object CustomC4PlantUMLExporter:
  def apply(workspace: SWorkspace): CustomC4PlantUMLExporter = new CustomC4PlantUMLExporter():
    val tmpUrl: String = "https://temp-url/"

    // Assumes a fixed URL structure
    def baseUrl(system: SoftwareSystem): String =
      s"${tmpUrl}../../software-systems/${SoftwareSystems.normaliseName(system.getName())}"

    override def writeElement(view: ModelView, element: Element, writer: IndentingWriter): Unit =
      element match
        case system: SoftwareSystem =>
          write(view, system, writer)
        case container: Container =>
          write(view, container, writer)
        case _ =>
          super.writeElement(view, element, writer)

    def hasContainerViews(system: SoftwareSystem): Boolean =
      workspace
        .getViews()
        .getContainerViews()
        .asScala
        .exists(_.getSoftwareSystemId() == system.getId())

    def hasContextViews(system: SoftwareSystem): Boolean =
      workspace
        .getViews()
        .getSystemContextViews()
        .asScala
        .exists(_.getSoftwareSystemId() == system.getId())

    def hasComponentViews(container: Container): Boolean =
      workspace
        .getViews()
        .getComponentViews()
        .asScala
        .exists(_.getContainerId() == container.getId())

    // Write Software System element
    def write(view: ModelView, system: SoftwareSystem, writer: IndentingWriter): Unit =
      val viewSystem: Option[SoftwareSystem] = Option(view.getSoftwareSystem())

      val url =
        val url: String = baseUrl(system)
        viewSystem match
          // this view is for the same system
          case Some(viewSystem) if viewSystem == system =>
            // link to container views if it has any, otherwise don't link
            if (hasContainerViews(system)) Some(s"${url}-container-views")
            else None
          case _ =>
            // in case of other system, link to the context views or the about page
            if (hasContextViews(system))
              Some(s"${url}-context-views")
            // Otherwise, use the base url - navigate to the about page
            else Some(url)

      url.fold(super.writeElement(view, system, writer))(writeWithUrl(view, system, writer, _))

    // Write Container element
    def write(view: ModelView, container: Container, writer: IndentingWriter): Unit =
      val parentSystem = container.getSoftwareSystem()
      def targetUrl(baseUrl: String): Option[String] =
        Option(view.getSoftwareSystem().getContainerWithId(container.getId())) match
          // parent system of this container is the same as this view's system
          case Some(_) =>
            // same system & has component views - component
            if (hasComponentViews(container)) Some(s"${baseUrl}-component-views")
            // same system - don't link
            else None
          case None =>
            // different system - link to the container views if has container views
            if (hasContainerViews(parentSystem)) Some(s"${baseUrl}-container-views")
            // link to the system context views if has context views
            else if (hasContextViews(parentSystem)) Some(s"${baseUrl}-context-views")
            // Otherwise, use the base url - navigate to the about page
            else Some(baseUrl)

      val url = for
        containerSystem <- Option(container.getSoftwareSystem())
        system          <- Option(workspace.getModel().getSoftwareSystemWithId(containerSystem.getId()))
        url             <- targetUrl(baseUrl(system))
      yield url

      url.fold(super.writeElement(view, container, writer))(writeWithUrl(view, container, writer, _))

    def writeWithUrl(view: ModelView, element: Element, writer: IndentingWriter, url: String): Unit =
      val elementUrl   = element.getUrl()
      val urlWithSlash = if (url.endsWith("/")) url else url + "/"
      element.setUrl(urlWithSlash)
      val iw = new IndentingWriter()
      super.writeElement(view, element, iw)
      element.setUrl(elementUrl)
      // remove the temporary url to make it relative
      iw.toString()
        .replace(tmpUrl, "")
        .split(java.lang.System.lineSeparator())
        .foreach(writer.writeLine)
