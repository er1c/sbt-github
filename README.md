sbt-github
----------
[![Continuous Integration](https://github.com/er1c/sbt-github/actions/workflows/ci.yml/badge.svg)](https://github.com/er1c/sbt-github/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.er1c/sbt-github/badge.svg)](https://search.maven.org/search?q=g:io.github.er1c%20AND%20a:sbt-github)


Forking/refactoring [`sbt-bintray`](https://github.com/sbt/sbt-bintray) for GitHub support

- Requirements: sbt `1.2.1` or later.

See [remote caching](REMOTE_CACHE.md) for information about sbt-github-remote-cache.

## Consuming or publishing?

```scala
resolvers +=  "sbt-github-releases" at "https://maven.pkg.github.com/er1c/sbt-github"
credentials += Credentials("GitHub Package Registry", "maven.pkg.github.com", "<github-user>", "<GITHUB_TOKEN>")
```

If you want to _publish_ to GitHub, read on.

## Install

### What you need


Add the following to your sbt `project/plugins.sbt` file:

```scala
addSbtPlugin("io.github.er1c" % "sbt-github" % "0.2.0")
```

## Usage

Note that when specifying `sbt-github` settings in `project/*.scala` files (as opposed to in `build.sbt`), you will need to add the following import:

```scala
import github.GitHubPluginKeys._
```

### GitHub User

```scala
ThisBuild / githubOwner := "er1c"
ThisBuild / githubOwnerType := GitHubOwnerType.User
ThisBuild / githubRepository := "sbt-github"
```

### GitHub Organization

```scala
ThisBuild / githubOwner := "sbt"
ThisBuild / githubOwnerType := GitHubOwnerType.Organization
ThisBuild / githubRepository := "io"
```

### Publishing

To publish a package to github, you need a github account. You can register for one [here](https://github.com/signup/index). 
`GitHubPlugin` is an auto plugin that will be added to all projects in your build.
This plugin will upload and release your artifacts into github when you run `publish`.

To exclude a project from being published (for example a root or a tests project) use the `skip` setting:

```scala
publish / skip := true
```

At any time you can check who you will be authenticated as with the `githubWhoami` setting which will print your github username

    > githubWhoami

#### Credentials

To publish, you need to provide github credentials (user name and API key). There are three ways to set them up: credential file, properties, and environment variables.

1. Credentials file

sbt-github will look for a credentials file under `~/.github/.credentials` used to authenticate publishing requests to github.

```
realm = GitHub API Realm
host = api.github.com
user = <github user>
password = <api_token>
```

You can interactively set up or change the github credentials used by sbt anytime with

    > githubChangeCredentials

Note you will need to `reload` your project afterwards which will reset your `publishTo` setting.

2.  Properties

You can pass the user and pass as JVM properties when starting sbt:

    sbt -Dgithub.user=yourgithubUser -Dgithub.pass=yourgithubPass
    
3. Environment variables

sbt-github will look for github user and pass in the environment variables `github_USER` and  `github_PASS`.

#### github organization

You may optionally wish to publish to a [github organization](https://github.com/docs/usermanual/interacting/interacting_githuborganizations.html)
instead of your individual github user account. To do so, use the `githubOrganization` setting in your project's build definition.

```scala
githubOwner := Some("strength-in-numbers")
githubOwnerType := GitHubOwnerType.Organization
```

The default GitHub repository, for a github user or organization is named `maven`. If your Maven repository is named differently, you will need to specify the `githubRepository` setting.

```scala
githubRepository := "oss-maven"
```

##### Public (default)

If your project uses a license, github supports several [OSS licenses](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository). If you are new to software licenses you may
want to grab a coffee and absorb some [well organized information](http://choosealicense.com/) on the topic of choice.
Sbt already defines a `licenses` setting key. In order to use github sbt you must define your `licenses` key to contain a license with a name matching
one of those github defines. I recommend [MIT](http://choosealicense.com/licenses/mit/).

```scala
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
```

### Unpublishing

It's generally a bad practice to remove a version of a library others may depend on but sometimes you may want test a release with the ability to immediately take it back down if something goes south before others start depending on it. github allows for this flexibility and thus, sbt-github does as well. Use the `unpublish` task to unpublish the current version from github.

    > githubUnpublish

### Finding your way around

The easiest way to learn about sbt-github is to use the sbt shell typing `github<tab>` or `help github` to discover github keys.

## Acknowledgments

This plugin was first created by Doug Tangren (softprops), 2013-2014.

The plugin is now maintained by [er1c](https://github.com/er1c).
