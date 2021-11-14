package github

import sbt._

trait GitHubKeys {
  val github = taskKey[String]("sbt-github is an interface for the github package service")

  val githubOwner = settingKey[String]("GitHub user or organization name to publish to.")
  val githubRepository = settingKey[String]("GitHub repository to publish to.")
  val githubOwnerType = settingKey[GitHubOwnerType]("GitHub owner type (GitHubOwnerType.User or GitHubOwnerType.Organization), default GitHubOwnerType.User.")
  val githubPackage = settingKey[String]("GitHub package name, default moduleName.value.")

  val githubResolverName = settingKey[String]("GitHub resolver name, default github-{githubOwner}-{githubRepository}.")
  val githubCredentialsFile = settingKey[File]("File containing github api credentials")
  val githubPackageVersions = taskKey[Seq[String]]("List github versions for the current package")

  val githubChangeCredentials = taskKey[Unit]("Change your current github credentials")
  val githubWhoami = taskKey[String]("Print the name of the currently authenticated github user")
  val githubEnsureCredentials = taskKey[GitHubCredentials]("Ensure that the credentials for github are valid.")
  val githubUnpublish = taskKey[Unit]("Unpublishes a version of package on github")
}

object GitHubKeys extends GitHubKeys {}

trait InternalGitHubKeys {
  val githubRepo = taskKey[Option[GitHubRepo]]("GitHub repository: GitHubRepo.")
  val githubPackageName = taskKey[String]("GitHub full package_name.")
}

object InternalGitHubKeys extends InternalGitHubKeys {}
