package github

import sbt._
import java.io.File

case class GitHubCredentials(user: String, token: String) {
  override def toString = s"GitHubCredentials($user, ${"x"*token.size})"
}

object GitHubCredentials {

  val Keys = Seq("realm", "host", "user", "password")
  def templateSrc(realm: String, host: String)(user: String, password: String): String =
    s"""realm = $realm
       |host = $host
       |user = $user
       |password = $password""".stripMargin

  /** github api */
  object api {
    def toDirect(bc: GitHubCredentials) =
      sbt.Credentials(Realm, Host, bc.user, bc.token)

    val Host = "api.github.com"
    val Realm = "GitHub API Realm"
    val template = templateSrc(Realm, Host)_
  }


  //  /** sonatype oss (for mvn central sync) */
  //  object sonatype {
  //    val Host = "oss.sonatype.org"
  //    val Realm = "Sonatype Nexus Repository Manager"
  //    val template = templateSrc(Realm, Host)_
  //  }

  def read(path: File): Option[GitHubCredentials] = {
    path match {
      case creds if creds.exists =>
        import scala.collection.JavaConverters._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val mapped = properties.asScala.map {
          case (k,v) => (k, v.trim)
        }.toMap

        val missing = Keys.filter(!mapped.contains(_))

        if (!missing.isEmpty) None
        else Some(GitHubCredentials(mapped("user"), mapped("password")))
      case _ => None
    }
  }

  def writeGitHub(user: String, token: String, path: File): Unit =
    IO.write(path, api.template(user, token))

  //  def writeSonatype(user: String, token: String, path: File) =
  //    IO.write(path, sonatype.template(user, password))
}