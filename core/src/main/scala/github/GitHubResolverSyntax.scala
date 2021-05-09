package github

import java.net.URL
import org.apache.ivy.plugins.repository.Repository
import sbt.{MavenRepository, Resolver}
import org.apache.ivy.plugins.resolver.{IBiblioResolver, URLResolver}

object GitHubResolverSyntax {
  private[github] def makeGitHubRepoName(owner: String, repo: String): String =
    s"github-$owner-$repo"

  private[github] def makeGitHubUrl(owner: String, repo: String): String =
    s"https://maven.pkg.github.com/$owner/$repo"

  private[github] def makeMavenRepository(owner: String, repo: String): MavenRepository =
    MavenRepository(makeGitHubRepoName(owner, repo), makeGitHubUrl(owner, repo))

  implicit class RichGitHubResolver(val resolver: sbt.Resolver.type) extends AnyVal {
    def githubRepo(owner: String, repo: String) = makeMavenRepository(owner, repo)
    def githubIvyRepo(owner: String, repo: String) =
      Resolver.url(s"github-$owner-$repo", new URL(s"https://maven.pkg.github.com/$owner/$repo"))(Resolver.ivyStylePatterns)
  }
}

trait GitHubResolverSyntax {
  import GitHubResolverSyntax._

  implicit def toGitHubResolverSyntax(resolver: sbt.Resolver.type): RichGitHubResolver =
    new RichGitHubResolver(resolver)
}

//case class GitHubMavenResolver(
//  owner: String,
//  repo: String,
//  release: Boolean,
//  ignoreExists: Boolean
//) extends IBiblioResolver {
//  import GitHubResolverSyntax._
//
//  setName(makeGitHubRepoName(owner, repo))
//  setM2compatible(true)
//  setRoot(makeGitHubUrl(owner, repo))
//
//  override def setRepository(repository: Repository): Unit =
//    super.setRepository(GitHubResolverSyntax.makeMavenRepository(owner, name))
//}