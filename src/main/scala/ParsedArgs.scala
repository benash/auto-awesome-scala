import cats.effect.IO

case class ParsedArgs(limit: Int, updatedWithinDays: Int)

object ParsedArgs {
  def apply(args: List[String]): IO[ParsedArgs] = args match {
    case limit :: updatedSinceDays :: Nil => IO.delay(ParsedArgs(limit.toInt, updatedSinceDays.toInt))
    case _ => IO.raiseError(new RuntimeException("bad args"))
  }
}
