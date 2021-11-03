package github

import sbt._
import org.apache.ivy.plugins.resolver.{DependencyResolver, IBiblioResolver}

object RawRepository {
  def apply(resolver: DependencyResolver): RawRepository =
    new RawRepository(resolver)
  def apply(resolver: IBiblioResolver, name: String): RawRepository =
    new RawRepository(resolver, name)
}
