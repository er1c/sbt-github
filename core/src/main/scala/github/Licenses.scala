package github

object Licenses {
  // This almost works: curl https://api.github.com/licenses | jq '. | map([.key, .name] | join(" -> "))'
  val keys = Map(
    "agpl-3.0" -> "GNU Affero General Public License v3.0",
    "apache-2.0" -> "Apache License 2.0",
    "bsd-2-clause" -> "BSD 2-Clause \"Simplified\" License",
    "bsd-3-clause" -> "BSD 3-Clause \"New\" or \"Revised\" License",
    "bsl-1.0" -> "Boost Software License 1.0",
    "cc0-1.0" -> "Creative Commons Zero v1.0 Universal",
    "epl-2.0" -> "Eclipse Public License 2.0",
    "gpl-2.0" -> "GNU General Public License v2.0",
    "gpl-3.0" -> "GNU General Public License v3.0",
    "lgpl-2.1" -> "GNU Lesser General Public License v2.1",
    "mit" -> "MIT License",
    "mpl-2.0" -> "Mozilla Public License 2.0",
    "unlicense" -> "The Unlicense"
  )
}