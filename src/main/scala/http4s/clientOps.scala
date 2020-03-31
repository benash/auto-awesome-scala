package http4s

import cats.effect.IO
import cats.implicits._
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Request}

object clientOps {
  implicit class ClientExtension(private val client: Client[IO]) {
    def expectOptionList[A](req: Request[IO])(implicit d: EntityDecoder[IO, A]): IO[List[A]] =
      IO.delay(println(req.uri)) *> client.expectOption[A](req).map(_.toList)
  }
}
