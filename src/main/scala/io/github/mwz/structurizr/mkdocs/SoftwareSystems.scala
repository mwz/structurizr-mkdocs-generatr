package io.github.mwz.structurizr.mkdocs

import com.structurizr.model.SoftwareSystem
import com.structurizr.view.StaticView
import com.structurizr.view.ViewSet

import java.nio.file.Path
import scala.collection.JavaConverters.*
import scala.collection.immutable.TreeMap
import com.structurizr.view.ModelView

trait Tree
case class System(group: List[String], system: SoftwareSystem)  extends Tree
case class Group(name: String, children: TreeMap[String, Tree]) extends Tree

trait SoftwareSystems:
  def allSystems: Seq[System]
  def tree: TreeMap[String, Tree]
  def prettyPrint: String
  def summaryView: String
  def summarySection: Section
  def sectionHierarchy(): SectionHierarchy

object SoftwareSystems:
  def apply(systems: Seq[SoftwareSystem], views: ViewSet, basePath: Path, separator: String) = new SoftwareSystems:
    given v: ViewSet = views

    lazy val allSystems =
      systems.map { system =>
        system.getGroup() match {
          case null  => System(List.empty, system)
          case group => System(group.split(separator).toList, system)
        }
      }

    lazy val tree: TreeMap[String, Tree] =
      allSystems.foldLeft(TreeMap.empty[String, Tree]) { (tree, system) =>
        populateTree(tree, system.group, None, system)
      }

    // pretty print tree with indentation
    lazy val prettyPrint: String =
      def prettyPrint(tree: Tree, string: String, indent: String = ""): String =
        tree match {
          case System(group, system) =>
            s"\n${indent}- ${system.getName()}"
          case Group(name, children) =>
            s"\n${indent}- ${name}${children.foldLeft("") { (s, c) =>
                s + prettyPrint(c._2, "", indent + "  ")
              }}"
        }
      tree.foldLeft("") { case (s, (_, tree)) =>
        s + prettyPrint(tree, "")
      }

    // Populate the tree recursively
    // TODO: Optimise this
    // TODO: Stack safety
    private def populateTree(
        tree: TreeMap[String, Tree],
        groups: List[String],
        group: Option[Group],
        system: System
    ): TreeMap[String, Tree] =
      group match
        // initial case
        case None =>
          groups match
            case Nil =>
              tree + (system.system.getName() -> system)
            case head :: next =>
              tree.getOrElse(head, Group(head, TreeMap.empty)) match
                case group: Group =>
                  val children = populateTree(group.children, next, Some(group), system)
                  tree + (group.name -> Group(group.name, children))
                case _ =>
                  // Name collision
                  tree
        // recursive call
        case Some(group) =>
          groups match
            case Nil =>
              group.children + (system.system.getName() -> system)
            case head :: next =>
              group.children.getOrElse(head, Group(head, TreeMap.empty)) match
                case group: Group =>
                  val children = populateTree(group.children + (head -> group), next, Some(group), system)
                  group.children + (group.name -> Group(group.name, children))
                case _ =>
                  // Name collision
                  tree

    // A summary view of all software systems
    lazy val summaryView: String =
      val (groups, others) = tree.partition((_, t) => t.isInstanceOf[Group])
      val hasGroups        = groups.nonEmpty
      val othersGroups =
        if (!hasGroups) others
        else TreeMap(("Others" -> Group("Others", others)))

      def indent(level: Int): String =
        if (hasGroups) "  " * math.max(0, level - 1)
        else "  " * level

      def md(tree: Tree, string: String, level: Int = 0): String =
        tree match
          case System(group, system) =>
            val description = Option(system.getDescription())
              .filter(_.trim().nonEmpty)
              .fold("")(d => s"  \n${indent(level)}  `${d}`")
            val link = s"[${system.getName()}](software-systems/${normaliseName(system.getName())}.md)"
            s"\n${indent(level)}- ${link}${description}"
          case Group(name, children) =>
            val prefix = level match
              case 0 => s"##"
              case l => indent(l - 1) + "-"
            s"\n${prefix} ${name}${children.foldLeft("") { (s, c) =>
                s + md(c._2, "", level + 1)
              }}"

      "# Software Systems\n" +
        groups.foldLeft("") { case (s, (_, tree)) =>
          s + md(tree, "")
        } + othersGroups.foldLeft("") { case (s, (_, tree)) =>
          s + md(tree, "")
        }

    lazy val summarySection =
      Section(Some("Software Systems - Summary"), summaryView, "software-systems.md")

    // TODO: Create Software System sections with the following views:
    // - ✅ Info
    //   - Documentation
    //   - ADRs
    // - ✅ Context views
    // - Container views
    //   - Documentation
    //   - ADRs
    // - Dynamic views
    // - Dependencies
    //   - Inbound
    //   - Outbound
    // - Component view
    // - Deployment view
    def sectionHierarchy(): SectionHierarchy =
      def convert(tree: Tree): SectionHierarchy =
        tree match
          case System(group, system @ given SoftwareSystem) =>
            val name = system.getName()
            val sections = Seq.empty ++
              info() ++
              context() ++
              container() ++
              dynamic()
            val default =
              Section(Some(name), "", s"software-systems/${normaliseName(name)}.md")
            if sections.nonEmpty then SectionGroup(Some(name), sections)
            else default

          case Group(name, children) =>
            SectionGroup(
              Some(name),
              children.values.map(convert).toSeq
            )

      // recursively transform systems tree into section hierarchy
      SectionGroup(
        Some("Software Systems"),
        tree.values.map(convert).toSeq
      )

    // TODO: Include System documentation
    def info()(using system: SoftwareSystem): Option[Section] =
      val description =
        Option(system.getDescription())
          .filter(_.trim().nonEmpty)
      description.map { description =>
        val info     = s"#Description\n${description}"
        val fileName = normaliseName(system.getName())
        Section(Some("Info"), info, s"software-systems/${fileName}.md")
      }

    def context()(using system: SoftwareSystem, views: ViewSet): Option[Section] =
      val contextViews = views.getSystemContextViews().asScala.toSeq
      sectionFromViews(system, contextViews, "Context views")

    def container()(using system: SoftwareSystem, views: ViewSet): Option[Section] =
      val containerViews = views.getContainerViews().asScala.toSeq
      sectionFromViews(system, containerViews, "Container views")

    def dynamic()(using system: SoftwareSystem, views: ViewSet): Option[Section] =
      val dynamicViews = views.getDynamicViews().asScala.toSeq
      sectionFromViews(system, dynamicViews, "Dynamic views")

    // TODO: Add description if available
    def sectionFromViews(system: SoftwareSystem, modelViews: Seq[ModelView], name: String) =
      val views    = modelViews.filter(_.getSoftwareSystemId() == system.getId())
      val baseName = normaliseName(system.getName())
      val fileName = s"software-systems/${baseName}-${normaliseName(name)}.md"
      val md = views.foldLeft("") { (s, view) =>
        val svg = s"../img/${view.getKey()}.svg"
        s + s"\n\n<center>![${view.getName()}](${svg})</center>" +
          s"\n<center>${view.getKey()} ([svg](${svg}))</center>"
      }
      Option(md)
        .filter(_.nonEmpty)
        .map(md => Section(Some(name), md, fileName))

    def normaliseName(name: String): String =
      name
        .replaceAll(" ", "-")
        .replaceAll("[^a-zA-Z0-9-]", "")
        .toLowerCase()
