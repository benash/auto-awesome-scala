package maven

import java.time.{Duration, ZonedDateTime}

import org.http4s.Uri
import org.http4s.implicits._

case class MavenSearchRequest(limit: Int, updatedWithin: Duration, tags: Seq[String]) {
  private def tagsTerms = tags.map(t => s"tags:$t").mkString(" ")
  private def minMillis = ZonedDateTime.now()
    .minus(updatedWithin)
    .toInstant
    .toEpochMilli

  def uri: Uri = uri"https://search.maven.org/solrsearch/select"
    .withQueryParam("q", s"$tagsTerms timestamp:[$minMillis TO *]")
    .withQueryParam("rows", limit)
    .withQueryParam("wt", "json")
}
