package github

import java.io.File

/**
 * This abstract which environmental variable will be used by which plugin.
 */
case class GitHubCredentialContext(
  credsFile: File,
  tokenSource: TokenSource
)