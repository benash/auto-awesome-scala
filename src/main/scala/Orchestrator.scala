import cats.effect._
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.parallel.catsSyntaxParallelFlatTraverse
import github.{GitHubProject, GitHubRepo, GitHubToken}
import http4s.clientOps._
import maven.Pom._
import maven.{MavenSearchRequest, MavenSearchResult, Pom}
import org.http4s.client.Client
import org.http4s.client.middleware.{FollowRedirect, RequestLogger}
import ratelimit.RateLimit

case class Orchestrator(client: Client[IO], mavenLimit: IO[RateLimit], gitHubToken: GitHubToken)(implicit timer: Timer[IO], cs: ContextShift[IO]) {
  val mavenClient: Client[IO] = RequestLogger(
    logHeaders = false,
    logBody = false,
    logAction = Some((s: String) => IO.delay(println(s))),
  )(client)

  val c: Client[IO] = FollowRedirect(3)(client)

  def queryMaven(request: MavenSearchRequest): IO[MavenSearchResult] =
    client.expect[MavenSearchResult](request.uri)
      .flatTap(r => IO.delay(println(s"Found ${r.total} artifacts; limited to ${r.size}")))

  def downloadPoms(result: MavenSearchResult): IO[List[Pom]] = for {
    limit <- mavenLimit
    res <- result.reqs.parFlatTraverse { req =>
      limit.throttle {
        client.expectOptionList[Pom](req)
          .handleErrorWith(_ => IO.delay(List()))
      }
    }
  } yield res

  def getRepos(projects: List[GitHubProject]): IO[List[GitHubRepo]] = projects.parFlatTraverse {
    repo => c.expectOptionList[GitHubRepo](repo.req(gitHubToken))
  }
    .flatTap(r => IO.delay(println(s"Retrieved ${r.size} repos")))
    .flatTap(r => IO.delay(r.foreach(println)))
}
