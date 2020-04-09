import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.flatMap.toFlatMapOps
import github.{GitHubProject, GitHubRepo, GitHubToken}
import maven.{MavenSearchRequest, MavenSearchResult, Pom}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import ratelimit.RateLimit

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationLong

object Main extends IOApp {
  def parseArgs(args: List[String]): MavenSearchRequest = args match {
    case updatedWithinDaysArg :: rowStartArg :: rowCountArg :: Nil => MavenSearchRequest(
      rowStart = rowStartArg.toInt,
      rowCount = rowCountArg.toInt,
      updatedWithin = java.time.Duration.ofDays(updatedWithinDaysArg.toLong),
    )
    case _ => throw new RuntimeException("bad args")
  }

  val gitHubToken: GitHubToken = GitHubToken(sys.env("GITHUB_TOKEN"))

  def run(args: List[String]): IO[ExitCode] = {
    val mavenRequest = parseArgs(args)
    val mavenLimit: IO[RateLimit] = RateLimit(10, 750.millis)

    BlazeClientBuilder[IO](global).resource.use {
      client: Client[IO] => {
        val o = Orchestrator(client, mavenLimit, gitHubToken)
        for {
          mavenSearchResult: MavenSearchResult <- o.queryMaven(mavenRequest)
          poms: List[Pom] <- o.downloadPoms(mavenSearchResult)
            .flatTap(r => IO.delay(println(s"Retrieved ${r.size} poms")))
          distinctProjects: List[GitHubProject] = GitHubProject.of(poms.filter(_.isScala)).distinct
          repos: List[GitHubRepo] <- o.getRepos(distinctProjects)
          qualityRepos: List[GitHubRepo] = repos.filter(_.stargazers_count > 10)
            .sortBy(_.stargazers_count)
            .reverse
          _ <- MdFile("output.md", qualityRepos).write
        } yield ExitCode.Success
      }
    }
  }
}
