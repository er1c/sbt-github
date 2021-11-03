package github

import sbt._

trait GitHubKeys {
  val github = taskKey[String]("sbt-github is an interface for the github package service")

  val githubOwner = settingKey[Option[String]](
    "GitHub user or organization name, default None.")

  val githubOwnerType = settingKey[GitHubOwnerType](
    "GitHub owner type (GitHubOwnerType.User or GitHubOwnerType.Org), default GitHubOwnerType.User.")

  val githubPackage = settingKey[String](
    "GitHub package name, default moduleName.value.")

  val githubRepository = settingKey[String](
    "GitHub repository to publish to. Defaults to 'maven' unless project is an sbtPlugin")

  val githubResolverName = settingKey[String](
    "GitHub resolver name, default github-{githubOwner}-{githubRepository}.")

  val githubPackageLabels = settingKey[Seq[String]](
    "List of labels associated with your github package")

  val githubCredentialsFile = settingKey[File](
    "File containing github api credentials")

  val githubPackageVersions = taskKey[Seq[String]](
    "List github versions for the current package")

  val githubChangeCredentials = taskKey[Unit](
    "Change your current github credentials")

  val githubWhoami = taskKey[String](
    "Print the name of the currently authenticated github user")

  val githubEnsureCredentials = taskKey[GitHubCredentials](
    "Ensure that the credentials for github are valid.")

  val githubUnpublish = taskKey[Unit](
    "Unpublishes a version of package on github")

  val githubRemoteSign = taskKey[Unit](
    "PGP sign artifacts hosted remotely on github. (See also https://github.com/docs/uploads/uploads_gpgsigning.html)")

  val githubVcsUrl = taskKey[Option[String]](
    "Canonical url for hosted version control repository")

  /** named used for common package attributes lifted from sbt
   *  build definitions */
  object AttrNames {
    val scalas = "scalas"
    val sbtPlugin = "sbt-plugin"
    val sbtVersion = "sbt-version"
  }
}

object GitHubKeys extends GitHubKeys {}

trait InternalGitHubKeys {
  val githubRepo = taskKey[GitHubRepo](
    "GitHub repository: GitHubRepo.")
  val githubPackageName = taskKey[String](
    "GitHub full package_name.")
}

object InternalGitHubKeys extends InternalGitHubKeys {}
