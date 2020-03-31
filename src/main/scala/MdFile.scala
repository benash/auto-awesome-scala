import java.io.{BufferedWriter, File, FileWriter}

import cats.effect.IO
import github.GitHubRepo

case class MdFile(filename: String, repos: List[GitHubRepo]) {
  private def writeFile(filename: String, s: String): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s)
    bw.close()
  }
  private val header = """
                         || Name | Description | GitHub Stats |
                         || --- | --- | --- |
                         |""".stripMargin

  private val repoLines: String = repos.map(_.md).mkString("\n")

  def write = IO.delay {
    println("GOT HERHERERE")
    writeFile(filename, header + repoLines)
  }
}
