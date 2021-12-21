import Dependencies.Library

val scalacOpts = Seq(
    "-language:postfixOps",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Ymacro-annotations",
)

val commonSettings = Seq(
    scalaVersion := "2.13.7",
    organization := "example",
    scalacOptions ++= scalacOpts,
)

lazy val fs2v2 = (project in file("fs2-v2"))
  .settings(commonSettings)
  .settings(
      name       := "fs2-v2",
      moduleName := "fs2-v2",
      libraryDependencies ++= Library.Fs2V2,
  )
  .enablePlugins(TpolecatPlugin)

lazy val fs2v3 = (project in file("fs2-v3"))
  .settings(commonSettings)
  .settings(
      name       := "fs2-v3",
      moduleName := "fs2-v3",
      libraryDependencies ++= Library.Fs2V3,
  )
  .enablePlugins(TpolecatPlugin)

lazy val root = (project in file("."))
  .aggregate(fs2v2, fs2v3)
