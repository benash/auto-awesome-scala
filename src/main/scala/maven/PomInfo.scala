package maven

import org.http4s.Uri

case class PomInfo(uris: List[Uri], isScala: Boolean)

object PomInfo {
  def fromPom(pom: Pom): PomInfo = PomInfo(
    uris = pom.urls.flatMap(Uri.fromString(_).toOption),
    isScala = pom.isScala,
  )
}