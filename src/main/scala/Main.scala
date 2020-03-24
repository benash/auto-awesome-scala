import java.time.Duration

import cats.effect._
import cats.implicits._
import github.{GitHubRepo, GitHubToken, gitHubRepoRequest}
import maven.MavenSearchResult.mavenSearchResultDecoder
import maven.{MavenSearchRequest, MavenSearchResult, Pom}
import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import http4s.clientOps._

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  implicit val gitHubToken: GitHubToken = GitHubToken(sys.env("GITHUB_TOKEN"))

  def distinctRepoRequests(poms: List[Pom]): List[Request[IO]] = for {
    pom <- poms
    distinctUrl <- pom.urls.distinct
    repoRequest <- gitHubRepoRequest.from(distinctUrl)
  } yield repoRequest

  def run(args: List[String]): IO[ExitCode] = {
    val limit = args match {
      case limit :: Nil => limit.toInt
      case _ => throw new RuntimeException("bad args")
    }

    val mavenSearchRequest: MavenSearchRequest = MavenSearchRequest(
      limit = limit,
      updatedWithin = Duration.ofDays(60),
      tags = Seq("scala*"),
    )

    BlazeClientBuilder[IO](global).resource.use { client =>
      for {
        searchResult: MavenSearchResult <- client.expect[MavenSearchResult](mavenSearchRequest.uri)
        response = searchResult.response
        _ <- IO.delay(println(s"Found ${response.numFound} artifacts; limiting to ${response.docs.size}"))
        pomResults: List[Pom] <- response.docs.flatTraverse(doc => client.expectOptionList[Pom](doc.req))
        _ <- IO.delay(println(s"Retrieved ${pomResults.size} poms"))
        repos: List[GitHubRepo] <- distinctRepoRequests(pomResults).flatTraverse(client.expectOptionList[GitHubRepo])
      } yield repos
    }.unsafeRunSync()
    .foreach(println)

    IO.pure(ExitCode.Success)
  }
}
