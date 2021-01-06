package github

import java.io.File

/**
 * This abstract which environmental variable will be used by which plugin.
 */
case class GitHubCredentialContext(
  credsFile: File,
  userNameProp: String,
  passProp: String,
  userNameEnv: String,
  passEnv: String
)

object GitHubCredentialContext {
  def apply(credsFile: File): GitHubCredentialContext =
    GitHubCredentialContext(
      credsFile,
      "github.user",
      "github.pass",
      "BINTRAY_USER",
      "BINTRAY_PASS"
    )

  def remoteCache(credsFile: File): GitHubCredentialContext =
    GitHubCredentialContext(
      credsFile,
      "github.remote.cache.user",
      "github.remote.cache.pass",
      "BINTRAY_REMOTE_CACHE_USER",
      "BINTRAY_REMOTE_CACHE_PASS"
    )
}
