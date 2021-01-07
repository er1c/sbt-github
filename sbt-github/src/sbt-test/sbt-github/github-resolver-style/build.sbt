
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
    githubOwner := Some("tinyrick"),
    githubRepository := "evilmorty",
    TaskKey[Unit]("check") := {
      println(s"resolvers: ${resolvers.value}")
      assert(resolvers.value.filter(_.name.equals("github-tinyrick-evilmorty")).head.isInstanceOf[MavenRepository],
        "A maven style project should have a maven repository as it default resolver"
      )
    }
  )

// TODO: GitHub Packages doesn't support ivy-style
//lazy val ivyStyle = (project in file("ivy-style"))
//  .settings(
//    version := "0.1",
//    scalaVersion := "2.10.6",
//    publishMavenStyle := false,
//    githubOwner := Some("tinyrick"),
//    githubRepository := "evilmorty",
//    TaskKey[Unit]("check") := {
//      assert(resolvers.value.filter(_.name.equals("github-tinyrick-evilmorty")).head.isInstanceOf[URLRepository],
//        "An ivy style project should have a URL repository as it default resolver"
//      )
//    }
//  )