package github

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object GitHubRepo {
  implicit val gitHubRepoDecoder: EntityDecoder[IO, GitHubRepo] = jsonOf[IO, GitHubRepo]
}

case class GitHubRepo(
  name: String,
  full_name: String,
  description: Option[String],
  html_url: String,
  stargazers_count: Int,
  pushed_at: String,
  archived: Boolean,
  disabled: Boolean,
) {
  def md: String = s"| [$name]($html_url) | ${description.getOrElse("")} | ![](https://img.shields.io/github/stars/$full_name) ![](https://img.shields.io/github/last-commit/$full_name) |"
}
