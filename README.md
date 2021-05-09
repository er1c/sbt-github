
# sbt-github

[![Continuous Integration](https://github.com/er1c/sbt-github/actions/workflows/ci.yml/badge.svg)](https://github.com/er1c/sbt-github/actions/workflows/ci.yml)

An sbt plugin for publishing [github](https://github.com) packages.

Usage and design inspired by [sbt-bintray](https://index.scala-lang.org/sbt/sbt-bintray) .

## TODO

- [x] Strip package down to API
- [x] Setup CI
- [x] Add first GraphQL API call & test
- [ ] Finish `sbt-github` implementation (See commented out code in: https://github.com/er1c/sbt-github/blob/develop/sbt-github/src/main/scala/github/GitHubPlugin.scala)
- [ ] Finish `sbt-github-remote-cache` implementation (See commented out code in: https://github.com/er1c/sbt-github/blob/develop/sbt-github-remote-cache/src/main/scala/github/GitHubRemoteCachePlugin.scala)
- [ ] Testing
- [ ] Release RC1



## Developing

CI Tests require a `GITHUB_TOKEN` environment variable to exist.  The only permissions it should need (as of right now) is `package:read`.

For local testing, add a `$HOME/.github/.credentials` with contents:

    realm = GitHub API Realm
    host = api.github.com
    user = username
    password = token

For IntelliJ, you can export the env var before starting the application has an easy hack.

There is a test repo: [github-package-tests](https://github.com/er1c/github-packages-tests) that will have a (semi-static) set of packages that can be used for CI tests.

### Example GitHub API Call

* [packageVersions](https://github.com/er1c/sbt-github/blob/develop/core/src/main/scala/github/GitHubRepo.scala#L17)
* [package-versions CI](https://github.com/er1c/sbt-github/blob/develop/sbt-github/src/sbt-test/sbt-github/package-versions/build.sbt#L5)

## References

* [sbt-github-packages](https://github.com/djspiewak/sbt-github-packages/tree/master/src/main/scala/sbtghpackages)
* [sbt-bintray](https://github.com/sbt/sbt-bintray)
* [Caliban GitHub Query Client Example](https://github.com/fokot/caliban-talk/blob/master/src/main/scala/example/GithubQuery.scala)
