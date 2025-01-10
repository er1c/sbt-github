{
  val pluginVersion = System.getProperty("plugin.version")
  if (pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("io.github.er1c" % "sbt-github" % pluginVersion)
}

//resolvers += "github-packages-tests" at "https://maven.pkg.github.com/er1c/github-packages-tests"
