package io.mdcatapult.doclib.models.ner

import java.util.UUID

import io.mdcatapult.doclib.models.BsonCodecCompatible
import io.mdcatapult.doclib.util.MongoCodecs
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
import org.scalatest.{FlatSpec, Matchers}


class FragmentOccurrenceSpec extends FlatSpec with Matchers with BsonCodecCompatible {

  val registry: CodecRegistry = MongoCodecs.get

  "Model" can "be encoded and decoded successfully to BSON" in {
    val uuid = UUID.fromString("dc83cac6-4daa-4a0b-8e52-df1543af1e8f")
    val docUUID = UUID.fromString("600029ba-ccea-4e46-9ea5-7f54996954dd")
    roundTrip(Occurrence(
      _id = uuid,
      nerDocument = docUUID,
      characterStart = 1,
      characterEnd = 2,
      wordIndex = Some(3)
    ),
      """{
        |"_id": {"$binary": "3IPKxk2qSguOUt8VQ68ejw==", "$type": "04"},
        |"nerDocument": {"$binary": "YAApuszqTkaepX9UmWlU3Q==", "$type": "04"},
        |"characterStart": 1,
        |"characterEnd": 2,
        |"wordIndex": 3,
        |"fragment": null,
        |"correctedValue": null,
        |"correctedValueHash": null,
        |"resolvedEntity": null,
        |"resolvedEntityHash": null,
        |"type": "fragment"}""".stripMargin, classOf[Occurrence])
  }

  it can "give old known hash for same document occurrence" in {
    val uuid = UUID.fromString("dc83cac6-4daa-4a0b-8e52-df1543af1e8f")
    val docUUID = UUID.fromString("600029ba-ccea-4e46-9ea5-7f54996954dd")
    val doc = Occurrence(
      _id = uuid,
      nerDocument = docUUID,
      characterStart = 12,
      characterEnd = 15,
      wordIndex = Some(10),
      fragment = Option(UUID.fromString("600029ba-ccea-4e46-9ea5-7f54996954dd")),
      correctedValue = Option("fixed!"),
      correctedValueHash = Option("5e185e300268642a0fcbc964"),
      resolvedEntity = Option("resolved entity"),
      resolvedEntityHash = Option("5e1860510268642a0fcbc965")
    )

    assert(Occurrence.md5(Seq(doc)) == "9fca56f6034faef6af4a5eebe6da48ba")
  }

  it can "give old known hash for same document occurrence with optionals are None" in {
    val uuid = UUID.fromString("dc83cac6-4daa-4a0b-8e52-df1543af1e8f")
    val docUUID = UUID.fromString("600029ba-ccea-4e46-9ea5-7f54996954dd")
    val doc = Occurrence(
      _id = uuid,
      nerDocument = docUUID,
      characterStart = 12,
      characterEnd = 15,
      wordIndex = Some(10),
      fragment = None,
      correctedValue = None,
      correctedValueHash = None,
      resolvedEntity = None,
      resolvedEntityHash = None
    )

    assert(Occurrence.md5(Seq(doc)) == "0bfff4caac95b2fda8f38ddf20715ddb")
  }
}
