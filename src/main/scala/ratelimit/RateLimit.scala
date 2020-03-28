package ratelimit

import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, ContextShift, IO, Timer}

import scala.concurrent.duration.FiniteDuration

object RateLimit {
  def apply(limit: Int, span: FiniteDuration)(implicit F: Concurrent[IO], timer: Timer[IO], cs: ContextShift[IO]): IO[RateLimit] = for {
    s <- Semaphore[IO](limit)
  } yield RateLimit(s, span)
}

case class RateLimit(sem: Semaphore[IO], per: FiniteDuration)(implicit timer: Timer[IO], cs: ContextShift[IO]) {
  def exert[A, B](function: A => IO[B]): A => IO[B] = (input: A) => for {
    _ <- sem.acquire
    limit <- IO.sleep(per).start
    res <- function(input)
    _ <- limit.join
    _ <- sem.release
  } yield res
}