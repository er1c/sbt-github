package github

import sbt._
import scala.collection.concurrent.TrieMap

object GitHub {
  val defaultMavenRepository = "maven"
  // http://www.scala-sbt.org/0.13/docs/Bintray-For-Plugins.html
  val defaultSbtPluginRepository = "sbt-plugins"

  def withRepo[A](context: GitHubCredentialContext, owner: String, repoName: String, log: Logger)
    (f: GitHubRepo => A): Option[A] =
    ensuredCredentials(context, log) map { cred =>
      val repo = cachedRepo(cred, owner, repoName)
      f(repo)
    }

  private val repoCache: TrieMap[(GitHubCredentials, String, String), GitHubRepo] = TrieMap()

  def cachedRepo(credential: GitHubCredentials, owner: String, repoName: String): GitHubRepo =
    repoCache.synchronized {
      // lock to avoid creating and leaking HTTP client threadpools
      // see: https://github.com/sbt/sbt-bintray/issues/144
      repoCache.getOrElseUpdate((credential, owner, repoName), GitHubRepo(credential, owner, repoName))
    }

  // region --- Credentials Management ---

  private[github] def ensuredCredentials(
    context: GitHubCredentialContext,
    log: sbt.Logger
  ): Option[GitHubCredentials] =
    propsCredentials(context)
      .orElse(envCredentials(context))
      .orElse(GitHubCredentials.read(context.credsFile))

  private def propsCredentials(context: GitHubCredentialContext): Option[GitHubCredentials] =
    for {
      name <- sys.props.get(context.userProp)
      pass <- sys.props.get(context.tokenProp)
    } yield GitHubCredentials(name, pass)

  private def envCredentials(context: GitHubCredentialContext): Option[GitHubCredentials] =
    for {
      name <- sys.env.get(context.userEnv)
      pass <- sys.env.get(context.tokenEnv)
    } yield GitHubCredentials(name, pass)

  private def saveGitHubCredentials(to: File)(creds: (String, String), log: Logger): Unit = {
    log.info(s"saving credentials to $to")
    val (name, pass) = creds
    GitHubCredentials.writeGitHub(name, pass, to)
    log.info("reload project for sbt setting `publishTo` to take effect")
  }

  private def requestCredentials(
    defaultUser: Option[String] = None,
    defaultToken: Option[String] = None): (String, String) = {
    val name = Prompt("Enter GitHub username%s" format defaultUser.map(" (%s)".format(_)).getOrElse("")).orElse(defaultUser).getOrElse {
      sys.error("GitHub username required")
    }
    val pass = Prompt.descretely("Enter GitHub API token %s" format defaultToken.map(_ => "(use current)").getOrElse("(under https://github.com/settings/tokens)"))
      .orElse(defaultToken).getOrElse {
      sys.error("GitHub API key required")
    }
    (name, pass)
  }

  /** assign credentials or ask for new ones */
  private[github] def changeCredentials(context: GitHubCredentialContext, log: Logger): Unit =
    GitHub.ensuredCredentials(context, Logger.Null) match {
      case None =>
        saveGitHubCredentials(context.credsFile)(requestCredentials(), log)
      case Some(GitHubCredentials(user, pass)) =>
        saveGitHubCredentials(context.credsFile)(requestCredentials(Some(user), Some(pass)), log)
    }

  def whoami(creds: Option[GitHubCredentials], log: Logger): String = {
    val is = creds match {
      case None => "nobody"
      case Some(GitHubCredentials(user, _)) => user
    }
    log.info(is)
    is
  }

  // endregion
}