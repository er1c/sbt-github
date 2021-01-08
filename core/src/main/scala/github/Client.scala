package github

import org.asynchttpclient.{ AsyncHandler, Response }
import dispatch.{ FunctionHandler, Http, Req }
import scala.concurrent.{ ExecutionContext, Future }

object Client {
  type Handler[T] = AsyncHandler[T]

  case class Error(code: Int, message: Message) extends RuntimeException(message.message)

  abstract class Completion[T: Rep] {

    def apply[TT](handler: Client.Handler[TT]): Future[TT]

    def apply(): Future[T] =
      apply(implicitly[Rep[T]].map(_))

    def apply[TT](f: Response => TT): Future[TT] =
      apply(new FunctionHandler(f) {
        override def onCompleted(response: Response) =
          if (response.getStatusCode / 100 == 2) f(response)
          else
            throw Error(
              response.getStatusCode,
              if (response.hasResponseBody) Message(response.getResponseBody)
              else Message.empty
            )
      })
  }
}

abstract class Requests(credentials: Credentials, http: Http)(implicit ec: ExecutionContext)
  extends DefaultHosts
    with Methods {

  def request[T](req: Req)(handler: Client.Handler[T]): Future[T] =
    http(credentials.sign(req) > handler)

  def complete[A: Rep](req: Req): Client.Completion[A] =
    new Client.Completion[A] {
      override def apply[T](handler: Client.Handler[T]) =
        request(req)(handler)
    }
}

case class Client(
  user: String,
  token: String,
  private val http: Http = Http(Http.defaultClientBuilder)
)(implicit ec: ExecutionContext)
  extends Requests(Credentials.BasicAuth(user, token), http) {

  /** releases http resources. once closed, this client may no longer be used */
  def close(): Unit = http.shutdown()
}