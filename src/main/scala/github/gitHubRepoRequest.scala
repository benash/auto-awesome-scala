package github

import cats.effect.IO
import org.http4s.Method.GET
import org.http4s.implicits._
import org.http4s.{Header, Headers, Request}

object gitHubRepoRequest {
  private val gitHubUrl = """https://github.com/([\w-]+)/([\w-]+)(\.git)?""".r

  def from(url: String)(implicit token: GitHubToken): Option[Request[IO]] = url match {
    case gitHubUrl(username, project, _) => Some(repoRequest(username, project))
    case _ => None
  }

  private def repoRequest(username: String, project: String)(implicit token: GitHubToken): Request[IO] = Request[IO](
    method = GET,
    uri = uri"https://api.github.com"
      .withPath(s"/repos/$username/$project"),
    headers = Headers.of(Header("Authorization", s"token ${token.value}"))
  )
}

