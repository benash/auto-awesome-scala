package http4s

import cats.effect.concurrent.Semaphore
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits.catsSyntaxParallelAp
import cats.syntax.apply.catsSyntaxApply
import org.http4s.{Request, Response}
import org.http4s.client.Client
import cats.syntax.flatMap.toFlatMapOps

import scala.concurrent.duration.FiniteDuration

object ClientMiddleware {
  object Throttled {
    def apply(duration: FiniteDuration)(implicit timer: Timer[IO], cs: ContextShift[IO]): Client[IO] => Client[IO] = (c: Client[IO]) => Client { req: Request[IO] =>
      Resource.liftF(IO.sleep(duration)) &> c.run(req)
    }
  }

  object Limited {
    private def permitResource(sem: Semaphore[IO])(implicit timer: Timer[IO]): Resource[IO, Unit] = {
      Resource.make(sem.acquire)(_ => sem.release)
    }

    def apply(limit: Int)(implicit timer: Timer[IO], cs: ContextShift[IO]): IO[Client[IO] => Client[IO]] = for {
      sem <- Semaphore[IO](limit)
    } yield (c: Client[IO]) => Client { req: Request[IO] =>
      permitResource(sem).flatMap { _ => c.run(req) }
    }
  }

  object Logged {
    def before(f: Request[IO] => Unit): Client[IO] => Client[IO] = (client: Client[IO]) =>
      Client { req: Request[IO] =>
        Resource.liftF(IO(f(req))) *> client.run(req)
      }

    def after(f: Response[IO] => Unit): Client[IO] => Client[IO] = (client: Client[IO]) =>
      Client { req: Request[IO] =>
        client.run(req).flatTap(resp => Resource.liftF(IO(f(resp))))
      }
  }
}
