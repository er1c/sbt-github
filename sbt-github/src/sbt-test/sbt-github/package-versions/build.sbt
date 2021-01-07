import sbt.util

import _root_.github.InternalGitHubKeys._

logLevel := util.Level.Debug

lazy val aggregatorIDs = Seq(
  "javaProjectExample",
  "crossPlatformExampleJVM",
  "crossPlatformExampleJS",
  "crossScalaExample",
//  "sbtPluginExample",
)

addCommandAlias("check", ";" + aggregatorIDs.map(id => s"+$id/checkGithubPackageName; +$id/checkGithubPackageVersions").mkString(";"))

// This is largely copy/pasted from https://github.com/er1c/github-packages-tests and then the tests added
// These two should mostly be in sync to allow verifying the correct packages.

lazy val ScalaTestVersion = "3.2.7"

val Scala300 = "3.0.1"
val Scala213 = "2.13.5"
val Scala212 = "2.12.13"
val Scala211 = "2.11.12"
val Scala210 = "2.10.7"

ThisBuild / scalaVersion := Scala212
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"
ThisBuild / publishMavenStyle := true
ThisBuild / githubOwner := Some("er1c")
ThisBuild / githubRepository := "github-packages-tests"
ThisBuild / logLevel := Level.Debug

val checkGithubPackageName = taskKey[Unit]("check githubPackage")
val checkGithubPackageVersions = taskKey[Unit]("check githubPackageVersions")

lazy val crossScalaExample = project.in(file("cross-scala-example"))
  .settings(
    name := "cross-scala-example",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    ),
    crossScalaVersions := Seq(Scala210, Scala211, Scala212, Scala213),//, Scala300),
    checkGithubPackageName := (Def.taskDyn {
      val packageName = githubPackageName.value
      val expectedPackageName = scalaBinaryVersion.value match {
        case "2.10" => "com.example.cross-scala-example_2.10"
        case "2.11" => "com.example.cross-scala-example_2.11"
        case "2.12" => "com.example.cross-scala-example_2.12"
        case "2.13" => "com.example.cross-scala-example_2.13"
      }
      Def.task {
        assert(
          packageName == expectedPackageName,
          s"crossScalaExample / githubPackage $packageName not $expectedPackageName"
        )
      }
    }).value,
    checkGithubPackageVersions := (Def.taskDyn {
      val versions = githubPackageVersions.value
      val expectedVersions = List("0.1.0")
      Def.task {
        assert(
          versions == expectedVersions,
          s"crossScalaExample / githubPackageVersions $versions not $expectedVersions"
        )
        ()
      }
    }).value,
  )

lazy val crossPlatformExample = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("cross-platform-example"))
  .settings(
    name := "cross-platform-example",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % ScalaTestVersion % Test,
    ),
    crossScalaVersions := Seq(Scala211, Scala212, Scala213),
    checkGithubPackageName := (Def.taskDyn {
      val (prefix, suffix) = projectID.value.crossVersion match {
        case b: librarymanagement.Binary => (b.prefix, b.suffix)
      }

      val expectedPackageName = s"com.example.cross-platform-example_${prefix}${scalaBinaryVersion.value}${suffix}"
      val packageName = githubPackageName.value
      Def.task {
        assert(
          packageName == expectedPackageName,
          s"crossPlatformExample / githubPackage $packageName not $expectedPackageName"
        )
      }
    }).value,
    checkGithubPackageVersions := (Def.taskDyn {
      val versions = githubPackageVersions.value
      val expectedVersions = List("0.1.0")
      Def.task {
        assert(
          versions == expectedVersions,
          s"crossPlatformExample / githubPackageVersions $versions not $expectedVersions"
        )
        ()
      }
    }).value,
  )
  .jsSettings(Seq(
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
  ))

lazy val crossPlatformExampleJVM = crossPlatformExample.jvm
lazy val crossPlatformExampleJS = crossPlatformExample.js

lazy val javaProjectExample = (project in file("java-project-example"))
  .settings(
    name := "java-project-example",
    crossPaths := false,
    scalaVersion := Scala212,
    crossScalaVersions := Seq(Scala212),
    checkGithubPackageName := (Def.taskDyn {
      val packageName = githubPackageName.value
      val expectedPackageName = "com.example.java-project-example"
      Def.task {
        assert(
          packageName == expectedPackageName,
          s"javaProjectExample / githubPackage $packageName not $expectedPackageName"
        )
      }
    }).value,
    checkGithubPackageVersions := (Def.taskDyn {
      val versions = githubPackageVersions.value
      val expectedVersions = Seq("0.1.0")
      Def.task {
        assert(
          versions == expectedVersions,
          s"javaProjectExample / githubPackageVersions $versions not $expectedVersions"
        )
      }
    }).value,
  )

//lazy val sbtPluginExample = project.in(file("sbt-plugin-example"))
//  .enablePlugins(SbtPlugin)
//  .settings(
//    name := "sbt-plugin-example",
//    scalaVersion := Scala212,
//    crossScalaVersions := Seq(Scala210, Scala212),
//    pluginCrossBuild / sbtVersion := {
//      scalaBinaryVersion.value match {
//        case "2.10" => "0.13.8"
//        case "2.12" => "1.2.1"
//        case _ => (pluginCrossBuild / sbtVersion).value
//      }
//    },
//    githubRepository := "github-packages-tests",
//    checkGithubPackageName := (Def.taskDyn {
//      val packageName = githubPackageName.value
//      val expectedPackageName = scalaBinaryVersion.value match {
//        case "2.10" => "com.example.sbt-plugin-example_2.10_0.13"
//        case "2.12" => "com.example.sbt-plugin-example_2.12_1.0"
//      }
//      Def.task {
//        assert(
//          packageName == expectedPackageName,
//          s"sbtPluginExample / githubPackage $packageName not $expectedPackageName"
//        )
//      }
//    }).value,
//    checkGithubPackageVersions := (Def.taskDyn {
//      val versions = githubPackageVersions.value
//      val expectedVersions = List("0.1.0")
//      Def.task {
//        assert(
//          versions == expectedVersions,
//          s"sbtPluginExample / githubPackageVersions $versions not $expectedVersions"
//        )
//      }
//    }).value,
//  )

lazy val root = project.in(file("."))
  .aggregate(
    crossPlatformExampleJVM,
    crossPlatformExampleJS,
    crossScalaExample,
    javaProjectExample,
//    sbtPluginExample,
  )
  .settings(publish / skip := false)