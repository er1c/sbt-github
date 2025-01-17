/*
 * Adapted from Copyright 2019 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package github

import scala.sys.process._
import scala.util.Try

sealed trait TokenSource extends Product with Serializable {
  def ||(that: TokenSource): TokenSource =
    TokenSource.Or(this, that)
}

object TokenSource {
  final case class Environment(variable: String) extends TokenSource
  final case class Property(key: String) extends TokenSource
  final case class GitConfig(key: String) extends TokenSource
  final case class Or(primary: TokenSource, secondary: TokenSource) extends TokenSource

  def resolveTokenSource(tokenSource: TokenSource): Option[String] = {
    tokenSource match {
      case TokenSource.Or(primary, secondary) =>
        resolveTokenSource(primary).orElse(
          resolveTokenSource(secondary))

      case TokenSource.Environment(variable) =>
        sys.env.get(variable)

      case TokenSource.Property(key) =>
        sys.props.get(key)

      case TokenSource.GitConfig(key) =>
        Try(s"git config $key".!!).map(_.trim).toOption
    }
  }
}