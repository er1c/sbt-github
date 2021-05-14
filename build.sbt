lazy val unusedWarnings = Seq("-Ywarn-unused-import", "-Ywarn-unused")

ThisBuild / organization        := "io.github.er1c"
ThisBuild / homepage            := Some(url("https://github.com/er1c/sbt-github"))
ThisBuild / licenses            := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / description         := "package publisher for github.com"
ThisBuild / developers          := List(Developer(id = "ericpeters", name = "Eric Peters", email = "eric@peters.org", url("https://github.com/er1c")))
ThisBuild / scmInfo             := Some(ScmInfo(url(s"https://github.com/er1c/${name.value}"), s"git@github.com:sbt/${name.value}.git"))
ThisBuild / scalaVersion        := "2.12.12"
ThisBuild / githubWorkflowOSes  := Seq("ubuntu-latest", "macos-latest", "windows-latest")
ThisBuild / githubWorkflowEnv   := Map.empty // ignore the default GITHUB_TOKEN for now
ThisBuild / githubWorkflowBuild := Seq(
  // Using credentials files to allow individual tests to use different information, this is the default
  WorkflowStep.Run(
    commands = {
      List(
        """|mkdir ~/.github && echo "
           |realm = GitHub API Realm
           |host = api.github.com
           |user = $GITHUB_USER
           |password = $GITHUB_TOKEN
           |" >> ~/.github/.credentials
           |""".stripMargin)
    },
    env = Map(
      "GITHUB_USER" -> "${{ secrets.GITHUB_USER }}",
      "GITHUB_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}",
    ),
  ),
  WorkflowStep.Sbt(
    commands = List("test", "scripted"),
    env = Map.empty,
  )
)

// dummy publication just to test that setup works
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("main")))
ThisBuild / githubWorkflowPublish := Seq()

//ThisBuild / githubWorkflowPublishPreamble := Seq(WorkflowStep.Run(
//  List("git config user.name \"Github Actions (dimitarg/bb-webhook)\"")
//))



//ThisBuild / githubWorkflowPublishPreamble += WorkflowStep.Use("olafurpg", "setup-gpg", "v3"),
//ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
//ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
//ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(
//  List("ci-release"),
//  env = Map(
//    "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
//    "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
//    "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
//    "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
//  )
//))

// Update ci via `sbt githubWorkflowGenerate`

val CalibanVersion = "0.9.5"
val SttpVersion = "3.2.0"

lazy val commonSettings: Seq[Setting[_]] = Seq(
  scalacOptions ++= Seq(Opts.compile.deprecation, "-Xlint", "-feature"),
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
    case Some((2, v)) if v >= 11 => unusedWarnings
  }.toList.flatten,
  libraryDependencies ++= Seq(
    "io.github.er1c" %% "caliban-github-api-client" % "0.9.5-2",
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % SttpVersion, // async-http-client-backend-zio
    //"org.asynchttpclient" % "async-http-client" % "2.12.2",
    "com.github.ghostdogpr" %% "caliban-http4s" % CalibanVersion,
    "com.eed3si9n.verify" %% "verify" % "0.2.0" % Test,
  ),

  publishArtifact in Test := false,
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-XX:MaxPermSize=256M",
//      s"-Dgithub.user=${sys.env.get("GITHUB_USER").getOrElse("username")}",
//      s"-Dgithub.token=${sys.env.get("GITHUB_TOKEN").getOrElse("password")}",
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
      "io.github.er1c" %% "caliban-github-api-client" % "0.9.5-1",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend" % SttpVersion, // async-http-client-backend-zio
      //"org.asynchttpclient" % "async-http-client" % "2.12.2",
      "com.github.ghostdogpr" %% "caliban-http4s" % CalibanVersion,
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
