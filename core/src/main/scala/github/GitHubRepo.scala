package github

import sbt._
import repatch.github.request.{MediaType, OAuthClient, orgs, user}
import repatch.github.{request => gh}
import dispatch.Http
import java.time.Instant
import repatch.github.response.PackageVersion
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class GitHubRepo(credentials: GitHubCredentials, owner: String, ownerType: GitHubOwnerType, repoName: String)  extends DispatchHandlers {
  import dispatch.as
  import GitHub.await

  lazy val http: Http = Http(Http.defaultClientBuilder)
  lazy val client: OAuthClient = OAuthClient(credentials.token, MediaType.default)
  lazy val repo = gh.repo(owner, repoName)

  val resolverName: String = GitHubResolverSyntax.makeGitHubRepoName(owner, repoName)

  def close(): Unit = http.shutdown()

  def buildPublishResolver(mvnStyle: Boolean, log: sbt.Logger): Resolver =
    GitHub.publishTo(owner, repoName, mvnStyle, log)

  def buildRemoteCacheResolver(mvnStyle: Boolean, log: sbt.Logger): Resolver = {
    GitHub.remoteCache(owner, repoName, mvnStyle, log)
  }

  /** unpublish (delete) a version of a package */
  def unpublish(packageName: String, vers: String, log: Logger): Unit = {
    packageVersionsImpl(packageName, log).foreach { ver: PackageVersion =>
      if (ver.name == vers) {

        val req = ownerType match {
          case GitHubOwnerType.Organization => client(orgs(owner).`package`("maven", packageName).deleteVersion(ver.id)) > asStatusAndBody
          case GitHubOwnerType.User => client(user(owner).`package`("maven", packageName).deleteVersion(ver.id)) > asStatusAndBody
        }

        await.result(
          for {
            res <- http(req)
          } yield {
            res match {
              case (200, _) => log.info(s"$owner/$packageName@$vers was discarded")
              case (404, _) => log.warn(s"$owner/$packageName@$vers was not found")
              case (_, fail) => sys.error(s"failed to discard $owner/$packageName@$vers: $fail")
            }
          }
        )
      }
    }
  }

  /** Lists versions of github packages corresponding to the current project */
  def packageVersions(packageName: String, log: Logger): Seq[String] =
    packageVersionsImpl(packageName, log).map{ _.name }

  private def packageVersionsImpl(packageName: String, log: Logger): Seq[PackageVersion] = {
    val req = ownerType match {
      case GitHubOwnerType.Organization => client(orgs(owner).`package`("maven", packageName).versions.page(1).per_page(100)) > as.repatch.github.response.PackageVersions
      case GitHubOwnerType.User => client(user(owner).`package`("maven", packageName).versions.page(1).per_page(100)) > as.repatch.github.response.PackageVersions
    }

    log.info(s"fetching package versions for package $packageName")
    await.result(
      for {
        res <- http(req).map{ _.items.sortBy{ _.created_at } }
      } yield res
    )
  }

  def packageVersionUpdatedDate(packageName: String, version: String): Instant = {
    val req = ownerType match {
      case GitHubOwnerType.Organization => client(orgs(owner).`package`("maven", packageName).versions.page(1).per_page(100)) > as.repatch.github.response.PackageVersions
      case GitHubOwnerType.User => client(user(owner).`package`("maven", packageName).versions.page(1).per_page(100)) > as.repatch.github.response.PackageVersions
    }

    await.result(
      for {
        res <- http(req).map{ _.items.collect{ case v: PackageVersion if v.name == version => v.updated_at.toInstant} }
      } yield res.head
    )
  }

  def cleandOldVersions(packageName: String, min: Int, ttl: Duration, log: Logger): Unit = {
    val vers0 = packageVersions(packageName, log)
    val vers = vers0.drop(min)
    if (vers.isEmpty || !ttl.isFinite) ()
    else {
      val expirationDate = Instant.now.minusSeconds(ttl.toSeconds)
      val expiredVersions = GitHubRepo.expiredVersions(vers.toVector, expirationDate)(packageVersionUpdatedDate(packageName, _))
      log.info(s"about to delete $expiredVersions")
      expiredVersions foreach { ver =>
        unpublish(packageName, ver, log)
      }
    }
  }
}

object GitHubRepo {
  /**
   * Return expired versions on or before the cutoffDate.
   * vers is expected to contain a sequence of versions from newest first to old.
   */
  def expiredVersions(vers: Vector[String], cutoffDate: Instant)(f: String => Instant): Vector[String] =
    {
      val cache = mutable.Map[String, Instant]()
      def cachedLookup(ver: String): Instant =
        cache.getOrElseUpdate(ver, f(ver))
      def doExpiredVersions(startIdx: Int, endIdx: Int): Vector[String] =
        if (startIdx > endIdx) Vector.empty
        else {
          val startVerExpired = !cachedLookup(vers(startIdx)).isAfter(cutoffDate)
          val endVerExpired = !cachedLookup(vers(endIdx)).isAfter(cutoffDate)
          if (startVerExpired) vers.slice(startIdx, endIdx + 1)
          else if (!endVerExpired) Vector.empty
          else {
            val midIdx = (startIdx + endIdx) / 2
            doExpiredVersions(startIdx, midIdx - 1) ++ doExpiredVersions(midIdx, endIdx)
          }
        }
      doExpiredVersions(0, vers.size - 1)
    }
}
