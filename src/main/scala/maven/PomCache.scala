package maven

import java.io.{File, PrintWriter}

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import http4s.Codecs._
import org.http4s.Uri

import scala.io.Source
import scala.util.Using

object PomCache {
  type PomLookup = Map[Uri, PomResult]
  val filename = "pom-experiment.cache.json"

  def writeFile(pomLookup: PomLookup): IO[Unit] = IO {
    Using(new PrintWriter(new File(PomCache.filename))) {
      writer => writer.println(pomLookup.values.asJson.spaces2)
    }
  }

  def readFile: IO[PomLookup] = IO {
    Using(Source.fromFile(filename)) { source => for {
      res: List[PomResult] <- decode[List[PomResult]](source.mkString)
    } yield res.map(pomRes => pomRes.uri -> pomRes).toMap
    }.flatMap(_.toTry)
      .getOrElse(Map.empty)
  }
}
