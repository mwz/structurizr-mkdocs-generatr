package io.github.mwz.structurizr.mkdocs

import com.structurizr.`export`.IndentingWriter
import com.structurizr.`export`.plantuml.C4PlantUMLExporter
import com.structurizr.model.Element
import com.structurizr.model.SoftwareSystem
import com.structurizr.view.ModelView
import com.structurizr.Workspace as SWorkspace

import scala.jdk.CollectionConverters.*

trait CustomC4PlantUMLExporter extends C4PlantUMLExporter

object CustomC4PlantUMLExporter:
  def apply(workspace: SWorkspace): CustomC4PlantUMLExporter = new CustomC4PlantUMLExporter():
    val tmpUrl = "https://temp-url/"

    override def writeElement(view: ModelView, element: Element, writer: IndentingWriter): Unit =
      element match
        case system: SoftwareSystem =>
          write(view, system, writer)
        case _ =>
          super.writeElement(view, element, writer)

    def write(view: ModelView, system: SoftwareSystem, writer: IndentingWriter): Unit =
      Option(workspace.getModel().getSoftwareSystemWithId(system.getId())) match
        case None =>
          super.writeElement(view, system, writer)
        case Some(system) =>
          val baseUrl = s"${tmpUrl}../../software-systems/${SoftwareSystems.normaliseName(system.getName())}"
          // Determine the url based on whether a system context view exists
          val targetUrl = workspace
            .getViews()
            .getSystemContextViews()
            .asScala
            .find(_.getSoftwareSystem().getId() == system.getId())
            .fold(baseUrl)(_ => s"${baseUrl}-context-views")
          writeWithUrl(view, system, writer, targetUrl)

    def writeWithUrl(view: ModelView, element: Element, writer: IndentingWriter, url: String): Unit =
      val elementUrl = element.getUrl()
      element.setUrl(url)
      val iw = new IndentingWriter()
      super.writeElement(view, element, iw)
      element.setUrl(elementUrl)
      // remove the temporary url to make it relative
      iw.toString()
        .replace(tmpUrl, "")
        .split(java.lang.System.lineSeparator())
        .foreach(writer.writeLine)
