package github

import org.apache.ivy.plugins.resolver.IBiblioResolver
import sbt.librarymanagement.{Resolver, RawRepository => SbtRawRepository}

object RawRepository {
  def apply(resolver: AnyRef): SbtRawRepository = {
    new SbtRawRepository(resolver, resolver.asInstanceOf[Resolver].name)
  }
  def apply(resolver: IBiblioResolver, name: String): SbtRawRepository =
    new SbtRawRepository(resolver, name)
}
