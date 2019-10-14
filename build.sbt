name := "minidi"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.specs2" %% "specs2-core" % "4.0.2" % "test"
)

scalacOptions in Test ++= Seq("-Yrangepos") //todo: remove
