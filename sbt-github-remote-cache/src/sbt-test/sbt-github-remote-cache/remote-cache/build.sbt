libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % Test

name := "sbt-github-remote-cache"
githubRemoteCacheOwner := "er1c"
githubRemoteCacheRepository := "sbt-github"
//ThisBuild / publishArtifact := false
////githubRemoteCachePackage := "sbt-github-remote-cache"
//ThisBuild / publish / skip := true