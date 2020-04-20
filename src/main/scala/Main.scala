import java.time.Duration

import cats.effect.{ExitCode, IO, IOApp}
import github.{GitHubProject, GitHubRepo, GitHubToken}
import maven.PomCache.PomLookup
import maven.{MavenSearchRequest, PomCache}
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationLong

object Main extends IOApp {
  def parseArgs(args: List[String]): MavenSearchRequest = args match {
    case updatedWithinDaysArg :: rowStartArg :: rowCountArg :: Nil => MavenSearchRequest(
      rowStart = rowStartArg.toInt,
      rowCount = rowCountArg.toInt,
      updatedWithin = Duration.ofDays(updatedWithinDaysArg.toLong),
    )
    case _ => throw new RuntimeException("bad args")
  }

  val gitHubToken: GitHubToken = GitHubToken(sys.env("GITHUB_TOKEN"))

  def run(args: List[String]): IO[ExitCode] = {
    val mavenRequest = parseArgs(args)

    BlazeClientBuilder[IO](global)
      .withRequestTimeout(2.minutes)
      .withIdleTimeout(2.minutes)
      .resource.use { client => {
      val o = Orchestrator(client, gitHubToken)
      for {
        pomUris <- o.queryMaven(mavenRequest)
        existingLookup: PomLookup <- PomCache.readFile
        newPomResults <- o.retrievePoms(pomUris, existingLookup)
        newLookup: PomLookup = newPomResults.map(res => res.uri -> res).toMap
        totalLookup = existingLookup ++ newLookup
        scalaProjects: List[GitHubProject] = totalLookup
          .values
          .toList
          .flatMap(_.maybePomInfo)
          .filter(_.isScala)
          .flatMap(GitHubProject.fromDistilledPom)
          .distinct
        _ <- IO(println(s"${scalaProjects.size} projects look like distinct scala"))
        repos: List[GitHubRepo] <- o.getRepos(scalaProjects)
        qualityRepos: List[GitHubRepo] = repos
          .filter(_.stargazers_count > 10)
          .sortBy(_.stargazers_count)
          .reverse
        _ <- MdFile("output.md", qualityRepos).write
        _ <- PomCache.writeFile(totalLookup)
      } yield ExitCode.Success
    } }
  }
}
