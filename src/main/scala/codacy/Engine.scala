package codacy

import codacy.dockerApi.DockerEngine
import codacy.gometalinter.GoMetaLinter

object Engine extends DockerEngine(GoMetaLinter)