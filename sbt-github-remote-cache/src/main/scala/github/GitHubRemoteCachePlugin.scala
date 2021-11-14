package github

import sbt._
import sbt.Def
import Keys._

object GitHubRemoteCachePlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  import InternalGitHubRemoteCacheKeys._
  object autoImport extends GitHubRemoteCacheKeys with GitHubResolverSyntax
  import autoImport._

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    githubRemoteCacheCredentialsFile := Path.userHome / ".github" / ".credentials",
    githubRemoteCacheEnsureCredentials := {
      val context = GitHubCredentialContext(githubRemoteCacheCredentialsFile.value)
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
    // pushRemoteCacheConfiguration
    remoteCacheResolvers := {
      val owner = githubRemoteCacheOwner.value
      val repoName = githubRemoteCacheRepository.value
      List(Resolver.githubRepo(owner, repoName))
    },
    pushRemoteCacheTo := publishToGitHubSetting.value,
    credentials := {
      println(s"githubRemoteCacheRepo: ${githubRemoteCacheRepo.value}")
      val ret = for {
        repo <- githubRemoteCacheRepo.value
      } yield Seq(Credentials("GitHub Package Registry", "maven.pkg.github.com", repo.credentials.user, repo.credentials.token))

      ret.getOrElse(Nil)
    },
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
  )

  private def publishToGitHubSetting: Def.Initialize[Option[Resolver]] = Def.setting {
    val credsFile = githubRemoteCacheCredentialsFile.value
    val ownerType = githubRemoteCacheOwnerType.value
    val context = GitHubCredentialContext(credsFile)

    for {
      owner <- githubRemoteCacheOwner.?.value
      repo <- githubRemoteCacheRepository.?.value
      // ensure that we have credentials to build a resolver that can publish to github
      resolver <- GitHub.withRepo(context, owner, ownerType, repo, sLog.value) { repo =>
        repo.buildRemoteCacheResolver(publishMavenStyle.value, sLog.value)
      }
    } yield resolver
}

  def packageCleanOldVersionsTask: Def.Initialize[Task[Unit]] = Def.taskDyn {
    val credsFile = githubRemoteCacheCredentialsFile.value
    val ownerType = githubRemoteCacheOwnerType.value
    val context = GitHubCredentialContext.remoteCache(credsFile)
    val s = streams.value
    val min = githubRemoteCacheMinimum.value
    val ttl = githubRemoteCacheTtl.value

    Def.task {
      for {
        owner <- githubRemoteCacheOwner.?.value
        repoName <- githubRemoteCacheRepository.?.value
        pkg = githubRemoteCachePackage.value
      } yield {
        GitHub.withRepo(context, owner, ownerType, repoName, s.log) { repo =>
          repo.cleandOldVersions(pkg, min, ttl, s.log)
        }
      }
    }
  }
}
