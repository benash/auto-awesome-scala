package maven

import java.time.ZonedDateTime

import org.http4s.{Status, Uri}

case class PomResult(uri: Uri, maybePomInfo: Option[PomInfo], status: Status, attemptedAt: ZonedDateTime)

object PomResult {
  def success(uri: Uri, pom: Pom): PomResult = PomResult(
    uri = uri,
    maybePomInfo = Some(PomInfo.fromPom(pom)),
    status = Status.Ok,
    attemptedAt = ZonedDateTime.now,
  )
  def failure(uri: Uri, status: Status): PomResult = PomResult(
    uri = uri,
    maybePomInfo = None,
    status = status,
    attemptedAt = ZonedDateTime.now,
  )
}
