import cats.effect._
import cats.implicits._
import github.{GitHubProject, GitHubRepo, GitHubToken}
import http4s.clientOps._
import maven.MavenSearchResult.mavenSearchResultDecoder
import maven.{MavenSearchRequest, MavenSearchResult, Pom}
import org.http4s.client.blaze.BlazeClientBuilder
import ratelimit.RateLimit

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object Main extends IOApp {

  implicit val gitHubToken: GitHubToken = GitHubToken(sys.env("GITHUB_TOKEN"))

  def run(args: List[String]): IO[ExitCode] = {
    BlazeClientBuilder[IO](global).resource.use { client =>
      for {
        parsed <- ParsedArgs(args)
        mavenRequest = MavenSearchRequest(parsed.limit, parsed.updatedWithinDays)
        mavenSearchResult <- client.expect[MavenSearchResult](mavenRequest.uri)
            .flatTap(r => IO.delay(println(s"Found ${r.total} artifacts; limited to ${r.size}")))
        limit <- RateLimit(10, 1.second)
        poms <- mavenSearchResult.reqs.parFlatTraverse(limit.throttle(client.expectOptionList[Pom]))
            .flatTap(r => IO.delay(println(s"Retrieved ${r.size} poms")))
        distinctRepos = GitHubProject.of(poms).distinct
        repos <- distinctRepos.parFlatTraverse(repo => client.expectOptionList[GitHubRepo](repo.req))
          .flatTap(r => IO.delay(println(s"Retrieved ${r.size} repos")))
        _ <- MdFile("output.md", repos).write
      } yield ExitCode.Success
    }.unsafeRunSync()
    IO(ExitCode.Success)
  }
}
