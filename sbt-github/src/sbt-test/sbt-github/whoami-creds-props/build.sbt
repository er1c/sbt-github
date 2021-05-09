githubOwner := "er1c"

ThisBuild / githubCredentialsFile := {
  val f = baseDirectory.value / "credentials"
  assert(f.isFile, s"didn't find credentials file: $f")
  f
}

TaskKey[Unit]("check") := {
  val whoami = githubWhoami.value
  if (whoami != "username") sys.error(s"unexpected whoami output: $whoami, ${githubCredentialsFile.value}")
  ()
}
