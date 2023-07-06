ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "zio-http-poc",
    libraryDependencies ++= Seq(
      "dev.zio"                     %% "zio"                           % "2.0.13",
      "com.github.ghostdogpr"       %% "caliban"                       % "2.2.1",
      "com.github.ghostdogpr"       %% "caliban-zio-http"              % "2.2.1",
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio"                % "1.2.11",
      "com.github.ghostdogpr"       %% "caliban-tracing"               % "2.2.1",
      "io.opentelemetry"             % "opentelemetry-sdk"             % "1.26.0",
      "io.opentelemetry"             % "opentelemetry-exporter-jaeger" % "1.26.0"
    )
  )
