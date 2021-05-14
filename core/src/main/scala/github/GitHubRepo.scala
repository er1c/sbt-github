package github

import sbt._
import caliban.client.SelectionBuilder
import caliban.client.github.Client.DeletePackageVersionPayload
import sbt.Keys._
import scala.concurrent.duration.Duration
import GitHubHelpers._

object GitHubRepo {
  import caliban.client.github.Client._

  private val packageVersionsBuilder: SelectionBuilder[PackageVersionConnection, List[String]] =
    PackageVersionConnection.nodes(PackageVersion.version).map(flattenToList)

  private val packageVersionIdsBuilder: SelectionBuilder[PackageVersionConnection, List[(String, String)]] =
    PackageVersionConnection.nodes(PackageVersion.id ~ PackageVersion.version).map(flattenToList)


  private val packageBuilder: SelectionBuilder[PackageConnection, List[GitHubPackage]] =
    PackageConnection.nodes(
      (
        Package.id ~
          Package.latestVersion(PackageVersion.version) ~
          Package.name ~
          //Package.repository()
          Package.packageType ~
          Package.statistics(PackageStatistics.downloadsTotalCount).map(_.getOrElse(0)) ~
          Package.versions(last = Some(100))(packageVersionsBuilder)
      ).mapN(GitHubPackage)
    ).map(flattenToList)

  case class GitHubPackage(
    id: String,
    latestVersion: Option[String],
    name: String,
    //repo: String,
    packageType: PackageType,
    //repository
    downloadsTotalCount: Int,
    versions: List[String],

//    owner: String,
//    desc: Option[String],
//    labels: List[String],
//    attrNames: List[String],
//    followers: Int,
//    created: String,
//    updated: String,
//    web: Option[String],
//    issueTracker: Option[String],
//    githubRepo: Option[String],
//    vcs: Option[String],
//    githubReleaseNotes: String,
//    publicDownloadNumbers: Boolean,
//    links: List[String],
//    versions: List[String],
//    latestVersion: Option[String],
//    rating: Int,
//    systemIds: List[String]
  )
}
case class GitHubRepo(credentials: GitHubCredentials, owner: Option[String], repoName: String) extends GitHubHelpers {
  import caliban.client.github.Client._
  import GitHubRepo._

  /**
   * Lists versions of github packages corresponding to the current project
   *  Example packages: https://github.com/er1c?tab=packages&repo_name=github-packages-tests
   * @param packageNames Sequence of github package names (e.g. `com.example.cross-platform-example_sjs1_2.11`)
   * @param log Sbt Logger
   * @return
   */
  def packageVersions(
    githubPackageName: String,
    log: Logger,
  ): Seq[String] = {
    log.info(s"GitHubRepo($credentials, $owner, $repoName).packageVersions($githubPackageName)")

    val query = Query.repository(name = repoName, owner = owner.getOrElse(credentials.user)) {
      Repository.packages(
        last = Some(50),
        names = Some(List(Some(githubPackageName))),
        packageType = Some(PackageType.MAVEN),
      )(packageBuilder)
    }

    val packages: List[GitHubPackage] = get(query).getOrElse(Nil)
    assert(packages.isEmpty || packages.size == 1, s"Unexpected multiple package results: $packages")
    packages.flatMap { _.versions }
  }

  // https://github.com/marketplace/actions/delete-package-versions


  def buildPublishResolver(mvnStyle: Boolean): Resolver =
    GitHub.publishTo(owner.getOrElse(credentials.user), repoName, mvnStyle)

//  def buildRemoteCacheResolver(packageName: String, log: Logger): Resolver = {
//    val pkg = repo.get(packageName)
//    GitHub.remoteCache(repo, pkg)
//  }
//
//  def upload(packageName: String, vers: String, path: String, f: File, log: Logger): Unit =
//    await.result(repo.get(packageName).version(vers).upload(path, f)(asStatusAndBody)) match {
//      case (201, _) => log.info(s"$f was uploaded to $owner/$packageName@$vers")
//      case (_, fail) => sys.error(s"failed to upload $f to $owner/$packageName@$vers: $fail")
//    }
//
//  def release(packageName: String, vers: String, log: Logger): Unit =
//    await.result(repo.get(packageName).version(vers).publish(asStatusAndBody)) match {
//      case (200, _) => log.info(s"$owner/$packageName@$vers was released")
//      case (_, fail) => sys.error(s"failed to release $owner/$packageName@$vers: $fail")
//    }

  /** unpublish (delete) a version of a package */
  def unpublish(githubPackageName: String, version: String, log: Logger): Unit = {
    log.info(s"GitHubRepo($credentials, $owner, $repoName).packageVersions($githubPackageName)")

    val packageVersions = Query.repository(name = repoName, owner = owner.getOrElse(credentials.user)) {
      Repository.packages(
        first = Some(1),
        names = Some(List(Some(githubPackageName))),
        packageType = Some(PackageType.MAVEN),
      )(
        PackageConnection.nodes(
          Package.versions(last = Some(100))(packageVersionIdsBuilder)
        ).map(flattenToList)
      ).map(flattenToList)
    }

    val packageVersionIdAndVersion: List[(String, String)] = {
      get(packageVersions).getOrElse(Nil)
    }

    packageVersionIdAndVersion.collect {
      case (versionId, v) if v == version => versionId
    }.foreach { versionId =>
      val query = Mutation
        .deletePackageVersion(DeletePackageVersionInput(packageVersionId = versionId)) {
          DeletePackageVersionPayload.success
        }

      get(query).map { success =>
        if (!success.getOrElse(false)) sys.error(s"Error deleting $version ($versionId) for $githubPackageName")
        else log.info(s"Successfully deleted $version for $githubPackageName")
      }
    }
  }

  //  /** Request pgp credentials from the environment in the following order:
//   *
//   *  1. From system properties.
//   *  2. From system environment variables.
//   *  3. From the github cache.
//   *
//   * This function behaves in the same way as `requestSonatypeCredentials`.
//   */
//  def requestPgpCredentials: Option[String] = {
//    sys.props.get("pgp.pass")
//      .orElse(sys.env.get("PGP_PASS"))
//      .orElse(Cache.get("pgp.pass"))
//  }
//
//  /** pgp sign remotely published artifacts then publish those signed artifacts.
//   *  this assumes artifacts are published remotely. signing artifacts doesn't
//   *  mean the signings themselves will be published so it is nessessary to publish
//   *  this immediately after.
//   */
//  def remoteSign(packageName: String, vers: String, log: Logger): Unit =
//  {
//    val btyVersion = repo.get(packageName).version(vers)
//    val passphrase = requestPgpCredentials
//      .orElse(Prompt.descretely("Enter pgp passphrase"))
//      .getOrElse(sys.error("pgp passphrase is required"))
//    val (status, body) = await.result(
//      btyVersion.sign(passphrase)(asStatusAndBody))
//    if (status == 200) {
//      // we want to only ask for pgp credentials once for a given sbt session
//      // so let's cache them for later use in the session after we're reasonable
//      // sure they are valid
//      Cache.put(("pgp.pass", passphrase))
//      log.info(s"$owner/$packageName@$vers was signed")
//      // after signing the remote artifacts, they remain
//      // unpublished (not available for download)
//      // we are opting to publish those unpublished
//      // artifacts here
//      val (pubStatus, pubBody) = await.result(
//        btyVersion.publish(asStatusAndBody))
//      if (pubStatus != 200) sys.error(
//        s"failed to publish signed artifacts for $owner/$packageName@$vers: $pubBody")
//    }
//    else sys.error(s"failed to sign $owner/$packageName@$vers: $body")
//  }
//
//  //  /** synchronize a published set of artifacts for a pkg version to mvn central
//  //   *  this requires already having a sonatype oss account set up.
//  //   *  this is itself quite a task but in the case the user has done this in the past
//  //   *  this can be quiet a convenient feature */
//  //  def syncMavenCentral(packageName: String, vers: String, creds: Seq[Credentials], close: Boolean, retryDelays: Seq[Duration], log: Logger): Unit =
//  //    {
//  //      val btyVersion = repo.get(packageName).version(vers)
//  //      val GitHubCredentials(sonauser, sonapass) = resolveSonatypeCredentials(creds)
//  //      Retry.withDelays(log, retryDelays) {
//  //        await.result(
//  //          btyVersion.mavenCentralSync(sonauser, sonapass, close)(asStatusAndBody)) match {
//  //          case (200, body) =>
//  //            // store these sonatype credentials in memory for the remainder of the sbt session
//  //            Cache.putMulti(
//  //              ("sona.user", sonauser), ("sona.pass", sonapass))
//  //            log.info(s"$owner/$packageName@$vers was synced with maven central")
//  //            log.info(body)
//  //          case (404, body) =>
//  //            log.info(s"$owner/$packageName@$vers was not found. try publishing this package version to github first by typing `publish`")
//  //            log.info(s"body $body")
//  //          case (_, body) =>
//  //            // ensure these items are removed from the cache, they are probably bad
//  //            Cache.removeMulti("sona.user", "sona.pass")
//  //            sys.error(s"failed to sync $owner/$packageName@$vers with maven central: $body")
//  //        }
//  //      }
//  //    }
//
//  //  private def resolveSonatypeCredentials(
//  //    creds: Seq[sbt.Credentials]): GitHubCredentials =
//  //    Credentials.forHost(creds, GitHubCredentials.sonatype.Host)
//  //      .map { d => (d.userName, d.passwd) }
//  //      .getOrElse(requestSonatypeCredentials) match {
//  //        case (user, pass) => GitHubCredentials(user, pass)
//  //      }
//
//  //  /** Search Sonatype credentials in the following order:
//  //   *  1. Cache
//  //   *  2. System properties
//  //   *  3. Environment variables
//  //   *  4. User input */
//  //  private def requestSonatypeCredentials: (String, String) = {
//  //    val cached = Cache.getMulti("sona.user", "sona.pass")
//  //    (cached("sona.user"), cached("sona.pass")) match {
//  //      case (Some(user), Some(pass)) =>
//  //        (user, pass)
//  //      case _ =>
//  //        val propsCredentials = for (name <- sys.props.get("sona.user"); pass <- sys.props.get("sona.pass")) yield (name, pass)
//  //        propsCredentials match {
//  //          case Some((name, pass)) => (name, pass)
//  //          case _ =>
//  //            val envCredentials = for (name <- sys.env.get("SONA_USER"); pass <- sys.env.get("SONA_PASS")) yield (name, pass)
//  //            envCredentials.getOrElse {
//  //              val name = Prompt("Enter sonatype username").getOrElse {
//  //                sys.error("sonatype username required")
//  //              }
//  //              val pass = Prompt.descretely("Enter sonatype password").getOrElse {
//  //                sys.error("sonatype password is required")
//  //              }
//  //              (name, pass)
//  //            }
//  //        }
//  //    }
//  //  }
//

//
//  def packageVersionUpdatedDate(packageName: String, version: String): Instant =
//  {
//    import _root_.org.json4s._
//    val ver = repo.get(packageName).version(version)
//    await.result(ver(EitherHttp({ _ => JNothing}, as.json4s.Json))).fold({ _ =>
//      sys.error("version does not exist")
//    }, { js =>
//      for {
//        JObject(fs)                   <- js
//        ("updated", JString(updated)) <- fs
//      } yield Instant.parse(updated)
//    }).head
//  }
//
  def cleandOldVersions(packageName: String, min: Int, ttl: Duration, log: Logger): Unit = {
//    val vers0 = packageVersions(packageName, log)
//    val vers = vers0.drop(min)
//    if (vers.isEmpty || !ttl.isFinite) ()
//    else {
//      val expirationDate = Instant.now.minusSeconds(ttl.toSeconds)
//      val expiredVersions = GitHubRepo.expiredVersions(vers.toVector, expirationDate)(packageVersionUpdatedDate(packageName, _))
//      log.info(s"about to delete $expiredVersions")
//      expiredVersions foreach { ver =>
//        unpublish(packageName, ver, log)
//      }
//    }

    ???
  }
//}
//
//object GitHubRepo {
//  /**
//   * Return expired versions on or before the cutoffDate.
//   * vers is expected to contain a sequence of versions from newest first to old.
//   */
//  def expiredVersions(vers: Vector[String], cutoffDate: Instant)(f: String => Instant): Vector[String] =
//  {
//    val cache = mutable.Map[String, Instant]()
//    def cachedLookup(ver: String): Instant =
//      cache.getOrElseUpdate(ver, f(ver))
//    def doExpiredVersions(startIdx: Int, endIdx: Int): Vector[String] =
//      if (startIdx > endIdx) Vector.empty
//      else {
//        val startVerExpired = !cachedLookup(vers(startIdx)).isAfter(cutoffDate)
//        val endVerExpired = !cachedLookup(vers(endIdx)).isAfter(cutoffDate)
//        if (startVerExpired) vers.slice(startIdx, endIdx + 1)
//        else if (!endVerExpired) Vector.empty
//        else {
//          val midIdx = (startIdx + endIdx) / 2
//          doExpiredVersions(startIdx, midIdx - 1) ++ doExpiredVersions(midIdx, endIdx)
//        }
//      }
//    doExpiredVersions(0, vers.size - 1)
//  }
}