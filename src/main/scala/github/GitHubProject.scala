package github

import cats.effect.IO
import org.http4s.Method.GET
import org.http4s.implicits._
import org.http4s.{Header, Headers, Request}

object GitHubProject {
  private val gitHubUrl = """https://github.com/([\w-]+)/([\w-]+)(\.git)?""".r
  def maybe(url: String): Option[GitHubProject] = url match {
    case gitHubUrl(username, project, _) => Some(GitHubProject(username, project))
    case _ => None
  }

}

case class GitHubProject(username: String, project: String) {
  def req(implicit token: GitHubToken): Request[IO] = Request[IO](
    method = GET,
    uri = uri"https://api.github.com"
      .withPath(s"/repos/$username/$project"),
    headers = Headers.of(Header("Authorization", s"token ${token.value}"))
  )
}

