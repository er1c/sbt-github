package github

import sbt._
import scala.collection.concurrent.TrieMap
import scala.util.Try

object GitHub {
  import GitHubResolverSyntax._
  val defaultMavenRepository = "maven"
  val defaultSbtPluginRepository = "sbt-plugins"

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

  def withRepo[A](context: GitHubCredentialContext, owner: Option[String], ownerType: GitHubOwnerType, repoName: String, log: Logger)
    (f: GitHubRepo => A): Option[A] =
    ensuredCredentials(context, log) map { cred =>
      val repo = cachedRepo(cred, owner, ownerType, repoName)
      f(repo)
    }

  private val repoCache: TrieMap[(GitHubCredentials, Option[String], String), GitHubRepo] = TrieMap()

  def cachedRepo(credential: GitHubCredentials, owner: Option[String], ownerType: GitHubOwnerType, repoName: String): GitHubRepo =
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
    propsCredentials(context, log)
      .orElse(envCredentials(context, log))
      .orElse(GitHubCredentials.read(context.credsFile, log))
  }

  private def propsCredentials(context: GitHubCredentialContext, log: sbt.Logger): Option[GitHubCredentials] =
    for {
      name <- sys.props.get(context.userProp)
      pass <- sys.props.get(context.tokenProp)
    } yield {
      log.info(s"Using Property-based credentials.")
      GitHubCredentials(name, pass)
    }

  private def envCredentials(context: GitHubCredentialContext, log: sbt.Logger): Option[GitHubCredentials] =
    for {
      name <- sys.env.get(context.userEnv)
      pass <- sys.env.get(context.tokenEnv)
    } yield {
      log.info(s"Using Environment-based credentials.")
      GitHubCredentials(name, pass)
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
  ): Resolver = makeRepository(owner, repoName, mvnStyle)

  def resolveVcsUrl: Try[Option[String]] =
    Try {
      val pushes =
        sys.process.Process("git" :: "remote" :: "-v" :: Nil).!!.split("\n")
          .flatMap {
            _.split("""\s+""") match {
              case Array(name, url, "(push)") => Some((name, url))
              case _                          => None
            }
          }

      pushes
        .find { case (name, _) => "origin" == name }
        .orElse(pushes.headOption)
        .map { case (_, url) => url }
    }

  private[github] def buildResolvers(
    creds: Option[GitHubCredentials],
    owner: Option[String],
    repoName: String,
    mavenStyle: Boolean,
    log: sbt.Logger
  ): Seq[Resolver] = {
    creds.map {
      case GitHubCredentials(user, _) =>
        if (mavenStyle) {
          Seq(Resolver.githubRepo(owner.getOrElse(user), repoName))
        } else {
          log.warn(IvyStyleNotSupported)
          Nil
        }
    } getOrElse Nil
  }

  private def makeRepository(
    owner: String,
    repoName: String,
    mvnStyle: Boolean
  ): RawRepository = {
    if (mvnStyle) {
      RawRepository(Resolver.githubRepo(owner, repoName))
    } else {
      sys.error(IvyStyleNotSupported)
    }
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
