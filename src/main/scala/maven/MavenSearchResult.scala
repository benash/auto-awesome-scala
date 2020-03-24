package maven

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.circe.jsonOf
import org.http4s.{EntityDecoder, Uri}

object MavenSearchResult {
  implicit val mavenSearchResultDecoder: EntityDecoder[IO, MavenSearchResult] =
    jsonOf[IO, MavenSearchResult]
}

case class MavenSearchResult(
  response: MavenSearchResultResponse,
) {
  // def pomUris: List[Uri] = response.docs.map(_.pomUri)
}
