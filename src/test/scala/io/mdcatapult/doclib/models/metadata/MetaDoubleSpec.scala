package io.mdcatapult.doclib.models.metadata

import io.mdcatapult.doclib.models.{BsonCodecCompatible, Derivative}
import io.mdcatapult.doclib.util.MongoCodecs
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
import org.scalatest.{FlatSpec, Matchers}


class MetaDoubleSpec extends FlatSpec with Matchers with BsonCodecCompatible {

  val registry: CodecRegistry = MongoCodecs.get

  "Model" should "be able to be encoded and decoded successfully to BSON" in {
    roundTrip(MetaDouble(
      key = "key",
      value = 2.0
    ),
      """{
        |  "key": "key",
        |  "value": 2.0
        |}""".stripMargin, classOf[MetaDouble])
  }
}
