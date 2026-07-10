ThisBuild / organization      := "io.github.mwz"
ThisBuild / scalaVersion      := "3.7.4"
ThisBuild / semanticdbEnabled := true

// sbt github packages plugin settings
ThisBuild / githubOwner      := "mwz"
ThisBuild / githubRepository := "structurizr-mkdocs-generatr"
ThisBuild / versionScheme    := Some("semver-spec")

lazy val root = (project in file("."))
  .enablePlugins(AssemblyPlugin)
  .settings(
    name                             := "structurizr-mkdocs-generatr",
    crossPaths                       := false,
    assembly / mainClass             := Some("io.github.mwz.structurizr.mkdocs.Main"),
    assembly / assemblyJarName       := "structurizr-mkdocs-generatr.jar",
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
      "com.github.alexarchambault" %% "case-app"           % "2.1.0",
      "com.github.alexarchambault" %% "case-app-cats"      % "2.1.0",
      "io.circe"                   %% "circe-core"         % "0.14.16",
      "io.circe"                   %% "circe-parser"       % "0.14.16",
      "io.circe"                   %% "circe-yaml-v12"     % "0.16.1",
      "org.typelevel"              %% "mouse"              % "1.4.0",
      "org.typelevel"              %% "toolkit"            % "0.1.30",
      "com.structurizr"             % "structurizr-core"   % "6.2.2",
      "com.structurizr"             % "structurizr-dsl"    % "6.2.2" exclude ("com.google.guava", "guava"),
      "com.structurizr"             % "structurizr-export" % "6.2.2",
      "net.sourceforge.plantuml"    % "plantuml"           % "1.2025.10" exclude ("com.google.guava", "guava"),
      "org.codehaus.groovy"         % "groovy-jsr223"      % "3.0.25" % Runtime,
      "org.typelevel"              %% "toolkit-test"       % "0.1.30" % Test,
      "org.typelevel"              %% "munit-cats-effect"  % "2.2.0"  % Test
    )
  )
