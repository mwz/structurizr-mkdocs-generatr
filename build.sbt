ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file(".")).settings(
  name := "structurizr-mkdocs-generatr",
  libraryDependencies ++= Seq(
    "io.circe"       %% "circe-core"         % "0.14.6",
    "io.circe"       %% "circe-parser"       % "0.14.6",
    "io.circe"       %% "circe-yaml-v12"     % "0.15.1",
    "org.typelevel"  %% "toolkit"            % "0.1.22",
    "com.structurizr" % "structurizr-core"   % "1.29.0",
    "com.structurizr" % "structurizr-dsl"    % "1.35.0",
    "com.structurizr" % "structurizr-export" % "1.19.0",
    "org.typelevel"  %% "toolkit-test"       % "0.1.22" % Test
  )
)
