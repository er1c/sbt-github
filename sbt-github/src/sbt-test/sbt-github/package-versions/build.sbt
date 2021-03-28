githubOwner := "er1c"
githubRepository := "github-packages-tests"
moduleName := "github-packages-tests"

TaskKey[Unit]("check") := {
  val versions = githubPackageVersions.value
  if (versions != List("com.example.github-packages-tests_2.13"))
    sys.error(s"unexpected githubPackageVersions output: $versions")
  ()
}
