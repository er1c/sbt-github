sbt-github-remote-cache
========================

requirements
------------

- an account on [github](https://github.com) (get one [here](https://github.com/signup))
- a desire to build a zero-second build

setup
-----

Add the following to your sbt `project/plugins.sbt` file:

[![GitHub Remote Cache Version](https://maven-badges.herokuapp.com/maven-central/io.github.er1c/sbt-sbt-github-remote-cache_2.12_1.0/badge.svg)](https://search.maven.org/search?q=g:io.github.er1c%20AND%20a:sbt-github-remote-cache_2.12_1.0)

```scala
addSbtPlugin("io.github.er1c" % "sbt-github-remote-cache" % "x.x.x")
```

### GitHub repo and package

Go to `https://github.com/<your_github_user>/` and create a new **Generic** repository with the name **`remote-cache`**.

Next, create a _package_ within the remote-cache repo. The granularity should typically be one package for one build.

### build.sbt

Then in your `build.sbt`:

```scala
ThisBuild / githubRemoteCacheOwner := "your_github_user or organization"
ThisBuild / githubRemoteCacheOwnerType := GitHubOwnerType.User // default value, or GitHubOwnerType.Organization 
ThisBuild / githubRemoteCacheRepository := "remote-cache" // default value
ThisBuild / githubRemoteCacheMinimum := 100 // default value
ThisBuild / githubRemoteCacheTtl := Duration(30, DAYS) // default value
```

usage
-----

### credentials

To push remote cache, you need to provide GitHub credentials (username and API key) using a credential file or environment variables.

1. Credentials file

sbt-github-remote-cache will look for a credentials file under `~/.github/.credentials` used to authenticate publishing requests to github.

```
realm = GitHub API Realm
host = api.github.com
user = <github user>
password = <api_token>
```

2.  Properties

You can pass the user and pass as JVM properties when starting sbt:

    sbt -Dgithub.remote.cache.user=yourgithubUser -Dgithub.remote.cache.token=yourgithubApiToken

3. Environment variables

sbt-github-remote-cache will look for github user and pass in the environment variables `GITHUB_REMOTE_CACHE_USER` and  `GITHUB_REMOTE_CACHE_TOKEN`. Note that these are different from sbt-github.

### pushing remote cache

From the CI machine, run

```
> pushRemoteCache
```

### cleaning up the old cache

From the CI machine, run

```
> githubRemoteCacheCleanOld
```

This will **remove** versions older than a month, while keeping minimum 100 cache entry.
