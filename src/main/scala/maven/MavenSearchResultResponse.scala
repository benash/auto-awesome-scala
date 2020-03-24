package maven

case class MavenSearchResultResponse(
  numFound: Int,
  docs: List[MavenSearchResultDoc],
)
