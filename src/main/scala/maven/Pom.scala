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
        media
          .as[Elem]
          .map { elem: Elem => Pom(elem).asRight[DecodeFailure] }
          .map {
            case Left(value) => {
              Left(value)
            }
            case Right(value) => Right(value)
          }
      }
    }
}

case class Pom(root: Node) {
  def urls: List[String] = {
    val nodes = (root \ "url") ++ (root \ "scm" \ "url")
    nodes.map(_.text).toList
  }

  def isScala: Boolean = (root \ "dependencies" \ "dependency" \ "artifactId")
    .exists(_.text == "scala-library")
}
