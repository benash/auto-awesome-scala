package github

import org.scalatest.flatspec.AnyFlatSpec

class GitHubProjectTest extends AnyFlatSpec {

  "GitHubProject" should "be created from URLs" in {
    val cases: Map[Seq[String], Option[GitHubProject]] = Map(
      Seq() -> None,
      Seq("https://aws.amazon.com/sdk") -> None,
      Seq("https://github.com/oracle/java") -> Some(GitHubProject("oracle", "java")),
      Seq("https://github.com/hyphenated-user/hyphenated-project") -> Some(GitHubProject("hyphenated-user", "hyphenated-project")),
      Seq("https://github.com/typelevel/cats", "https://github.com/oracle/java") -> Some(GitHubProject("typelevel", "cats")),
      Seq("https://github.com/typelevel/cats.git") -> Some(GitHubProject("typelevel", "cats")),
    )

    cases.foreach {
      case (urls, maybeGitHubProject) =>
        assert(GitHubProject.fromUrls(urls) == maybeGitHubProject)
    }
  }
}
