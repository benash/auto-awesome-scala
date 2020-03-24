package maven

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import org.http4s._

import scala.xml.{Elem, Node}

object Pom {
  private implicit val decoder: EntityDecoder[IO, Elem] = scalaxml.xml[IO]
  implicit val pomDecoder: EntityDecoder[IO, Pom] =
    EntityDecoder.decodeBy(MediaType.text.xml) { media: Media[IO] =>
      EitherT {
        media.as[Elem].map { elem: Elem => Pom(elem).asRight[DecodeFailure] }
      }
    }
}

case class Pom(root: Node) {
  def urls: Seq[String] = {
    val nodes = (root \ "url") ++ (root \ "scm" \ "url")
    nodes.map(_.text)
  }
}
