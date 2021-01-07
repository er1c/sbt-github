package github

import java.io.File

/**
 * This abstract which environmental variable will be used by which plugin.
 */
case class GitHubCredentialContext(
  credsFile: File,
  tokenProp: String,
  tokenEnv: String,
)

object GitHubCredentialContext {
  def apply(credsFile: File): GitHubCredentialContext =
    GitHubCredentialContext(
      credsFile,
      "github.token",
      "GITHUB_TOKEN",
    )

  def remoteCache(credsFile: File): GitHubCredentialContext =
    GitHubCredentialContext(
      credsFile,
      "github.remote.cache.token",
      "GITHUB_REMOTE_CACHE_TOKEN",
    )
}
