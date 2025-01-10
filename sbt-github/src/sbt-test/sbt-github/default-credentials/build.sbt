val check = taskKey[Unit]("check")


resolvers += "github-packages-tests" at "https://maven.pkg.github.com/er1c/github-packages-tests"
name := "default-credentials"
libraryDependencies += "com.example" % "java-project-example" % "0.1.0"
check := (Def.task {
  val creds = credentials.value
  assert(creds.nonEmpty, "credentials were empty")
  creds.foreach{
    case cred: DirectCredentials =>
      assert(cred.host == "maven.pkg.github.com", "Didn't get maven.pkg.github.com credentials")
    case cred: FileCredentials => ()
  }
  ()
}).value