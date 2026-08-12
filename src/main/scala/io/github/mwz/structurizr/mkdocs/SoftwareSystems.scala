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

import io.github.mwz.structurizr.mkdocs.markdown.imageView
import com.structurizr.model.Container
import com.structurizr.model.Element
import com.structurizr.model.Relationship
import com.structurizr.model.SoftwareSystem
import com.structurizr.view.ImageView
import com.structurizr.view.ModelView
import com.structurizr.view.ViewSet
import mouse.string.*

import java.net.URL
import scala.annotation.tailrec
import scala.collection.immutable.TreeMap
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

import markdown.imgTabs
import markdown.escape

sealed trait Tree
case class System(groups: List[String], system: SoftwareSystem) extends Tree
case class Group(name: String, children: TreeMap[String, Tree]) extends Tree

trait SoftwareSystems:
  def allSystems(): Seq[System]
  def tree(): TreeMap[String, Tree]
  def nameClashes(): Set[String]
  def prettyPrint(): String
  def summaryView(): String
  def summarySection(): Section
  def sectionHierarchy(): SectionHierarchy

object SoftwareSystems:
  def apply(
      systems: Seq[SoftwareSystem],
      views: ViewSet,
      relationships: Set[Relationship],
      separator: String
  ) = new SoftwareSystems:
    given viewSet: ViewSet = views
    given rels: Set[Relationship] = relationships

    def allSystems(): Seq[System] =
      systems.map { system =>
        system.getGroup() match {
          case null  => System(List.empty, system)
          case group => System(group.split(separator).toList, system)
        }
      }

    // The (immutable) input to the tree build: each system paired with its group
    // path. Memoised and shared by the tree and clash computations below.
    private lazy val systemEntries: List[(List[String], System)] =
      allSystems().map(sys => (sys.groups, sys)).toList

    // Memoised: tree() is consumed once by the summary view and once by the
    // section hierarchy, so build it a single time per instance.
    private lazy val treeMemo: TreeMap[String, Tree] = buildTree(systemEntries)

    def tree(): TreeMap[String, Tree] = treeMemo

    // Names that are both a software system and a group at the same level.
    // On such a clash buildTree keeps the group and drops the software system;
    // nameClashes surfaces those drops so the caller can warn (never fatal).
    private lazy val nameClashesMemo: Set[String] = clashingNames(systemEntries)

    def nameClashes(): Set[String] = nameClashesMemo

    // pretty print tree with indentation
    def prettyPrint(): String =
      def prettyPrint(tree: Tree, indent: String = ""): String =
        tree match {
          case System(_, system) =>
            s"\n${indent}- ${system.getName()}"
          case Group(name, children) =>
            s"\n${indent}- ${name}${children.foldLeft("") { (s, c) =>
                s + prettyPrint(c._2, indent + "  ")
              }}"
        }
      tree().foldLeft("") { case (s, (_, tree)) =>
        s + prettyPrint(tree)
      }

    /**
     * Build the group tree by recursively grouping systems on the head of their remaining group
     * path. Systems whose remaining path is empty become leaf `System` nodes (retaining their full
     * original `groups`); the rest are grouped under a `Group` and recursed into.
     *
     * Collision rule: if a group name and a system name coincide at the same level, the group wins
     * and the software system is dropped.
     *
     * Recursion depth == group-nesting depth (typically 1–3), never the number of systems — those
     * are folded on the heap via partition/groupBy/map. `convert`, `summaryView`'s `md`, and the
     * Circe SectionHierarchy encoder in MkDocs recurse to the same bounded depth, so a trampoline
     * here would add no real protection.
     */
    private def buildTree(entries: List[(List[String], System)]): TreeMap[String, Tree] =
      val (leaves, nested) = entries.partition((path, _) => path.isEmpty)
      val leafMap: TreeMap[String, Tree] =
        TreeMap.from(leaves.map((_, sys) => sys.system.getName() -> sys))
      val groupMap: TreeMap[String, Tree] =
        TreeMap.from(nested.groupBy((path, _) => path.head).map { (name, members) =>
          name -> Group(name, buildTree(members.map((path, sys) => (path.tail, sys))))
        })
      leafMap ++ groupMap

    /**
     * Names that are both a software system and a group at the same level — the collisions
     * buildTree resolves by dropping the software system. Kept structurally parallel to buildTree's
     * partition/groupBy so the reported names match exactly what buildTree omits.
     */
    private def clashingNames(entries: List[(List[String], System)]): Set[String] =
      val (leaves, nested) = entries.partition((path, _) => path.isEmpty)
      val leafNames = leaves.map((_, sys) => sys.system.getName()).toSet
      val groupNames = nested.map((path, _) => path.head).toSet
      val deeper = nested.groupBy((path, _) => path.head).values.flatMap { members =>
        clashingNames(members.map((path, sys) => (path.tail, sys)))
      }
      (leafNames intersect groupNames) ++ deeper

    // A summary view of all software systems
    def summaryView(): String =
      val (groups, others) = tree().partition((_, t) => t.isInstanceOf[Group])
      val hasGroups = groups.nonEmpty
      val othersGroups =
        if (!hasGroups) others
        else TreeMap(("Others" -> Group("Others", others)))

      def indent(level: Int): String =
        if (hasGroups) "  " * math.max(0, level - 1)
        else "  " * level

      def md(tree: Tree, level: Int = 0): String =
        tree match
          case System(_, system) =>
            val description = system
              .getDescription()
              .toOption
              .fold("")(d => s"  \n${indent(level)}  `${d}`")
            val link = s"[${system.getName()}](${normaliseName(system.getName())})"
            s"\n${indent(level)}- ${link}${description}"
          case Group(name, children) =>
            val prefix = level match
              case 0 => s"##"
              case l => indent(l) + "-"
            s"\n${prefix} ${name}${children.foldLeft("") { case (s, (_, children)) =>
                s + md(children, level + 1)
              }}"

      val fold: (String, (String, Tree)) => String =
        case (s, (_, children)) => s + md(children)

      "# Software Systems\n" +
        groups.foldLeft("")(fold) +
        othersGroups.foldLeft("")(fold)

    def summarySection(): Section =
      Section(Some("Software Systems - Summary"), summaryView(), "software-systems.md")

    // TODO: Create Software System sections with the following views:
    // - About:
    //   - Documentation
    //   - ADRs
    // - Container views:
    //   - Documentation
    //   - ADRs
    def sectionHierarchy(): SectionHierarchy =
      def convert(tree: Tree): SectionHierarchy =
        tree match
          case System(_, system @ given SoftwareSystem) =>
            val name = system.getName()
            val sections: Seq[Section] =
              Seq.empty ++
                context() ++
                container() ++
                component() ++
                dynamic() ++
                deployment() ++
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
      SectionGroup(Some("Software Systems"), tree().values.map(convert).toSeq)

    def about()(using system: SoftwareSystem): Section =
      val description = system.getDescription().toOption
      val fileName = normaliseName(system.getName())
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
      sectionFromViews("Context views", contextViews)

    def container()(using system: SoftwareSystem, views: ViewSet): Option[Section] =
      val containerViews = views.getContainerViews().asScala.toSeq
      val imgViews =
        views
          .getImageViews()
          .asScala
          .toSeq
          .filter(i => isSystemMemberOfType[SoftwareSystem](i.getElement()))
      sectionFromViews("Container views", containerViews, imgViews)

    def component()(using system: SoftwareSystem, views: ViewSet): Option[Section] =
      val componentViews = views.getComponentViews().asScala.toSeq
      val imgViews =
        views
          .getImageViews()
          .asScala
          .toSeq
          .filter(i => isSystemMemberOfType[Container](i.getElement()))
      sectionFromViews("Component views", componentViews, imgViews)

    def dynamic()(using system: SoftwareSystem, views: ViewSet): Option[Section] =
      val dynamicViews = views.getDynamicViews().asScala.toSeq
      sectionFromViews("Dynamic views", dynamicViews)

    def deployment()(using system: SoftwareSystem, views: ViewSet): Option[Section] =
      val deploymentViews = views.getDeploymentViews().asScala.toSeq
      sectionFromViews("Deployment views", deploymentViews)

    def sectionFromViews(using system: SoftwareSystem)(
        name: String,
        modelViews: Seq[ModelView],
        imageViews: Seq[ImageView] = Seq.empty
    ): Option[Section] =
      val views: Seq[ModelView] = modelViews.filter(_.getSoftwareSystemId() == system.getId())
      val baseName = normaliseName(system.getName())
      val fileName = s"software-systems/${baseName}-${normaliseName(name)}.md"
      val md: String = views.foldLeft("") { (s, view) =>
        s + imgTabs(view.getKey(), view.getTitle().toOption, view.getDescription().toOption)
      }
      val imgMd = imageViews.foldLeft("") { _ + imageView(_) }
      Option(md + imgMd)
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
            val link = s"../${normaliseName(system.getName())}"
            val desc = Option(r.getDescription()).map(escape).getOrElse("")
            val tech = Option(r.getTechnology()).map(escape).getOrElse("")
            s + s"\n| [${escape(system.getName())}](${link}) | ${desc} | ${tech} |"
          }
          Some(md)

      val inMd = makeMd("## Inbound", in, _.getSource())
      val outMd = makeMd("## Outbound", out, _.getDestination())

      val combined =
        (inMd, outMd) match
          case (Some(in), Some(out)) => Some(s"${in}\n\n${out}")
          case (Some(in), None)      => Some(in)
          case (None, Some(out))     => Some(out)
          case _                     => None

      combined.map { md =>
        Section(Some("Dependencies"), md, s"software-systems/${normaliseName(system.getName())}-dependencies.md")
      }

  // Check if an Element is of a given type and a member of the given Software System.
  @tailrec
  def isSystemMemberOfType[T <: Element: ClassTag](using system: SoftwareSystem)(element: Element): Boolean =
    val classOfT = implicitly[ClassTag[T]].runtimeClass
    element match
      case sys: SoftwareSystem =>
        classOfT == classOf[SoftwareSystem] && sys == system
      case container: Container =>
        classOfT == classOf[Container] && isSystemMemberOfType[SoftwareSystem](container.getSoftwareSystem())
      // TODO: scope to implement Component check if needed
      case _ => false

  def normaliseName(name: String): String =
    name
      .replaceAll(" ", "-")
      .replaceAll("[^a-zA-Z0-9-]", "")
      .toLowerCase()
