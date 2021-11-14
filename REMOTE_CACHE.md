sbt-github-remote-cache
========================

requirements
------------

- an account on [github](https://github.com) (get one [here](https://github.com/signup/oss))
- a desire to build a zero-second build

setup
-----

Add the following to your sbt `project/plugins.sbt` file:

![Bintray version](https://img.shields.io/github/v/sbt/sbt-plugin-releases/sbt-github.svg)

```scala
addSbtPlugin("io.github.er1c" % "sbt-github-remote-cache" % "0.1.0")
```

### GitHub repo and package

Go to `https://github.com/<your_github_user>/` and create a new **Generic** repository with the name **`remote-cache`**.

Next, create a _package_ within the remote-cache repo. The granularity should typically be one package for one build.

### build.sbt

Then in your `build.sbt`:

```scala
ThisBuild / githubRemoteCacheOrganization := "your_github_user or organization"
ThisBuild / githubRemoteCachePackage := "your_package_name"
```

usage
-----

### credentials

To push remote cache, you need to provide Bintray credentials (user name and API key) using a credential file or environment variables.
    
1. Environment variables

sbt-github-remote-cache will look for github user and pass in the environment variables `BINTRAY_REMOTE_CACHE_USER` and  `BINTRAY_REMOTE_CACHE_PASS`. Note that these are different from sbt-github.

2. Credentials file

sbt-github-remote-cache will look for a credentials file under `~/.github/.credentials` used to authenticate publishing requests to github.

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
