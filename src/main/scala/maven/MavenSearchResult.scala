package maven

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, Uri}
import org.http4s.circe.jsonOf

object MavenSearchResult {
  implicit val mavenSearchResultDecoder: EntityDecoder[IO, MavenSearchResult] =
    jsonOf[IO, MavenSearchResult]
}

case class MavenSearchResult(
  response: MavenSearchResultResponse,
) {
  def total: Int = response.numFound
  def size: Int = response.docs.size
  def pomUris: List[Uri] = response.docs.map(_.uri)
}
