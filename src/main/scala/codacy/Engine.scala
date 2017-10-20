package codacy

import codacy.dockerApi.DockerEngine
import codacy.shellcheck.ShellCheck

object Engine extends DockerEngine(GoMetaLinter)