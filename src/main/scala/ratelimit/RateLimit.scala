package ratelimit

import cats.implicits._
import cats.effect.concurrent.Semaphore
import cats.effect.{Concurrent, ContextShift, IO, Timer}

import scala.concurrent.duration.FiniteDuration

object RateLimit {
  def apply(limit: Int, span: FiniteDuration) (implicit F: Concurrent[IO]) : IO[RateLimit] = for {
    s <- Semaphore[IO](limit)
  } yield RateLimit(s, span)
}

case class RateLimit(sem: Semaphore[IO], span: FiniteDuration) {
  def throttle[A, B](function: A => IO[B])
    (implicit timer: Timer[IO], cs: ContextShift[IO]): A => IO[B] = (input: A) => {
    sem.withPermit {
      IO.sleep(span) &> function(input)
    }
  }
}
