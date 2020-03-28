package http4s

import cats.syntax.functor._
import cats.effect.Bracket
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Request}

object clientOps {
  implicit class ClientExtension[F[_]](private val client: Client[F])(implicit B: Bracket[F, Throwable]){
    def expectOptionList[A](req: Request[F])(implicit d: EntityDecoder[F, A]): F[List[A]] =
      client.expectOption[A](req)
        .map(_.toList)
  }
}
