package codacy.gometalinter

import java.nio.file.Path

import codacy.dockerApi._
import codacy.dockerApi.utils.{CommandRunner, FileHelper, ToolHelper}
import play.api.libs.json._

import scala.util.Try

case class GoMetaLinterResult(file: String, line: Int, column: Int, level: String, code: Int, message: String)

object GoMetaLinterResult {
  implicit val GoMetaLinterResult = Json.format[GoMetaLinterResult]
}

object GoMetaLinter extends Tool {

  override def apply(path: Path, conf: Option[List[PatternDef]], files: Option[Set[Path]])
                    (implicit spec: Spec): Try[List[Result]] = {
    Try {
      val filesToLint: Seq[String] = files.fold {
        FileHelper.listAllFiles(path)
          .map(_.getAbsolutePath).filter(_.endsWith(".sh"))
      } {
        paths =>
          paths.map(_.toString).toList
      }

// @TODO - update with the proper command
      val command = List("gometalinter", "-f", "json") ++ filesToLint
      CommandRunner.exec(command) match {
        case Right(resultFromTool) =>
          parseToolResult(resultFromTool.stdout, path, conf)
        case Left(failure) =>
          throw failure
      }
    }
  }

  private def parseToolResult(resultFromTool: List[String], path: Path, conf: Option[List[PatternDef]])
                             (implicit spec: Spec): List[Result] = {
    val results = Try(Json.parse(resultFromTool.mkString)).toOption
      .flatMap(_.asOpt[List[GoMetaLinterResult]]).getOrElse(List.empty)
      .map { result =>
        Issue(
          SourcePath(FileHelper.stripPath(result.file, path.toString)),
          ResultMessage(result.message),
          PatternId(s"SC${result.code}"),
          ResultLine(result.line))
      }

    ToolHelper.getPatternsToLint(conf).fold {
      results
    } { patterns =>
      results.filter { r => patterns.map(_.patternId).contains(r.patternId) }
    }
  }

}