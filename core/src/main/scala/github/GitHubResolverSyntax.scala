package github

import java.net.URL
import sbt.{MavenRepository, Resolver}

object GitHubResolverSyntax {
  implicit class RichGitHubResolver(val resolver: sbt.Resolver.type) extends AnyVal {
    def githubRepo(owner: String, repo: String) =
      MavenRepository(s"github-$owner-$repo", s"https://maven.pkg.github.com/$owner/$repo")
    def githubIvyRepo(owner: String, repo: String) =
      Resolver.url(s"github-$owner-$repo", new URL(s"https://maven.pkg.github.com/$owner/$repo"))(Resolver.ivyStylePatterns)
  }
}

trait GitHubResolverSyntax {
  import GitHubResolverSyntax._

  implicit def toGitHubResolverSyntax(resolver: sbt.Resolver.type): RichGitHubResolver =
    new RichGitHubResolver(resolver)
}