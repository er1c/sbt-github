
lazy val mavenStyle = (project in file("maven-style"))
  .settings(
    version := "0.1",
    scalaVersion := "2.10.6",
    publishMavenStyle := true,
    githubCredentialsFile := {
      val f = baseDirectory.value / ".." / "credentials"
      assert(f.isFile, s"didn't find credentials file: $f")
      f
    },
    githubOwner := "tinyrick",
    githubRepository := "evilmorty",
    TaskKey[Unit]("check") := {
      assert((sbtgithub / resolvers).value.filter(_.name.equals("github-tinyrick-evilmorty")).head.isInstanceOf[MavenRepository],
        "A maven style project should have a maven repository as it default resolver"
      )
    }
  )

//// TODO: GitHub Packages doesn't support ivy-style
//lazy val ivyStyle = (project in file("ivy-style"))
//  .settings(
//    version := "0.1",
//    scalaVersion := "2.10.6",
//    publishMavenStyle := false,
//    githubOwner := "tinyrick",
//    githubRepository := "evilmorty",
//    // TODO: Maybe this can get set to empty rather than throwing an error
//    TaskKey[Unit]("check") := {
//      assert(resolvers.value.filter(_.name.equals("github-tinyrick-evilmorty")).isEmpty,
//        "An ivy style project should not have any resolvers"
//      )
//    }
//  )