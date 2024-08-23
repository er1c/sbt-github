lazy val unusedWarnings = Seq("-Ywarn-unused-import", "-Ywarn-unused")

ThisBuild / organization := "io.github.er1c"
ThisBuild / homepage     := Some(url("https://github.com/er1c/sbt-github"))
ThisBuild / licenses     := Seq("MIT" ->
  url(s"https://github.com/er1c/${name.value}/blob/${version.value}/LICENSE"))
ThisBuild / description  := "package publisher for github.com"
ThisBuild / developers   := List(
  Developer("ericpeters", "Eric Peters", "eric@peters.org", url("https://github.com/er1c"))
)
ThisBuild / scmInfo            := Some(ScmInfo(url(s"https://github.com/er1c/sbt-github"), s"git@github.com:er1c/sbt-github.git"))
ThisBuild / scalaVersion       := "2.12.12"

ThisBuild / githubWorkflowOSes  := Seq("ubuntu-latest", "macos-latest", "windows-latest")
ThisBuild / githubWorkflowEnv   := Map.empty
ThisBuild / githubWorkflowBuild := Seq(
  // Using credentials files to allow individual tests to use different information, this is the default
  WorkflowStep.Run(
    commands = {
      List(
        """|rm -rf ~/.ivy2/cache/io.github.er1c
           |rm -rf ~/.ivy2/local/io.github.er1c
           |mkdir ~/.github || true
           |echo "
           |realm = GitHub API Realm
           |host = api.github.com
           |user = ${{ secrets.GH_USER }}
           |password = ${{ secrets.GITHUB_TOKEN }}
           |" >> ~/.github/.credentials
           |""".stripMargin)
    },
//    env = Map(
//      "GITHUB_USER" -> "${{ secrets.GH_USER }}",
//      "GITHUB_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}"
//    )
  ),
  WorkflowStep.Sbt(
    commands = List("test", "scripted"),
    env = Map(
      "GITHUB_TOKEN_FOR_ENV_TEST" -> "${{ secrets.GITHUB_TOKEN }}",
    )
  )
)

ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("main")))
ThisBuild / githubWorkflowPublishPreamble := Seq(WorkflowStep.Run(
  List("git config user.name \"Github Actions (er1c/sbt-github)\"")
))

ThisBuild / githubWorkflowPublishPreamble += WorkflowStep.Use(UseRef.Public("olafurpg", "setup-gpg", "v3"))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(
  List("ci-release"),
  env = Map(
    "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
    "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
    "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
    "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
  )
))

// Update ci via `sbt githubWorkflowGenerate`

val dispatchVersion = "1.2.0"

lazy val commonSettings: Seq[Setting[_]] = Seq(
    scalacOptions ++= Seq(Opts.compile.deprecation, "-Xlint", "-feature"),
    scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
      case Some((2, v)) if v >= 11 => unusedWarnings
    }.toList.flatten,
//    githubOwner := Some("er1c"),
//    githubRepository := "sbt-github",
    publishMavenStyle := true,
    Test / publishArtifact := false,
    logBuffered := false,
    scriptedBufferLog := false,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-XX:MaxPermSize=256M",
      "-Dgithub.user=username",
      "-Dgithub.pass=password",
      "-Dplugin.version=" + version.value,
      "-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"
    )
  ) ++ Seq(Compile, Test).flatMap(c =>
    c / console / scalacOptions --= unusedWarnings
  )

lazy val root =
  project
    .in(file("."))
    .aggregate(core, sbtGitHub, sbtGitHubRemoteCache)
    .settings(
      name := "sbt GitHub",
      publish / skip := true,
    )

lazy val core =
  project
    .in(file("core"))
    .enablePlugins(SbtPlugin)
    .settings(commonSettings)
    .settings(
      name := "sbt GitHub Core",
      libraryDependencies ++= Seq(
        "org.dispatchhttp" %% "dispatch-core"   % "1.2.0",
        "com.eed3si9n"     %% "repatch-github" % "dispatch1.2.0_0.1.0",
        //"org.dispatchhttp" %% "dispatch-json4s-native" % "1.1.3",
        "org.slf4j" % "slf4j-nop" % "1.7.28", // https://github.com/sbt/sbt-bintray/issues/26
        "com.eed3si9n.verify" %% "verify" % "0.2.0" % Test
      ),
      testFrameworks += new TestFramework("verify.runner.Framework"),
      resolvers += Resolver.sonatypeRepo("releases")
   )

lazy val sbtGitHub =
  project
    .in(file("sbt-github"))
    .enablePlugins(SbtPlugin)
    .dependsOn(core)
    .settings(commonSettings)
    .settings(
      name := "sbt GitHub",
      pluginCrossBuild / sbtVersion := "1.2.1"
    )

lazy val sbtGitHubRemoteCache =
  project
    .in(file("sbt-github-remote-cache"))
    .enablePlugins(SbtPlugin)
    .dependsOn(core)
    .settings(commonSettings)
    .settings(
      name := "sbt GitHub Remote Cache",
      pluginCrossBuild / sbtVersion := "1.4.2"
    )
