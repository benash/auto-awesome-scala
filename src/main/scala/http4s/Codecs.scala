package http4s

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import org.http4s.Uri

import scala.util.Try

object Codecs {
  implicit val uriKeyEncoder: KeyEncoder[Uri] = _.renderString
  implicit val uriKeyDecoder: KeyDecoder[Uri] = (s: String) => Uri.fromString(s).toOption

  implicit val uriEncoder: Encoder[Uri] = Encoder.encodeString.contramap[Uri](uriKeyEncoder.apply)
  implicit val uriDecoder: Decoder[Uri] = Decoder.decodeString.emapTry { str =>
    Try(uriKeyDecoder.apply(str).get)
  }
}
