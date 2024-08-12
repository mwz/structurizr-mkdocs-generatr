ThisBuild / organization := "io.github.mwz"
ThisBuild / scalaVersion := "3.4.2"

// sbt github packages plugin settings
ThisBuild / githubOwner      := "mwz"
ThisBuild / githubRepository := "structurizr-mkdocs-generatr"

lazy val root = (project in file("."))
  .enablePlugins(AssemblyPlugin)
  .settings(
    name                       := "structurizr-mkdocs-generatr",
    crossPaths                 := false,
    assembly / mainClass       := Some("io.github.mwz.structurizr.mkdocs.Main"),
    assembly / assemblyJarName := "structurizr-mkdocs-generatr.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        MergeStrategy.discard
      case x =>
        MergeStrategy.first
    },
    Compile / run / fork := true,
    Compile / packageBin := assembly.value,
    javacOptions ++= Seq("-source", "21", "-target", "21"),
    libraryDependencies ++= Seq(
      "com.github.alexarchambault" %% "case-app"           % "2.1.0-M29",
      "com.github.alexarchambault" %% "case-app-cats"      % "2.1.0-M29",
      "io.circe"                   %% "circe-core"         % "0.14.9",
      "io.circe"                   %% "circe-parser"       % "0.14.9",
      "io.circe"                   %% "circe-yaml-v12"     % "0.15.3",
      "org.typelevel"              %% "mouse"              % "1.3.2",
      "org.typelevel"              %% "toolkit"            % "0.1.27",
      "com.structurizr"             % "structurizr-core"   % "1.29.0",
      "com.structurizr"             % "structurizr-dsl"    % "1.35.0",
      "com.structurizr"             % "structurizr-export" % "1.19.0",
      "net.sourceforge.plantuml"    % "plantuml"           % "1.2024.6",
      "org.typelevel"              %% "toolkit-test"       % "0.1.27" % Test,
      "org.typelevel"              %% "munit-cats-effect"  % "2.0.0"  % Test
    )
  )
