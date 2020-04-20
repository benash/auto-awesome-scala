import cats.effect.{ContextShift, IO, Timer}
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.parallel.{catsSyntaxParallelFlatTraverse, catsSyntaxParallelTraverse}
import github.{GitHubProject, GitHubRepo, GitHubToken}
import http4s.ClientMiddleware._
import http4s.clientOps._
import maven.Pom._
import maven.PomCache.PomLookup
import maven._
import org.http4s.client.middleware.FollowRedirect
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{MalformedMessageBodyFailure, Status, Uri}

import scala.concurrent.duration.DurationLong

case class Orchestrator(client: Client[IO], gitHubToken: GitHubToken)(implicit timer: Timer[IO], cs: ContextShift[IO]) {
  val redirectedClient: Client[IO] = FollowRedirect(3)(client)

  def queryMaven(request: MavenSearchRequest): IO[List[Uri]] =
    client.expect[MavenSearchResult](request.uri)
      .flatTap(_ => IO.delay(println(request.uri)))
      .flatTap(r => IO.delay(println(s"Found ${r.total} artifacts; limited to ${r.size}")))
      .map(_.pomUris)

  def retrievePoms(pomUris: List[Uri], cache: PomLookup): IO[List[PomResult]] = for {
    limited <- Limited(10)
    throttled = Throttled(750.millis)
    mavenClient = limited(throttled(client))
    _ <- IO(println(s"cache size: ${cache.size}"))
    pomResults: List[PomResult] <- pomUris.parTraverse { (uri: Uri) =>
      cache.get(uri) match {
        case None => fetchPom(mavenClient, uri).flatTap(_ => IO(print("0")))
        case Some(pomResult) => IO(print(".")).map(_ => pomResult)
      }
    }
    _ <- IO(println())
  } yield pomResults

  private def fetchPom(client: Client[IO], uri: Uri) = {
    client.expect[Pom](uri)
      .map(pom => PomResult.success(uri, pom))
      .handleErrorWith {
        case UnexpectedStatus(Status.NotFound) => IO(PomResult.failure(uri, Status.NotFound))
        case _: MalformedMessageBodyFailure => IO(PomResult.failure(uri, Status.UnprocessableEntity))
      }
  }

  def getRepos(projects: List[GitHubProject]): IO[List[GitHubRepo]] = for {
    limited <- Limited(5)
    logged = Logged.after(_ => print("0"))
    gitHubClient = limited(logged(redirectedClient))
    res <- projects.parFlatTraverse { repo => gitHubClient.expectOptionList[GitHubRepo](repo.req(gitHubToken)) }
    _ <- IO(println(s"\nRetrieved ${res.size} repos"))
  } yield res
}
