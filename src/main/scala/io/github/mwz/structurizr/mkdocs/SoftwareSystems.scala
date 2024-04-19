package io.github.mwz.structurizr.mkdocs

import cats.syntax.contravariantSemigroupal.*
import com.structurizr.model.Element
import com.structurizr.model.Relationship
import com.structurizr.model.SoftwareSystem
import com.structurizr.view.ModelView
import com.structurizr.view.ViewSet
import mouse.string.*

import java.net.URL
import java.nio.file.Path
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters.*

import markdown.imgTabs
import markdown.escape

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
  def apply(
      systems: Seq[SoftwareSystem],
      views: ViewSet,
      relationships: Set[Relationship],
      basePath: Path,
      separator: String
  ) = new SoftwareSystems:
    given viewSet: ViewSet        = views
    given rels: Set[Relationship] = relationships

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
            val link = s"[${system.getName()}](${normaliseName(system.getName())})"
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
    // - About:
    //   - Documentation
    //   - ADRs
    // - Container views:
    //   - Documentation
    //   - ADRs
    // - Component views
    // - Deployment views
    def sectionHierarchy(): SectionHierarchy =
      def convert(tree: Tree): SectionHierarchy =
        tree match
          case System(group, system @ given SoftwareSystem) =>
            val name = system.getName()
            val sections: Seq[Section] =
              Seq.empty ++
                context() ++
                container() ++
                dynamic() ++
                dependencies()

            val default =
              Section(Some(name), "", s"software-systems/${normaliseName(name)}.md")

            // create a group if there are sections & add info section,
            // otherwise just add an empty section
            if sections.nonEmpty then SectionGroup(Some(name), about() +: sections)
            else default

          case Group(name, children) =>
            SectionGroup(
              Some(name),
              children.values.map(convert).toSeq
            )

      // recursively transform systems tree into section hierarchy
      SectionGroup(Some("Software Systems"), tree.values.map(convert).toSeq)

    def about()(using system: SoftwareSystem): Section =
      val description      = Option(system.getDescription()).filter(_.trim().nonEmpty)
      val fileName         = normaliseName(system.getName())
      val url: Option[URL] = system.getUrl().parseURLOption
      val properties: Map[String, String] =
        system
          .getProperties()
          .asScala
          .filterNot((k, _) => k == "structurizr.dsl.identifier")
          .toMap
      val allProperties = url.fold(properties) { url =>
        properties + ("url" -> url.toString())
      }

      val propertiesMd: String = allProperties.foldLeft("\n") { case (s, (k, v)) =>
        val parsed = v.parseURLOption.fold(s"`${v}`")(url => s"[${v}](${url})")
        s + s"\n- `${k}`: ${parsed}"
      }
      val content: String =
        description
          .map(desc => s"#About\n${desc}")
          .getOrElse("") + propertiesMd

      Section(Some("About"), content, s"software-systems/${fileName}.md")

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
    def sectionFromViews(system: SoftwareSystem, modelViews: Seq[ModelView], name: String): Option[Section] =
      val views: Seq[ModelView] = modelViews.filter(_.getSoftwareSystemId() == system.getId())
      val baseName              = normaliseName(system.getName())
      val fileName              = s"software-systems/${baseName}-${normaliseName(name)}.md"
      val md: String = views.foldLeft("") { (s, view) =>
        s + imgTabs(view.getKey())
      }
      Option(md)
        .filter(_.nonEmpty)
        .map(md => Section(Some(name), md, fileName))

    def dependencies()(using system: SoftwareSystem, relationships: Set[Relationship]): Option[Section] =
      val inbound: Set[Relationship] =
        (relationships ++ system.getRelationships().asScala.toSet)
          .filter(r =>
            r.getSource().isInstanceOf[SoftwareSystem] &&
              r.getDestination().getId() == system.getId()
          )
      val in: Seq[Relationship] = inbound.toSeq.sortBy(_.getSource().getName())

      val outbound: Set[Relationship] =
        (relationships ++ system.getRelationships().asScala.toSet)
          .filter(r =>
            r.getDestination().isInstanceOf[SoftwareSystem] &&
              r.getSource().getId() == system.getId()
          )
      val out: Seq[Relationship] = outbound.toSeq.sortBy(_.getDestination().getName())

      val header = """
      \| System | Description | Technology |
      \| ------ | ----------- | ---------- |
      """.stripMargin('\\').trim()

      def makeMd(
          heading: String,
          relationships: Seq[Relationship],
          getSystem: Relationship => Element
      ): Option[String] =
        if relationships.isEmpty then None
        else
          val md = relationships.foldLeft(heading + "\n" + header) { (s, r) =>
            val system = getSystem(r)
            val link   = s"../${normaliseName(system.getName())}"
            val desc   = Option(r.getDescription()).map(escape).getOrElse("")
            val tech   = Option(r.getTechnology()).map(escape).getOrElse("")
            s + s"\n| [${escape(system.getName())}](${link}) | ${desc} | ${tech} |"
          }
          Some(md)

      val inMd  = makeMd("## Inbound", in, _.getSource())
      val outMd = makeMd("## Outbound", out, _.getDestination())

      (inMd, outMd)
        .mapN { (inMd, outMd) => s"${inMd}\n\n${outMd}" }
        .map { md =>
          Section(Some("Dependencies"), md, s"software-systems/${normaliseName(system.getName())}-dependencies.md")
        }

  def normaliseName(name: String): String =
    name
      .replaceAll(" ", "-")
      .replaceAll("[^a-zA-Z0-9-]", "")
      .toLowerCase()
