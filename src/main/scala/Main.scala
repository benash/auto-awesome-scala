import java.io.{BufferedWriter, File, FileWriter}
import java.time.Duration

import cats.effect._
import cats.implicits._
import github.{GitHubProject, GitHubRepo, GitHubToken}
import http4s.clientOps._
import maven.MavenSearchResult.mavenSearchResultDecoder
import maven.{MavenSearchRequest, MavenSearchResult, Pom}
import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import ratelimit.RateLimit

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object Main extends IOApp {

  implicit val gitHubToken: GitHubToken = GitHubToken(sys.env("GITHUB_TOKEN"))

  def distinctRepoRequests(poms: List[Pom]): List[Request[IO]] = {
    val projects = for {
      pom <- poms
      url <- pom.urls
      project <- GitHubProject.maybe(url)
    } yield project

    for {
      project <- projects.distinct
    } yield project.req
  }

  def writeFile(filename: String, s: String): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s)
    bw.close()
  }

  def run(args: List[String]): IO[ExitCode] = {
    val limit = args match {
      case limit :: Nil => limit.toInt
      case _ => throw new RuntimeException("bad args")
    }

    val mavenSearchRequest: MavenSearchRequest = MavenSearchRequest(
      limit = limit,
      updatedWithin = Duration.ofDays(365),
    )

    def interceptReq[B](f: Request[IO] => IO[B]): Request[IO] => IO[B] = (input: Request[IO]) => for {
      _ <- IO.delay(println(s"Request: ${input.uri}"))
      res <- f(input)
    } yield res

    val repos = BlazeClientBuilder[IO](global).resource.use { client =>
      for {
        result: MavenSearchResult <- client.expect[MavenSearchResult](mavenSearchRequest.uri)
        _ <- IO.delay(println(s"Found ${result.total} artifacts; limiting to ${result.size}"))
        limit <- RateLimit(10, 1.second)
        pomResults: List[Pom] <- result.reqs.parFlatTraverse(limit.throttle(interceptReq(client.expectOptionList[Pom])))
        _ <- IO.delay(println(s"Retrieved ${pomResults.size} poms"))
        distinctReqs = distinctRepoRequests((pomResults))
        _ <- IO.delay(println(s"Narrowed to ${distinctReqs.size} reqs"))
        repos: List[GitHubRepo] <- distinctReqs.parFlatTraverse(interceptReq(client.expectOptionList[GitHubRepo]))
      } yield repos
    }.unsafeRunSync()

    val header = """
      || Name | Description | GitHub Stats |
      || --- | --- | --- |
      |""".stripMargin

    val repoLines: String = repos.map(_.md).mkString("\n")

    writeFile("output.md", header + repoLines)
    IO(ExitCode.Success)
  }
}
