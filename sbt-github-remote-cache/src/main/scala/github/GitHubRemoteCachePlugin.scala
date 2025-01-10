package github

import sbt._
import sbt.Def
import Keys._
import java.io.IOException

object GitHubRemoteCachePlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  import InternalGitHubRemoteCacheKeys._
  object autoImport extends GitHubRemoteCacheKeys with GitHubResolverSyntax
  import autoImport._

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    githubRemoteCacheCredentialsFile := Path.userHome / ".github" / ".credentials",
    githubRemoteCacheTokenSource :=
      TokenSource.Property("github.token") ||
        TokenSource.Environment("GITHUB_TOKEN") ||
        TokenSource.GitConfig("github.token"),
    githubRemoteCacheEnsureCredentials := {
      val context = GitHubCredentialContext(githubRemoteCacheCredentialsFile.value, githubRemoteCacheTokenSource.value)
      GitHub.ensuredCredentials(context, streams.value.log).getOrElse {
        sys.error(s"Missing github credentials. " +
          s"Either create a credentials file with the githubChangeCredentials task, " +
          s"set the GITHUB_TOKEN environment variables or " +
          s"pass github.token properties to sbt.")
      }
    },
    githubRemoteCacheOwnerType := GitHubOwnerType.User,
    githubRemoteCacheRepository := "remote-cache",
    githubRemoteCacheMinimum := GitHubRemoteDefaults.minimum,
    githubRemoteCacheTtl := GitHubRemoteDefaults.ttl,
    githubRemoteCacheRepo := None
  )

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    githubRemoteCacheCleanOld := packageCleanOldVersionsTask.value,
    githubRemoteCacheCleanOld / aggregate := false,
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    remoteCacheResolvers := {
      val owner = githubRemoteCacheOwner.value
      val repoName = githubRemoteCacheRepository.value
      List(Resolver.githubRepo(owner, repoName))
    },
    pushRemoteCacheTo := publishToGitHubSetting.value,
    credentials := {
      val ret = for {
        repo <- githubRemoteCacheRepo.value
      } yield Seq(Credentials("GitHub Package Registry", "maven.pkg.github.com", repo.credentials.user, repo.credentials.token))

      ret.getOrElse(Nil)
    },
    //githubRemoteCachePackageName := githubRemoteCachePackageNameTask.value,
    githubRemoteCachePackage := moduleName.value,
    githubRemoteCacheRepo := {
      val creds = githubRemoteCacheEnsureCredentials.value
      val ownerType = githubRemoteCacheOwnerType.value

      for {
        owner <- githubRemoteCacheOwner.?.value
        repoName <- githubRemoteCacheRepository.?.value
      } yield {
        GitHub.cachedRepo(
          creds,
          owner,
          ownerType,
          repoName
        )
      }
    },
    //githubRemoteCacheUnpublish := githubRemoteCacheUnpublishTask.value,
    //githubRemoteCacheVersions := githubRemoteCacheVersionsTask.value,
    githubRemoteCachePushRemoteCacheResult := githubRemoteCachePushRemoteCacheResultTask.value,
    pushRemoteCache := (Def.taskDyn {
      val ret = githubRemoteCachePushRemoteCacheResult.value
      val retEither = ret.toEither
      val isFailure: Boolean = retEither.isLeft
      val logger = streams.value.log

      val alreadyExists: Boolean = {
        if (isFailure) {
          Incomplete.allExceptions(retEither.left.get).collectFirst {
            case ex: IOException if ex.getMessage.contains("failed with status code 409") => true
          } getOrElse false
        } else {
          false
        }
      }

      if (alreadyExists) Def.task {
//        streams.value.log.error(s"Need to unpublish first...")
//        val _ = githubRemoteCacheUnpublish.value
//        githubRemoteCachePushRemoteCacheResult.value match {
//          case Inc(incomplete: Incomplete) => sys.error(s"Error trying to publish after deleting version: ${incomplete} ")
//          case Value(_) => ()
//        }
        logger.info(s"Remote Cache version already exists, skipping.")
        ()
      } else Def.task {
        if (isFailure) sys.error(s"${retEither.left.get}")
        else ()
      }
    }).value,
    Compile / pushRemoteCacheConfiguration := (Compile / pushRemoteCacheConfiguration).value.withOverwrite(true),
    Test / pushRemoteCacheConfiguration := (Test / pushRemoteCacheConfiguration).value.withOverwrite(true)
  )

  private def publishToGitHubSetting: Def.Initialize[Option[Resolver]] = Def.setting {
    val credsFile = githubRemoteCacheCredentialsFile.value
    val tokenSource = githubRemoteCacheTokenSource.value
    val ownerType = githubRemoteCacheOwnerType.value
    val context = GitHubCredentialContext(credsFile, tokenSource)

    for {
      owner <- githubRemoteCacheOwner.?.value
      repo <- githubRemoteCacheRepository.?.value
      // ensure that we have credentials to build a resolver that can publish to github
      resolver <- GitHub.withRepo(context, owner, ownerType, repo, sLog.value) { repo =>
        repo.buildRemoteCacheResolver(publishMavenStyle.value, sLog.value)
      }
    } yield resolver
  }


  // This is basically duplicated from https://github.com/sbt/sbt/blob/8586e19f62c60d2e6cc903ddc71df94f3fb2e012/main/src/main/scala/sbt/internal/RemoteCache.scala#L72-L85
  // in order to get the full Result
  private def githubRemoteCachePushRemoteCacheResultTask: Def.Initialize[Task[Result[Seq[Unit]]]] = Def.taskDyn {
    val arts = (pushRemoteCacheConfiguration / remoteCacheArtifacts).value
    val configs = arts flatMap { art =>
      art.packaged.scopedKey.scope match {
        case Scope(_, Select(c), _, _) => Some(c)
        case _                         => None
      }
    }
    val filter = ScopeFilter(configurations = inConfigurationsByKeys(configs: _*))
    Def.task {
      pushRemoteCache.all(filter).result.value
    }
  }

//  private def githubRemoteCacheUnpublishTask: Def.Initialize[Task[Unit]] = Def.taskDyn {
//    val skip = ((packageCache / pushRemoteCacheArtifact) ?? false).value
//    val s = streams.value
//    val ref = thisProjectRef.value
//    if (skip) Def.task {
//      s.log.debug(s"Skipping githubRemoteCacheUnpublish for ${ref.project}")
//    } else githubRemoteCacheUnpublishImpl
//  }
//
//  private def githubRemoteCacheUnpublishImpl: Def.Initialize[Task[Unit]] = Def.task {
//    val repo = githubRemoteCacheRepo.value
//    val versions = githubRemoteCacheVersions.value
//    val pkg = githubRemoteCachePackageName.value
//    val logger = streams.value.log
//
//    logger.error(s"githubRemoteCacheUnpublishImpl(pkg: $pkg, versions: $versions)")
//
//    for {
//      r <- repo.toSeq
//      version <- versions
//    } {
//      logger.error(s"githubRemoteCacheUnpublishImpl.unpublish(pkg: $pkg, version: $version)")
//      r.unpublish(pkg, version, logger)
//    }
//  }
//
//  def githubRemoteCacheVersionsTask: Def.Initialize[Task[Seq[String]]] = Def.taskDyn {
//    val arts = (pushRemoteCacheConfiguration / remoteCacheArtifacts).value
//    val configs = arts flatMap { art =>
//      art.packaged.scopedKey.scope match {
//        case Scope(_, Select(c), _, _) => Some(c)
//        case _                         => None
//      }
//    }
//    val filter = ScopeFilter(configurations = inConfigurationsByKeys(configs: _*))
//    Def.task {
//      remoteCacheId.all(filter).value.map { id =>
//        // https://github.com/sbt/sbt/blob/8586e19f62c60d2e6cc903ddc71df94f3fb2e012/main/src/main/scala/sbt/internal/RemoteCache.scala#L347
//        s"0.0.0-$id"
//      }
//    }
//  }

  def packageCleanOldVersionsTask: Def.Initialize[Task[Unit]] = Def.taskDyn {
    val credsFile = githubRemoteCacheCredentialsFile.value
    val tokenSource = githubRemoteCacheTokenSource.value
    val ownerType = githubRemoteCacheOwnerType.value
    val context = GitHubCredentialContext(credsFile, tokenSource)
    val s = streams.value
    val min = githubRemoteCacheMinimum.value
    val ttl = githubRemoteCacheTtl.value

    Def.task {
      for {
        owner <- githubRemoteCacheOwner.?.value
        repoName <- githubRemoteCacheRepository.?.value
        pkg = githubRemoteCachePackageName.value
      } yield {
        GitHub.withRepo(context, owner, ownerType, repoName, s.log) { repo =>
          repo.cleandOldVersions(pkg, min, ttl, s.log)
        }
      }
    }
  }

//  /** Lists github package name (full w/org and bin versions) corresponding to the current project */
//  private def githubRemoteCachePackageNameTask: Def.Initialize[Task[String]] = Def.task {
//    val projectModuleId: ModuleID = projectID.value
//    val projectArtifact: Artifact = artifact.value
//    val projectGroupId: String = organization.value
//    val projectScalaBinaryVersion: String = scalaBinaryVersion.value
//    val isCrossPathsEnabled: Boolean = crossPaths.value
//    val isSbtPlugin: Boolean = sbtPlugin.value
//
//    if (isSbtPlugin) {
//      val base = s"$projectGroupId.${projectArtifact.name}_$projectScalaBinaryVersion"
//      projectScalaBinaryVersion match {
//        case "2.10" => s"${base}_0.13"
//        case "2.12" => s"${base}_1.0"
//      }
//    } else if (isCrossPathsEnabled) {
//      val (prefix, suffix) = projectModuleId.crossVersion match {
//        case _: librarymanagement.Disabled => ("", "")
//        case _: librarymanagement.Constant => sys.error("Unsupported projectModuleId.crossVersion: sbt.librarymanagement.Constant")
//        case _: librarymanagement.Patch => sys.error("Unsupported projectModuleId.crossVersion: sbt.librarymanagement.Path")
//        case binary: librarymanagement.Binary => (binary.prefix, binary.suffix)
//      }
//
//      s"$projectGroupId.${projectArtifact.name}_$prefix$projectScalaBinaryVersion$suffix"
//    } else {
//      s"$projectGroupId.${projectArtifact.name}"
//    }
//  }
}
