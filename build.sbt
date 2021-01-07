lazy val unusedWarnings = Seq("-Ywarn-unused-import", "-Ywarn-unused")

ThisBuild / organization := "com.github.er1c"
ThisBuild / homepage     := Some(url("https://github.com/er1c/sbt-github"))
ThisBuild / licenses     := Seq("MIT" ->
  url(s"https://github.com/er1c/${name.value}/blob/${version.value}/LICENSE"))
ThisBuild / description  := "package publisher for github.com"
ThisBuild / developers   := List(
  Developer("softprops", "Doug Tangren", "@softprops", url("https://github.com/softprops"))
)
ThisBuild / scmInfo      := Some(ScmInfo(url(s"https://github.com/er1c/${name.value}"), s"git@github.com:sbt/${name.value}.git"))
ThisBuild / scalaVersion := "2.12.12"

ThisBuild / githubOrganization := Some("er1c")
ThisBuild / githubRepository := "sbt-github"
ThisBuild / githubPackage := "sbt-github"

lazy val commonSettings: Seq[Setting[_]] = Seq(
    scalacOptions ++= Seq(Opts.compile.deprecation, "-Xlint", "-feature"),
    scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
      case Some((2, v)) if v >= 11 => unusedWarnings
    }.toList.flatten,
    publishArtifact in Test := false,

//    bintrayRepository := "sbt-plugin-releases",
//    bintrayOrganization := Some("sbt"),
//    bintrayPackage := "sbt-github",
    scriptedBufferLog := true,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-XX:MaxPermSize=256M",
      "-Dgithub.user=username",
      "-Dgithub.pass=password",
      "-Dplugin.version=" + version.value
    ),
  ) ++ Seq(Compile, Test).flatMap(c =>
    scalacOptions in (c, console) --= unusedWarnings
  )

lazy val root = (project in file("."))
  .aggregate(core, sbtGitHub, sbtGitHubRemoteCache)
  .settings(
    publish / skip := true,
  )

lazy val core = (project in file("core"))
  .enablePlugins(SbtPlugin)
  .settings(commonSettings)
  .settings(
    name := "sbt-github-core",
    libraryDependencies ++= Seq(
      "org.foundweekends" %% "bintry" % "0.6.0",
      "org.slf4j" % "slf4j-nop" % "1.7.28", // https://github.com/sbt/sbt-bintray/issues/26
      "com.eed3si9n.verify" %% "verify" % "0.2.0" % Test,
    ),
    testFrameworks += new TestFramework("verify.runner.Framework"),
    resolvers += Resolver.sonatypeRepo("releases"),
  )

lazy val sbtGitHub = (project in file("sbt-github"))
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "sbt-github",
    pluginCrossBuild / sbtVersion := "1.0.0",
  )

lazy val sbtGitHubRemoteCache = (project in file("sbt-github-remote-cache"))
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "sbt-github-remote-cache",
    pluginCrossBuild / sbtVersion := "1.4.2",
  )
