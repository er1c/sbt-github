package github

import sbt.librarymanagement.{ RawRepository, Resolver }

object RawRepository {
  def apply(resolver: AnyRef): RawRepository = {
    new RawRepository(resolver, resolver.asInstanceOf[Resolver].name)
  }
}
