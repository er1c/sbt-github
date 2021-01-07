package github

import sbt._
import bintry.{ Licenses, Client }
import scala.util.Try
import scala.collection.concurrent.TrieMap

object GitHub {
  import GitHubResolverSyntax._

  val defaultMavenRepository = "maven"
  // http://www.scala-sbt.org/0.13/docs/GitHub-For-Plugins.html
  val defaultSbtPluginRepository = "sbt-plugins"

  def publishTo(repo: Client#Repo, pkg: Client#Repo#Package, version: String,
    mvnStyle: Boolean = true, isSbtPlugin: Boolean = false, release: Boolean = false): Resolver =
    RawRepository(
      (mvnStyle, isSbtPlugin) match {
        case (true, true) =>
          GitHubMavenSbtPluginResolver(
            s"GitHub-Sbt-Maven-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
            pkg.version(version), release)
        case (true, _) =>
          GitHubMavenResolver(
            s"GitHub-Maven-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
            s"https://api.github.com/maven/${repo.subject}/${repo.repo}/${pkg.name}", pkg, release, ignoreExists = false)
        case (false, _) =>
          GitHubIvyResolver(
            s"GitHub-${if (isSbtPlugin) "Sbt" else "Ivy"}-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
            pkg.version(version), release)
      })

  def remoteCache(repo: Client#Repo, pkg: Client#Repo#Package): Resolver =
    RawRepository(
      GitHubMavenResolver(
        s"GitHub-Remote-Cache-${repo.subject}-${repo.repo}-${pkg.name}",
        s"https://api.github.com/maven/${repo.subject}/${repo.repo}/${pkg.name}", pkg, true, true)
    )

//  def whoami(creds: Option[GitHubCredentials], log: Logger): String =
//    {
//      val is = creds match {
//        case None => "nobody"
//        case Some(GitHubCredentials(user, _)) => user
//      }
//      log.info(is)
//      is
//    }

  private[github] def ensureLicenses(licenses: Seq[(String, URL)], omit: Boolean): Unit =
    {
      val acceptable = Licenses.Names.toSeq.sorted.mkString(", ")
      if (!omit) {
        if (licenses.isEmpty) sys.error(
          s"you must define at least one license for this project. Please choose one or more of\n $acceptable")
        if (!licenses.forall { case (name, _) => Licenses.Names.contains(name) }) sys.error(
          s"One or more of the defined licenses were not among the following allowed licenses\n $acceptable")
      }
    }

  def withRepo[A](context: GitHubCredentialContext, owner: String, repoName: String, log: Logger)
    (f: GitHubRepo => A): Option[A] =
    ensuredCredentials(context, log) map { cred =>
      val repo = cachedRepo(cred, owner, repoName)
      f(repo)
    }

  private val repoCache: TrieMap[(GitHubToken, String, String), GitHubRepo] = TrieMap()
  def cachedRepo(credential: GitHubToken, owner: String, repoName: String): GitHubRepo =
    repoCache.synchronized {
      // lock to avoid creating and leaking HTTP client threadpools
      // see: https://github.com/sbt/sbt-github/issues/144
      repoCache.getOrElseUpdate((credential, owner, repoName), GitHubRepo(credential, owner, repoName))
    }

  private[github] def ensuredCredentials(
    context: GitHubCredentialContext, log: Logger): Option[GitHubToken] =
      propsCredentials(context)
        .orElse(envCredentials(context))
        .orElse(GitHubToken.read(context.credsFile))

  private def propsCredentials(context: GitHubCredentialContext) =
    for {
      token <- sys.props.get(context.tokenProp)
    } yield GitHubToken(token)

  private def envCredentials(context: GitHubCredentialContext) =
    for {
      token <- sys.env.get(context.tokenEnv)
    } yield GitHubToken(token)

//  /** assign credentials or ask for new ones */
//  private[github] def changeCredentials(context: GitHubCredentialContext, log: Logger): Unit =
//    GitHub.ensuredCredentials(context, Logger.Null) match {
//      case None =>
//        saveGitHubCredentials(context.credsFile)(requestCredentials(), log)
//      case Some(GitHubCredentials(user, pass)) =>
//        saveGitHubCredentials(context.credsFile)(requestCredentials(Some(user), Some(pass)), log)
//    }

  private[github] def buildResolvers(creds: Option[GitHubToken], owner: String, repoName: String, mavenStyle: Boolean): Seq[Resolver] =
    creds.map {
      case GitHubToken(_) => Seq(
        if (mavenStyle) Resolver.githubRepo(owner, repoName)
        else Resolver.githubIvyRepo(owner, repoName)
      )
    } getOrElse Nil

//  private def saveGitHubCredentials(to: File)(creds: (String, String), log: Logger) = {
//    log.info(s"saving credentials to $to")
//    val (name, pass) = creds
//    GitHubCredentials.writeGitHub(name, pass, to)
//    log.info("reload project for sbt setting `publishTo` to take effect")
//  }

//  // todo: generalize this for both github & sonatype credential prompts
//  private def requestCredentials(
//    defaultName: Option[String] = None,
//    defaultKey: Option[String] = None): (String, String) = {
//    val name = Prompt("Enter github username%s" format defaultName.map(" (%s)".format(_)).getOrElse("")).orElse(defaultName).getOrElse {
//      sys.error("github username required")
//    }
//    val pass = Prompt.descretely("Enter github API key %s" format defaultKey.map(_ => "(use current)").getOrElse("(under https://github.com/profile/edit)"))
//        .orElse(defaultKey).getOrElse {
//          sys.error("github API key required")
//        }
//    (name, pass)
//  }

  private[github] object await {
    import scala.concurrent.{ Await, Future }
    import scala.concurrent.duration.Duration

    def result[T](f: => Future[T]) = Await.result(f, Duration.Inf)
    def ready[T](f: => Future[T]) = Await.ready(f, Duration.Inf)
  }

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
}
