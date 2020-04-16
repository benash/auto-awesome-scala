package http4s

import cats.effect.IO
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Request}

object clientOps {
  implicit class ClientExtension(private val client: Client[IO]) {
    def expectOptionList[A](req: Request[IO])(implicit d: EntityDecoder[IO, A]): IO[List[A]] =
      client.expectOption[A](req).map(_.toList)
  }
}
