githubOwner := "er1c"

TaskKey[Unit]("check") := {
  val whoami = githubWhoami.value
  if (whoami != "username") sys.error(s"unexpected whoami output: $whoami")
  ()
}
