package github

import github.TokenSource.resolveTokenSource
import sbt._

import scala.collection.concurrent.TrieMap

object GitHub {
  import GitHubResolverSyntax._
  val defaultMavenRepository = "maven"
  val defaultRemoteCacheMavenRepository = "remote-cache"

  private[github] object await {
    import scala.concurrent.{ Await, Future }
    import scala.concurrent.duration.Duration

    def result[T](f: => Future[T]): T = Await.result(f, Duration.Inf)
    def ready[T](f: => Future[T]): Unit = Await.ready(f, Duration.Inf)
  }

  def publishTo(
    owner: String,
    repoName: String,
    mvnStyle: Boolean = true,
    log: sbt.Logger,
  ): Resolver = makeResolver(owner, repoName, mvnStyle, log)

  def withRepo[A](context: GitHubCredentialContext, owner: String, ownerType: GitHubOwnerType, repoName: String, log: Logger)
    (f: GitHubRepo => A): Option[A] =
    ensuredCredentials(context, log) map { cred =>
      val repo = cachedRepo(cred, owner, ownerType, repoName)
      f(repo)
    }

  private val repoCache: TrieMap[(GitHubCredentials, String, String), GitHubRepo] = TrieMap()

  def cachedRepo(credential: GitHubCredentials, owner: String, ownerType: GitHubOwnerType, repoName: String): GitHubRepo =
    repoCache.synchronized {
      // lock to avoid creating and leaking HTTP client threadpools
      // see: https://github.com/sbt/sbt-bintray/issues/144
      repoCache.getOrElseUpdate((credential, owner, repoName), GitHubRepo(credential, owner, ownerType, repoName))
    }

  // region --- Credentials Management ---

  private[github] def ensuredCredentials(
    context: GitHubCredentialContext,
    log: sbt.Logger
  ): Option[GitHubCredentials] = {
    resolveTokenSource(context.tokenSource).map(token => GitHubCredentials("_", token)) // GH Ignores user just use token
      .orElse(GitHubCredentials.read(context.credsFile, log))
  }

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

  def remoteCache(
    owner: String,
    repoName: String,
    mvnStyle: Boolean = true,
    log: sbt.Logger,
  ): Resolver = makeResolver(owner, repoName, mvnStyle, log)

  private[github] def buildResolvers(
    creds: Option[GitHubCredentials],
    owner: String,
    repoName: String,
    mavenStyle: Boolean,
    log: sbt.Logger
  ): Seq[Resolver] = {
    creds.map {
      case GitHubCredentials(_, _) =>
        if (mavenStyle) {
          Seq(Resolver.githubRepo(owner, repoName))
        } else {
          log.warn(IvyStyleNotSupported)
          Nil
        }
    } getOrElse Nil
  }

  private def makeResolver(
    owner: String,
    repoName: String,
    mvnStyle: Boolean,
    log: sbt.Logger,
  ): Resolver = {
    if (!mvnStyle) log.warn(IvyStyleNotSupported)
    Resolver.githubRepo(owner, repoName)
  }

  private final val IvyStyleNotSupported = "publishMavenStyle is set to false, and GitHub Packages doesn't support ivy, consider 'InThisBuild / publishMavenStyle := true'"
}
