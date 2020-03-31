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

  case class ParsedArgs(limit: Int, updatedWithinDays: Int)
  object ParsedArgs {
    def apply(args: List[String]): IO[ParsedArgs] = args match {
      case limit :: updatedSinceDays :: Nil => IO.delay(ParsedArgs(limit.toInt, updatedSinceDays.toInt))
      case _ => IO.raiseError(new RuntimeException("bad args"))
    }
  }

  def run(args: List[String]): IO[ExitCode] = {
    val repos = BlazeClientBuilder[IO](global).resource.use { client =>
      for {
        parsed <- ParsedArgs(args)
        mavenRequest = MavenSearchRequest(parsed.limit, parsed.updatedWithinDays)
        result: MavenSearchResult <- client.expect[MavenSearchResult](mavenRequest.uri)
            .flatTap(r => IO.delay(println(s"Found ${r.total} artifacts; limited to ${r.size}")))
        limit <- RateLimit(10, 1.second)
        pomResults: List[Pom] <- result.reqs.parFlatTraverse(limit.throttle(client.expectOptionList[Pom]))
            .flatTap(r => IO.delay(println(s"Retrieved ${r.size} poms")))
        distinctReqs = distinctRepoRequests(pomResults)
        repos: List[GitHubRepo] <- distinctReqs.parFlatTraverse(client.expectOptionList[GitHubRepo])
          .flatTap(r => IO.delay(println(s"Retrieved ${r.size} repos")))
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
