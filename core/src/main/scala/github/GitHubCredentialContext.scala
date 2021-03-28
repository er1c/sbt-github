package github

import java.io.File

/**
 * This abstract which environmental variable will be used by which plugin.
 */
case class GitHubCredentialContext(
  credsFile: File,
  userProp: String,
  tokenProp: String,
  userEnv: String,
  tokenEnv: String
)

object GitHubCredentialContext {
  def apply(credsFile: File): GitHubCredentialContext =
    GitHubCredentialContext(
      credsFile,
      "github.user",
      "github.token",
      "GITHUB_USER",
      "GITHUB_TOKEN"
    )

  def remoteCache(credsFile: File): GitHubCredentialContext =
    GitHubCredentialContext(
      credsFile,
      "github.remote.cache.user",
      "github.remote.cache.token",
      "GITHUB_REMOTE_CACHE_USER",
      "GITHUB_REMOTE_CACHE_TOKEN"
    )
}
