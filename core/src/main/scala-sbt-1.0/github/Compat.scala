package github

import sbt.librarymanagement.RawRepository
import scala.language.reflectiveCalls

object RawRepository {
  def apply(resolver: AnyRef): RawRepository =
    new RawRepository(resolver, resolver.asInstanceOf[{ def getName(): String }].getName)
}
