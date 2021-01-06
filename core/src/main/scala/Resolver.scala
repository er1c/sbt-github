package github

import java.io.File
import java.net.URL

import bintry.Client
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.plugins.repository.{AbstractRepository, Repository, Resource}
import org.apache.ivy.plugins.resolver.{IBiblioResolver, URLResolver}
import sbt.Resolver.{ivyStylePatterns, mavenStylePatterns}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

sealed abstract class AbstractGitHubRepository(underlying: Repository)
  extends AbstractRepository with DispatchHandlers {

  override def getResource(src: String): Resource = underlying.getResource(src)

  override def get(src: String, dest: File): Unit = underlying.get(src, dest)

  override def list(parent: String): java.util.List[_] = underlying.list(parent)
}

case class GitHubGenericRepository(
  underlying: Repository,
  ver: Client#Repo#Package#Version,
  release: Boolean)
  extends AbstractGitHubRepository(underlying) {

  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit =
    Await.result(
      ver.upload(dest, src).publish(release)(asStatusAndBody),
      Duration.Inf) match {
      case (201, _) =>
      case (_, fail) =>
        throw new RuntimeException(s"error uploading to $dest: $fail")
    }
}

case class GitHubMavenRepository(
  underlying: Repository,
  pkg: Client#Repo#Package,
  release: Boolean,
  ignoreExists: Boolean)
  extends AbstractGitHubRepository(underlying) {

  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit =
    Await.result(
      pkg.mvnUpload(transform(dest), src).publish(release)(asStatusAndBody),
      Duration.Inf) match {
      case (201, _) =>
      case (_, fail) =>
        if (ignoreExists && (fail contains "already exists")) ()
        else throw new RuntimeException(s"error uploading to $dest: $fail")
    }

  /** transforms a full url like
    *  https://api.github.com/maven/:subject/maven/:name/me/lessis/:name_2.10/0.1.0/:name_2.10-0.1.0.pom
    *  into a path like
    *  me/lessis/:name_2.10/0.1.0/:name_2.10-0.1.0.pom
    */
  private def transform(dest: String) =
    new URL(dest).getPath.split('/').drop(5).mkString("/")
}

case class GitHubIvyResolver(
  name: String,
  ver: Client#Repo#Package#Version,
  release: Boolean)
  extends URLResolver {

  setName(name)
  setM2compatible(false)
  setArtifactPatterns(ivyStylePatterns.artifactPatterns.asJava)

  override def setRepository(repository: Repository): Unit =
    super.setRepository(GitHubGenericRepository(repository, ver, release))
}

case class GitHubMavenResolver(
  name: String,
  rootURL: String,
  ver: Client#Repo#Package,
  release: Boolean,
  ignoreExists: Boolean
)
  extends IBiblioResolver {

  setName(name)
  setM2compatible(true)
  setRoot(rootURL)

  override def setRepository(repository: Repository): Unit =
    super.setRepository(GitHubMavenRepository(repository, ver, release, ignoreExists))
}

case class GitHubMavenSbtPluginResolver(
  name: String,
  ver: Client#Repo#Package#Version,
  release: Boolean)
  extends URLResolver {

  setName(name)
  setM2compatible(true)
  setArtifactPatterns(mavenStylePatterns.artifactPatterns.asJava)

  override def setRepository(repository: Repository): Unit =
    super.setRepository(GitHubGenericRepository(repository, ver, release))
}
