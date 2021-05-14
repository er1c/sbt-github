package github

import sbt._

import scala.concurrent.duration.Duration

trait GitHubKeys {
  val github = taskKey[String]("sbt-github is an interface for the github package service")

  val githubRelease = taskKey[Unit](
    "Releases a version of package on github")

  val githubReleaseOnPublish = settingKey[Boolean](
    "When set to true, publish also runs githubRelease")

  val githubOwner = settingKey[Option[String]](
    "GitHub user or organization name")

  val githubPackage = taskKey[String](
    "GitHub package name")

  val githubRepository = settingKey[String](
    "GitHub repository to publish to. Defaults to 'maven' unless project is an sbtPlugin")

  val githubPackageLabels = settingKey[Seq[String]](
    "List of labels associated with your github package")

//  val githubPackageAttributes = settingKey[AttrMap](
//    "List of github package metadata attributes")
//
//  val githubVersionAttributes = settingKey[AttrMap](
//    "List of github version metadata attributes")

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

  val githubEnsureGitHubPackageExists = taskKey[Unit](
    "Ensure that the github package exists and is valid.")

  val githubUnpublish = taskKey[Unit](
    "Unpublishes a version of package on github")

  val githubRemoteSign = taskKey[Unit](
    "PGP sign artifacts hosted remotely on github. (See also https://github.com/docs/uploads/uploads_gpgsigning.html)")

  val githubSyncMavenCentral = taskKey[Unit](
    "Sync github-published artifacts with maven central")

  val githubSyncSonatypeStaging = taskKey[Unit](
    "Sync github-published artifacts with sonatype staging repo without releasing them to maven central")

  val githubSyncMavenCentralRetries = settingKey[Seq[Duration]](
    "In case of synchronization failure, it will be retried according to delays specified. Set to empty sequence for no retries.")

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
  val githubRepo = taskKey[GitHubRepo]("GitHub repository.")
}

object InternalGitHubKeys extends InternalGitHubKeys {}