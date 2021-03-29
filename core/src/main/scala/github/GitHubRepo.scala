package github

import sbt._
import caliban.client.SelectionBuilder

object GitHubRepo {
  import caliban.client.github.Client._
  import GitHubHelpers._

  private val packageVersionsBuilder: SelectionBuilder[PackageConnection, List[String]] =
    PackageConnection.nodes(Package.name).map(flattenToList)
}
case class GitHubRepo(credentials: GitHubCredentials, owner: String, repoName: String) extends GitHubHelpers {
  import caliban.client.github.Client._
  import GitHubRepo._

  /** Lists versions of github packages corresponding to the current project */
  def packageVersions(packageName: String, log: Logger): Seq[String] = {
    log.debug(s"${this.toString}.packageVersions($packageName)")
    val query = Query.repository(name = repoName, owner = owner) {
      Repository.packages(
        last = Some(50),
        // com.example.github-packages-tests_2.13
        //names = Some(List(Some(packageName))),
        packageType = Some(PackageType.MAVEN)
      )(packageVersionsBuilder)
    }

    get(query).getOrElse(Nil)
  }

//  def buildPublishResolver(
//    packageName: String,
//    vers: String,
//    mvnStyle: Boolean,
//    isSbtPlugin: Boolean,
//    isRelease: Boolean,
//    log: Logger
//  ): Resolver = {
//    val query = Query.repository(owner, repoName)
//    val pkg = repo.get(packageName)
//    // warn the user that github expects maven published artifacts to be published to the `maven` repo
//    // but they have explicitly opted into a publish style and/or repo that
//    // deviates from that expectation
//    if (GitHub.defaultMavenRepository == repo.repo && !mvnStyle) log.info(
//      "you have opted to publish to a repository named 'maven' but publishMavenStyle is assigned to false. This may result in unexpected behavior")
//    GitHub.publishTo(repo, pkg, vers, mvnStyle, isSbtPlugin, isRelease)
//  }
//
//  def buildRemoteCacheResolver(packageName: String, log: Logger): Resolver =
//  {
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
//
//  /** unpublish (delete) a version of a package */
//  def unpublish(packageName: String, vers: String, log: Logger): Unit =
//    await.result(repo.get(packageName).version(vers).delete(asStatusAndBody)) match {
//      case (200, _) => log.info(s"$owner/$packageName@$vers was discarded")
//      case (404, _) => log.warn(s"$owner/$packageName@$vers was not found")
//      case (_, fail) => sys.error(s"failed to discard $owner/$packageName@$vers: $fail")
//    }
//
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
//  def cleandOldVersions(packageName: String, min: Int, ttl: Duration, log: Logger): Unit =
//  {
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
//  }
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