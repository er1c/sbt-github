package github

import sbt._
import Keys._

object GitHubRemoteCachePlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends GitHubRemoteCacheKeys with GitHubResolverSyntax
  import autoImport._

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    githubRemoteCacheCredentialsFile := Path.userHome / ".github" / ".credentials",
    githubRemoteCacheRepository := "remote-cache",
    githubRemoteCacheMinimum := GitHubRemoteDefaults.minimum,
    githubRemoteCacheTtl := GitHubRemoteDefaults.ttl,
  )

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    githubRemoteCacheCleanOld := packageCleanOldVersionsTask.value,
    githubRemoteCacheCleanOld / aggregate := false,
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    pushRemoteCacheTo := publishToGitHubSetting.value,
    remoteCacheResolvers := {
      val owner = githubRemoteCacheOwner.value
      val repoName = githubRemoteCacheRepository.value
      List(Resolver.githubRepo(owner, repoName))
    },
  )

  def publishToGitHubSetting =
    Def.setting {
      val credsFile = githubRemoteCacheCredentialsFile.value
      val owner = githubRemoteCacheOwner.value
      val repoName = githubRemoteCacheRepository.value
      val context = GitHubCredentialContext.remoteCache(credsFile)
      val ownerType = githubRemoteCacheOwnerType.value
      val isMavenStyle = publishMavenStyle.value
      GitHub.withRepo(context, Some(owner), ownerType, repoName, sLog.value) { repo =>
        repo.buildRemoteCacheResolver(isMavenStyle)
      }
    }

  def packageCleanOldVersionsTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val credsFile = githubRemoteCacheCredentialsFile.value
      val owner = githubRemoteCacheOwner.value
      val ownerType = githubRemoteCacheOwnerType.value
      val repoName = githubRemoteCacheRepository.value
      val context = GitHubCredentialContext.remoteCache(credsFile)
      val pkg = githubRemoteCachePackage.value
      val s = streams.value
      val min = githubRemoteCacheMinimum.value
      val ttl = githubRemoteCacheTtl.value
      GitHub.withRepo(context, Some(owner), ownerType, repoName, s.log) { repo =>
        repo.cleandOldVersions(pkg, min, ttl, s.log)
      }
    }
}
