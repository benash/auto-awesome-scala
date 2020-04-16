package maven

import org.http4s.Uri
import org.http4s.implicits._

case class MavenSearchResultDoc(g: String, a: String, latestVersion: String) {
  private val baseUri = uri"https://search.maven.org/remotecontent"
  private val gId = g.replaceAllLiterally(".", "/")

  def uri: Uri = baseUri.withQueryParam("filepath", s"$gId/$a/$latestVersion/$a-$latestVersion.pom")

  override def toString: String = s"$g/$a"
}
