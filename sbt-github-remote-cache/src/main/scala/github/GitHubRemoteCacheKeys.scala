package github

import sbt._
import scala.concurrent.duration._

trait GitHubRemoteCacheKeys {
  val githubRemoteCache = taskKey[String]("sbt-github-remote-cache is an interface for the github package service")

  val githubRemoteCacheCredentialsFile = settingKey[File]("File containing github api credentials")

  val githubRemoteCacheOwner = settingKey[String]("GitHub user or organization name to push to")
  val githubRemoteCacheOwnerType = settingKey[GitHubOwnerType]("GitHub owner type (GitHubOwnerType.User or GitHubOwnerType.Org), default GitHubOwnerType.User.")

  val githubRemoteCacheRepository = settingKey[String]("GitHub repository to publish to (default: remote-cache)")
  val githubRemoteCachePackage = settingKey[String]("GitHub package name")
  val githubRemoteCacheCleanOld = taskKey[Unit]("Clean old remote cache")
  val githubRemoteCacheMinimum = settingKey[Int](s"Minimum number of cache to keep around (default: ${GitHubRemoteDefaults.minimum})")
  val githubRemoteCacheTtl = settingKey[Duration](s"Time to keep remote cache around (default: ${GitHubRemoteDefaults.ttl})")

  val githubRemoteCacheEnsureCredentials = taskKey[GitHubCredentials]("Ensure that the credentials for github are valid.")
}

object GitHubRemoteCacheKeys extends GitHubRemoteCacheKeys

object GitHubRemoteDefaults {
  def minimum: Int = 100
  def ttl: Duration = Duration(30, DAYS)
}

trait InternalGitHubRemoteCacheKeys {
  val githubRemoteCacheRepo = taskKey[Option[GitHubRepo]]("GitHub repository: GitHubRepo.")
  val githubRemoteCachePackageName = taskKey[String]("GitHub full package_name.")
  val githubRemoteCachePushRemoteCacheResult = taskKey[Result[Seq[Unit]]]("Return the pushRemoteCache as a Result type.")
  val githubRemoteCacheUnpublish = taskKey[Unit]("Unpublish the current remote cache.")
  val githubRemoteCacheVersions = taskKey[Seq[String]]("Artifat version for remote cache.")
}

object InternalGitHubRemoteCacheKeys extends InternalGitHubRemoteCacheKeys {}

