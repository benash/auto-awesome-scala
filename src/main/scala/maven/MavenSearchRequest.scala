package maven

import java.time.{Duration, ZonedDateTime}

import org.http4s.Uri
import org.http4s.implicits._

object MavenSearchRequest {
  def apply(limit: Int, updatedWithinDays: Int): MavenSearchRequest = MavenSearchRequest(
    limit = limit,
    updatedWithin = Duration.ofDays(updatedWithinDays),
  )
}

case class MavenSearchRequest(limit: Int, updatedWithin: Duration) {
  private def minMillis = ZonedDateTime.now()
    .minus(updatedWithin)
    .toInstant
    .toEpochMilli

  def uri: Uri = uri"https://search.maven.org/solrsearch/select"
    .withQueryParam("q", s"timestamp:[$minMillis TO *]")
    .withQueryParam("rows", limit)
    .withQueryParam("wt", "json")
}
