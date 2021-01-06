package testpkg

import verify._
import github._
import java.time.Instant

object GitHubRepoTest extends BasicTestSuite {
  test("empty expiredVersions") {
    val expired = GitHubRepo.expiredVersions(
      Vector.empty,
      Instant.parse("2020-04-01T00:00:00Z")
    )(fakeLookup(_))
    assert(expired == Vector.empty)
  }

  test("cutoff date includes all") {
    val expired = GitHubRepo.expiredVersions(
      Vector("2.0.0", "1.0.0", "0.5.0", "0.4.0", "0.3.0", "0.2.0", "0.1.0"),
      Instant.parse("2020-10-01T00:00:00Z")
    )(fakeLookup(_))
    assert(expired == Vector("2.0.0", "1.0.0", "0.5.0", "0.4.0", "0.3.0", "0.2.0", "0.1.0"))
  }

  test("cutoff date predates all") {
    val expired = GitHubRepo.expiredVersions(
      Vector("2.0.0", "1.0.0", "0.5.0", "0.4.0", "0.3.0", "0.2.0", "0.1.0"),
      Instant.parse("2020-03-01T00:00:00Z")
    )(fakeLookup(_))
    assert(expired == Vector.empty)
  }

  test("cutoff date 06-01") {
    val expired = GitHubRepo.expiredVersions(
      Vector("2.0.0", "1.0.0", "0.5.0", "0.4.0", "0.3.0", "0.2.0", "0.1.0"),
      Instant.parse("2020-06-01T00:00:00Z")
    )(fakeLookup(_))
    assert(expired == Vector("0.3.0", "0.2.0", "0.1.0"))
  }

  test("cutoff date 09-01") {
    val expired = GitHubRepo.expiredVersions(
      Vector("2.0.0", "1.0.0", "0.5.0", "0.4.0", "0.3.0", "0.2.0", "0.1.0"),
      Instant.parse("2020-09-01T00:00:00Z")
    )(fakeLookup(_))
    assert(expired == Vector("1.0.0", "0.5.0", "0.4.0", "0.3.0", "0.2.0", "0.1.0"))
  }

  lazy val fakeLookup = Map(
    "2.0.0" -> Instant.parse("2020-10-01T00:00:00Z"),
    "1.0.0" -> Instant.parse("2020-09-01T00:00:00Z"),
    "0.5.0" -> Instant.parse("2020-08-01T00:00:00Z"),
    "0.4.0" -> Instant.parse("2020-07-01T00:00:00Z"),
    "0.3.0" -> Instant.parse("2020-06-01T00:00:00Z"),
    "0.2.0" -> Instant.parse("2020-05-01T00:00:00Z"),
    "0.1.0" -> Instant.parse("2020-04-01T00:00:00Z"),
  )
}
