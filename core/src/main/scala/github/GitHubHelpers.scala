package github

import caliban.client.Operations.IsOperation
import caliban.client.{CalibanClientError, Operations, SelectionBuilder}
import scala.concurrent.Future
import sttp.client3.{SttpBackend, UriContext}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

object GitHubHelpers {
  private val backend: SttpBackend[Future, Any] = AsyncHttpClientFutureBackend()
  private val uri = uri"https://api.github.com/graphql"

  private[github] object await {
    import scala.concurrent.{ Await, Future }
    import scala.concurrent.duration.Duration

    def result[T](f: => Future[T]): T = Await.result(f, Duration.Inf)
    def ready[T](f: => Future[T]) = Await.ready(f, Duration.Inf)
  }

  private[github] def flattenToList[A](as: Option[List[Option[A]]]): List[A] =
    as.getOrElse(Nil).collect { case Some(gf) => gf }

  private[github] def flattenToList[A](as: List[List[A]]): List[A] =
    as.flatten

}

trait GitHubHelpers {
  import GitHubHelpers._
  import scala.concurrent.ExecutionContext.Implicits.global
  def credentials: GitHubCredentials

  protected def runRequest[Origin, A](query: SelectionBuilder[Origin, A])(implicit ev: IsOperation[Origin]): Future[A] =
    backend
      .send(query.toRequest(uri).header("Authorization", s"Bearer ${credentials.token}"))
      .map(_.body)
      .flatMap(handleError)

  protected def get[Origin, A](query: SelectionBuilder[Origin, A])(implicit ev: IsOperation[Origin]): A =
    await.result(runRequest(query))


  private def handleError[A](value: Either[CalibanClientError, A]): Future[A] = value match {
    case Left(error) => Future.failed(error)
    case Right(succ) => Future.successful(succ)
  }

  def close(): Unit = await.ready(backend.close())
}