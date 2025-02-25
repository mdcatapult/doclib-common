/*
 * Copyright 2024 Medicines Discovery Catapult
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mdcatapult.doclib.path

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TargetPathSpec extends AnyFlatSpec with Matchers {

  implicit val config: Config = ConfigFactory.parseString(
    """
      |doclib {
      |  root: "test-assets"
      |  overwriteDerivatives: false
      |  local {
      |    target-dir: "local"
      |    temp-dir: "ingress"
      |  }
      |  remote {
      |    target-dir: "remote"
      |    temp-dir: "remote-ingress"
      |  }
      |  archive {
      |    target-dir: "archive"
      |  }
      |  derivative {
      |    target-dir: "derivatives"
      |  }
      |}
      |mongo {
      |  database: "prefetch-test"
      |  collection: "documents"
      |  connection {
      |    username: "doclib"
      |    password: "doclib"
      |    database: "admin"
      |    hosts: ["localhost"]
      |  }
      |}
    """.stripMargin)

  class MyTargetPath extends TargetPath

  val targetPath = new MyTargetPath

  "A ingress path " can "be converted to a local path" in {
    val source = "ingress/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.target-dir"))
    assert(target == "local/test.csv")
  }

  "A deeply nested ingress path " can "be converted to a equally nested local path" in {
    val source = "ingress/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.target-dir"))
    assert(target == "local/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested derivatives ingress path " can "be converted to a equally nested derivatives local path" in {
    val source = "ingress/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.target-dir"))
    assert(target == "local/derivatives/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested double derivatives ingress path " can "be converted to a equally nested single derivative local path" in {
    val source = "ingress/derivatives/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.target-dir"))
    assert(target == "local/derivatives/path/to/a/file/somewhere/test.csv")
  }


  "A local path " can "be converted to a ingress path" in {
    val source = "local/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.temp-dir"))
    assert(target == "ingress/test.csv")
  }

  "A deeply nested local path " can "be converted to a equally nested ingress path" in {
    val source = "local/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.temp-dir"))
    assert(target == "ingress/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested derivatives local path " can "be converted to a equally nested derivatives ingress path" in {
    val source = "local/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.temp-dir"))
    assert(target == "ingress/derivatives/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested double derivatives local path " can "be converted to a equally nested single derivative ingress path" in {
    val source = "local/derivatives/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.temp-dir"))
    assert(target == "ingress/derivatives/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested double derivatives local path with prefix " can "be converted to a equally nested single derivative ingress path with prefix" in {
    val source = "local/derivatives/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.local.temp-dir"), Some("spreadsheet_conv-"))
    assert(target == "ingress/derivatives/path/to/a/file/somewhere/spreadsheet_conv-test.csv")
  }


  "A local path " can "be converted to a archive path" in {
    val source = "local/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.archive.target-dir"))
    assert(target == "archive/local/test.csv")
  }

  "A deeply nested local path " can "be converted to a equally nested archive path" in {
    val source = "local/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.archive.target-dir"))
    assert(target == "archive/local/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested derivatives local path " can "be converted to a equally nested derivatives archive path" in {
    val source = "local/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.archive.target-dir"))
    assert(target == "archive/local/derivatives/path/to/a/file/somewhere/test.csv")
  }


  "A remote path " can "be converted to a remote ingress path" in {
    val source = "remote/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.remote.temp-dir"))
    assert(target == "remote-ingress/test.csv")
  }

  "A deeply nested double derivatives local path " can "be converted to a equally nested single derivative archive path" in {
    val source = "local/derivatives/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.archive.target-dir"))
    assert(target == "archive/local/derivatives/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested remote path " can "be converted to a equally nested archive path" in {
    val source = "remote/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.archive.target-dir"))
    assert(target == "archive/remote/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested derivatives remote path " can "be converted to a equally nested derivatives archive path" in {
    val source = "remote/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.archive.target-dir"))
    assert(target == "archive/remote/derivatives/path/to/a/file/somewhere/test.csv")
  }

  "A deeply nested double derivatives remote path " can "be converted to a equally nested single derivative archive path" in {
    val source = "remote/derivatives/derivatives/path/to/a/file/somewhere/test.csv"
    val target = targetPath.getTargetPath(source, config.getString("doclib.archive.target-dir"))
    assert(target == "archive/remote/derivatives/path/to/a/file/somewhere/test.csv")
  }

}
