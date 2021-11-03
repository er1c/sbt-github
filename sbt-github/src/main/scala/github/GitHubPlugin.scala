package github

import sbt.{ AutoPlugin, Credentials, Global, Path, Resolver, Setting, Task, Tags, ThisBuild }
import sbt.Classpaths.publishTask
import sbt.Def.{ Initialize, setting, task, taskDyn }
import sbt.Keys._
import sbt._

object GitHubPlugin extends AutoPlugin {
  import GitHubKeys._
  import InternalGitHubKeys._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override def globalSettings: Seq[Setting[_]] = globalPublishSettings
  override def buildSettings: Seq[Setting[_]] = buildPublishSettings
  override def projectSettings: Seq[Setting[_]] = githubSettings

  lazy val isEnabledViaProp: Boolean = sys.props.get("sbt.sbtgithub")
    .getOrElse("true").toLowerCase(java.util.Locale.ENGLISH) match {
    case "true" | "1" | "always" => true
    case _ => false
  }

  object autoImport extends GitHubKeys with GitHubResolverSyntax

  lazy val Git = Tags.Tag("git")

  def githubSettings: Seq[Setting[_]] =
    githubCommonSettings ++ githubPublishSettings ++ githubQuerySettings

  def githubCommonSettings: Seq[Setting[_]] = Seq(
    githubChangeCredentials := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.changeCredentials(context, streams.value.log)
    },
    githubWhoami := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.whoami(GitHub.ensuredCredentials(context, streams.value.log), streams.value.log)
    }
  )

  def githubQuerySettings: Seq[Setting[_]] = Seq(
    githubPackageVersions := packageVersionsTask.value
  )

  def globalPublishSettings: Seq[Setting[_]] = Seq(
    githubCredentialsFile in Global := Path.userHome / ".github" / ".credentials",
    concurrentRestrictions in Global += Tags.exclusive(Git)
  )

  def buildPublishSettings: Seq[Setting[_]] = Seq(
    githubOwner in ThisBuild := None
  )

  def githubPublishSettings: Seq[Setting[_]] = githubCommonSettings ++ Seq(
    githubPackage := moduleName.value,
    githubPackageName := githubPackageNameTask.value,
    githubRepo := GitHub.cachedRepo(
      githubEnsureCredentials.value,
      githubOwner.value,
      githubOwnerType.value,
      githubRepository.value
    ),
    githubOwnerType := GitHubOwnerType.User,
    // todo: don't force this to be sbt-plugin-releases
    githubRepository := {
      if (sbtPlugin.value) GitHub.defaultSbtPluginRepository
      else GitHub.defaultMavenRepository
    },
    publishMavenStyle := {
      if (sbtPlugin.value) false
      else publishMavenStyle.value
    },
    githubPackageLabels := Nil,
    description in github := description.value,
    // note: publishTo may not have dependencies. therefore, we can not rely well on inline overrides
    // for inline credentials resolution we recommend defining githubCredentials _before_ mixing in the defaults
    // perhaps we should try overriding something in the publishConfig setting -- https://github.com/sbt/sbt-pgp/blob/master/pgp-plugin/src/main/scala/com/typesafe/sbt/pgp/PgpSettings.scala#L124-L131
    publishTo in github := publishToGitHub.value,
    //githubResolverName := GitHubResolverSyntax.makeGitHubRepoName(githubOwner.value.getOrElse(crede), githubRepository.value),
    resolvers in github := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.buildResolvers(GitHub.ensuredCredentials(context, sLog.value),
        githubOwner.value,
        githubRepository.value,
        publishMavenStyle.value,
        sLog.value
      )
    },
    credentials in github := {
      val repo = githubRepo.value
      Seq(Credentials("GitHub Package Registry", "maven.pkg.github.com", repo.credentials.user, repo.credentials.token))
    },
    githubEnsureCredentials := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.ensuredCredentials(context, streams.value.log).getOrElse {
        sys.error(s"Missing github credentials. " +
          s"Either create a credentials file with the githubChangeCredentials task, " +
          s"set the GITHUB_TOKEN environment variables or " +
          s"pass github.token properties to sbt.")
      }
    },
    githubUnpublish := dynamicallyGitHubUnpublish.value,
  ) ++ Seq(
    resolvers ++= {
      val rs = (resolvers in github).value
      if (isEnabledViaProp) rs
      else Nil
    },
    credentials ++= {
      val cs = (credentials in github).value
      if (isEnabledViaProp) cs
      else Nil
    },
    publishTo := {
      val old = publishTo.value
      val p = (publishTo in github).value
      if (isEnabledViaProp) p
      else old
    },
    publish := dynamicallyPublish.value
  )

  // uses taskDyn because it can return one of two potential tasks
  // as its result, each with their own dependencies
  // see also: http://www.scala-sbt.org/0.13/docs/Tasks.html#Dynamic+Computations+with
  private def dynamicallyPublish: Initialize[Task[Unit]] = taskDyn {
    val sk = ((skip in publish) ?? false).value
    val s = streams.value
    val ref = thisProjectRef.value

    if (!isEnabledViaProp) {
      publishTask(publishConfiguration)
    } else if (sk) Def.task {
      s.log.debug(s"skipping publish for ${ref.project}")
    } else {
      dynamicallyPublish0
    }
  }

  private def dynamicallyPublish0: Initialize[Task[Unit]] = taskDyn {
    // (if (githubReleaseOnPublish.value) githubRelease else warnToRelease).dependsOn(publishTask(publishConfiguration))
    publishTask(publishConfiguration)
  }

  // uses taskDyn because it can return one of two potential tasks
  // as its result, each with their own dependencies
  // see also: http://www.scala-sbt.org/0.13/docs/Tasks.html#Dynamic+Computations+with
  private def dynamicallyGitHubUnpublish: Initialize[Task[Unit]] =
    taskDyn {
      val sk = ((skip in publish) ?? false).value
      val s = streams.value
      val ref = thisProjectRef.value
      if (sk) Def.task {
        s.log.debug(s"Skipping githubUnpublish for ${ref.project}")
      } else dynamicallyGitHubUnpublish0
    }

  private def dynamicallyGitHubUnpublish0: Initialize[Task[Unit]] = Def.task {
    val repo = githubRepo.value
    repo.unpublish(githubPackage.value, version.value, streams.value.log)
  }

  /** set a user-specific github endpoint for sbt's `publishTo` setting.*/
  private def publishToGitHub: Initialize[Option[Resolver]] = setting {
    val credsFile = githubCredentialsFile.value
    val owner = githubOwner.value
    val repoName = githubRepository.value
    val ownerType = githubOwnerType.value
    val context = GitHubCredentialContext(credsFile)
    val publishEnabled = publishArtifact.value

    if (publishEnabled) {
      // ensure that we have credentials to build a resolver that can publish to github
      GitHub.withRepo(context, owner, ownerType, repoName, sLog.value) { repo =>
        repo.buildPublishResolver(publishMavenStyle.value, sLog.value)
      }
    } else {
      None
    }
  }

  /** Lists versions of github packages corresponding to the current project */
  private def packageVersionsTask: Initialize[Task[Seq[String]]] = task {
    val credsFile = githubCredentialsFile.value
    val owner = githubOwner.value
    val repoName = githubRepository.value
    val context = GitHubCredentialContext(credsFile)
    val fullPackageName = githubPackageName.value
    val ownerType = githubOwnerType.value
    streams.value.log.error(s"packageVersionTask - fullPackageName: $fullPackageName")
    GitHub.withRepo(context, owner, ownerType, repoName, streams.value.log) { repo =>
      repo.packageVersions(fullPackageName, streams.value.log)
    }.getOrElse(Nil)
  }


  /** Lists github package name (full w/org and bin versions) corresponding to the current project */
  private def githubPackageNameTask: Initialize[Task[String]] = task {
    val projectModuleId: ModuleID = projectID.value
    val projectArtifact: Artifact = artifact.value
    val projectGroupId: String = organization.value
    val projectScalaBinaryVersion: String = scalaBinaryVersion.value
    val isCrossPathsEnabled: Boolean = crossPaths.value
    val isSbtPlugin: Boolean = sbtPlugin.value

    if (isSbtPlugin) {
      val base = s"$projectGroupId.${projectArtifact.name}_$projectScalaBinaryVersion"
      projectScalaBinaryVersion match {
        case "2.10" => s"${base}_0.13"
        case "2.12" => s"${base}_1.0"
      }
    } else if (isCrossPathsEnabled) {
      val (prefix, suffix) = projectModuleId.crossVersion match {
        case _: librarymanagement.Disabled => ("", "")
        case _: librarymanagement.Constant => ???
        case _: librarymanagement.Patch => ???
        case binary: librarymanagement.Binary => (binary.prefix, binary.suffix)
      }

      s"$projectGroupId.${projectArtifact.name}_$prefix$projectScalaBinaryVersion$suffix"
    } else {
      s"$projectGroupId.${projectArtifact.name}"
    }
  }
}
