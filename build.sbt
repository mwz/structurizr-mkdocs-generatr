import sbtlicensereport.license.DepModuleInfo

ThisBuild / organization := "io.github.mwz"
ThisBuild / scalaVersion := "3.7.0"

ThisBuild / versionScheme := Some("semver-spec")

// Open-source / POM metadata
ThisBuild / organizationName := "Michael Wizner"
ThisBuild / startYear := Some(2024)
ThisBuild / homepage := Some(url("https://github.com/mwz/structurizr-mkdocs-generatr"))
ThisBuild / licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / description :=
  "Static MkDocs site generator for C4 architecture model documentation created with Structurizr DSL"
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/mwz/structurizr-mkdocs-generatr"),
    "scm:git:https://github.com/mwz/structurizr-mkdocs-generatr.git",
    "scm:git:git@github.com:mwz/structurizr-mkdocs-generatr.git"
  )
)
ThisBuild / developers := List(
  Developer("mwz", "Michael Wizner", "mwz@users.noreply.github.com", url("https://github.com/mwz"))
)
ThisBuild / headerEndYear := Some(2026)
ThisBuild / headerLicense := Some(
  HeaderLicense.ALv2(s"${startYear.value.get}-${headerEndYear.value.get}", "Michael Wizner and contributors")
)

// Dependency license check over the scopes bundled into the fat jar
ThisBuild / licenseConfigurations := Set("compile", "runtime")
ThisBuild / licenseCheckExclusions := {
  // Dual-licensed CDDL 1.1 / GPLv2 with Classpath Exception, used under CDDL 1.1.
  // Pulled in transitively by structurizr-dsl -> structurizr-client.
  case DepModuleInfo("javax.activation", "javax.activation-api", _) => true
  case DepModuleInfo("javax.xml.bind", "jaxb-api", _)               => true
}

lazy val root = (project in file("."))
  .enablePlugins(AssemblyPlugin, HeaderPlugin)
  .settings(
    name                       := "structurizr-mkdocs-generatr",
    crossPaths                 := false,
    assembly / mainClass       := Some("io.github.mwz.structurizr.mkdocs.Main"),
    assembly / assemblyJarName := "structurizr-mkdocs-generatr.jar",
    Compile / resourceGenerators += Def.task {
      val resourceDirectory = (Compile / resourceManaged).value / "META-INF"
      val projectDirectory  = baseDirectory.value
      Seq("LICENSE", "NOTICE").map { name =>
        val destination = resourceDirectory / name
        IO.copyFile(projectDirectory / name, destination)
        destination
      }
    }.taskValue,
    Compile / packageDoc / mappings ++= Seq(
      (baseDirectory.value / "LICENSE") -> "META-INF/LICENSE",
      (baseDirectory.value / "NOTICE") -> "META-INF/NOTICE"
    ),
    assembly / assemblyMergeStrategy := {
      case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
        MergeStrategy.rename
      case x
          if x.endsWith("plugin.properties") || x.endsWith("module-info.class") ||
            x.endsWith("plugin.xml") =>
        MergeStrategy.discard
      case PathList("META-INF", xs @ _*) =>
        xs.map(_.toLowerCase) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) | ("plugin.xml" :: Nil) |
              ("eclipse_.sf" :: Nil) | ("eclipse_.rsa" :: Nil) =>
            MergeStrategy.discard
          case ps @ (x :: xs) if ps.last.contains("module-info.class") =>
            MergeStrategy.discard
          case "services" :: xs =>
            MergeStrategy.filterDistinctLines
          case _ =>
            MergeStrategy.deduplicate
        }
      case x =>
        val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    Compile / run / fork := true,
    Compile / packageBin := assembly.value,
    javacOptions ++= Seq("-source", "21", "-target", "21"),
    libraryDependencies ++= Seq(
      "com.github.alexarchambault" %% "case-app"           % "2.1.0-M30",
      "com.github.alexarchambault" %% "case-app-cats"      % "2.1.0-M30",
      "io.circe"                   %% "circe-core"         % "0.14.13",
      "io.circe"                   %% "circe-parser"       % "0.14.13",
      "io.circe"                   %% "circe-yaml-v12"     % "0.16.0",
      "org.typelevel"              %% "mouse"              % "1.3.2",
      "org.typelevel"              %% "toolkit"            % "0.1.29",
      "com.structurizr"             % "structurizr-core"   % "4.0.0",
      "com.structurizr"             % "structurizr-dsl"    % "4.0.0" exclude ("com.google.guava", "guava"),
      "com.structurizr"             % "structurizr-export" % "4.0.0",
      "net.sourceforge.plantuml"    % "plantuml-mit"       % "1.2025.2" exclude ("com.google.guava", "guava"),
      "org.codehaus.groovy"         % "groovy-jsr223"      % "3.0.24" % Runtime,
      "org.typelevel"              %% "toolkit-test"       % "0.1.29" % Test,
      "org.typelevel"              %% "munit-cats-effect"  % "2.1.0"  % Test
    )
  )
