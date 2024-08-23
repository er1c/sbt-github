resolvers += "github-packages-tests" at "https://maven.pkg.github.com/er1c/github-packages-tests"
githubTokenSource := TokenSource.Environment("GITHUB_TOKEN_FOR_ENV_TEST")
name := "default-credentials"
libraryDependencies += "com.example" % "java-project-example" % "0.1.0"
