package github

import java.net.URL
import sbt.{MavenRepository, Resolver, URLRepository}

object GitHubResolverSyntax {
  private[github] def makeGitHubRepoName(owner: String, repo: String): String =
    s"github-$owner-$repo"

  private[github] def makeGitHubUrl(owner: String, repo: String): String =
    s"https://maven.pkg.github.com/$owner/$repo"

  private[github] def makeMavenRepository(owner: String, repo: String): MavenRepository =
    MavenRepository(makeGitHubRepoName(owner, repo), makeGitHubUrl(owner, repo))

  private[github] def makeIvyRepository(owner: String, repo: String): URLRepository =
    Resolver.url(makeGitHubUrl(owner, repo), new URL(makeGitHubUrl(owner, repo)))(Resolver.ivyStylePatterns)

  implicit class RichGitHubResolver(val resolver: sbt.Resolver.type) extends AnyVal {
    def githubRepo(owner: String, repo: String): MavenRepository = makeMavenRepository(owner, repo)
    def githubIvyRepo(owner: String, repo: String): URLRepository = makeIvyRepository(owner, repo)
  }
}

trait GitHubResolverSyntax {
  import GitHubResolverSyntax._

  implicit def toGitHubResolverSyntax(resolver: sbt.Resolver.type): RichGitHubResolver =
    new RichGitHubResolver(resolver)
}
