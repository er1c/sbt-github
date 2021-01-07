package github

import sbt._
import java.io.File

case class GitHubToken(apiKey: String) {
  override def toString = s"GitHubToken(${"x"*apiKey.size})"
}

object GitHubToken {

  val Keys = Seq("realm", "host", /*"user",*/ "token")
//  def templateSrc(realm: String, host: String)(
//    name: String, password: String) =
//    s"""realm = $realm
//       |host = $host
//       |user = $name
//       |password = $password""".stripMargin

  /** github api */
  object api {
    def toDirect(t: GitHubToken) =
      sbt.Credentials(Realm, Host, t.apiKey, t.apiKey)
    val Host = "api.github.com"
    val Realm = "GitHub API Realm"
    //val template = templateSrc(Realm, Host)_
  }

  /** sonatype oss (for mvn central sync) */
  object sonatype {
    val Host = "oss.sonatype.org"
    val Realm = "Sonatype Nexus Repository Manager"
    //val template = templateSrc(Realm, Host)_
  }

  def read(path: File): Option[GitHubToken] =
    path match {
      case creds if creds.exists =>
        import scala.collection.JavaConverters._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val mapped = properties.asScala.map {
          case (k,v) => (k.toString, v.toString.trim)
        }.toMap
        val missing = Keys.filter(!mapped.contains(_))
        if (!missing.isEmpty) None
        else Some(GitHubToken(mapped("token")))
      case _ => None
    }

//  def writeGitHub(
//    user: String, password: String, path: File) =
//    IO.write(path, api.template(user, password))
//
//  def writeSonatype(
//    user: String, password: String, path: File) =
//    IO.write(path, sonatype.template(user, password))
}
