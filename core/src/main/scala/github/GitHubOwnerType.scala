package github

sealed trait GitHubOwnerType

object GitHubOwnerType {
  case object User extends GitHubOwnerType
  case object Org extends GitHubOwnerType
}