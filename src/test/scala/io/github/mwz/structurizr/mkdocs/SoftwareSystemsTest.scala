package io.github.mwz.structurizr.mkdocs

import com.structurizr.Workspace as SWorkspace
import com.structurizr.model.SoftwareSystem
import munit.FunSuite

import scala.collection.immutable.TreeMap

case class TestSystem(name: String, group: String = "")

def createWorkspace(systems: List[TestSystem]): (SWorkspace, List[SoftwareSystem]) =
  val workspace = new SWorkspace("test", "")
  workspace.getModel().addProperty("structurizr.groupSeparator", "/")
  val sys = systems.map { system =>
    val sys = workspace.getModel().addSoftwareSystem(system.name, "")
    sys.setGroup(system.group)
    sys
  }
  (workspace, sys)

class SoftwareSystemsTest extends FunSuite:
  test("return systems with groups"):
    val (workspace, systems @ List(system1, system2, system3, system4)) =
      createWorkspace(
        List(
          TestSystem("system1", "group1"),
          TestSystem("system2"),
          TestSystem("system3", "group1/group2"),
          TestSystem("system4", "group3")
        )
      )
    assertEquals(
      SoftwareSystems(systems, null, Set(), "/").allSystems(),
      List(
        System(List("group1"), system1),
        System(List.empty, system2),
        System(List("group1", "group2"), system3),
        System(List("group3"), system4)
      )
    )

  test("create simple system tree"):
    val (workspace, systems @ List(system1, system2, system3, system4)) =
      createWorkspace(
        List(
          TestSystem("system1", "group1"),
          TestSystem("system2"),
          TestSystem("system3", "group2"),
          TestSystem("system4", "group3")
        )
      )
    assertEquals(
      SoftwareSystems(systems, null, Set(), "/").tree(),
      TreeMap(
        "group1" -> Group(
          "group1",
          TreeMap(
            "system1" -> System(List("group1"), system1)
          )
        ),
        "system2" -> System(List.empty, system2),
        "group2" -> Group(
          "group2",
          TreeMap(
            "system3" -> System(List("group2"), system3)
          )
        ),
        "group3" -> Group(
          "group3",
          TreeMap(
            "system4" -> System(List("group3"), system4)
          )
        )
      )
    )

  test("create nested system tree"):
    val (workspace, systems @ List(system1, system2, system3, system4)) =
      createWorkspace(
        List(
          TestSystem("system1", "group1"),
          TestSystem("system2"),
          TestSystem("system3", "group1/group2"),
          TestSystem("system4", "group3")
        )
      )
    assertEquals(
      SoftwareSystems(systems, null, Set(), "/").tree(),
      TreeMap(
        "group1" -> Group(
          "group1",
          TreeMap(
            "system1" -> System(List("group1"), system1),
            "group2" -> Group(
              "group2",
              TreeMap(
                "system3" -> System(List("group1", "group2"), system3)
              )
            )
          )
        ),
        "system2" -> System(List.empty, system2),
        "group3" -> Group(
          "group3",
          TreeMap(
            "system4" -> System(List("group3"), system4)
          )
        )
      )
    )

  test("create basic section hierarchy"):
    val (workspace, systems @ List(system1, system2, system3, system4)) =
      createWorkspace(
        List(
          TestSystem("system1", "group1"),
          TestSystem("system2"),
          TestSystem("system3", "group1/group2"),
          TestSystem("system4", "group3")
        )
      )
    assertEquals(
      SoftwareSystems(systems, workspace.getViews(), Set(), "/").sectionHierarchy(),
      SectionGroup(
        Some("Software Systems"),
        List(
          SectionGroup(
            Some("group1"),
            List(
              SectionGroup(Some("group2"), List(Section(Some("system3"), "", "software-systems/system3.md"))),
              Section(Some("system1"), "", "software-systems/system1.md")
            )
          ),
          SectionGroup(Some("group3"), List(Section(Some("system4"), "", "software-systems/system4.md"))),
          Section(Some("system2"), "", "software-systems/system2.md")
        )
      )
    )
