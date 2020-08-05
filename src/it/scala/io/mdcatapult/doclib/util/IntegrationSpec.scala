package io.mdcatapult.doclib.util

import com.typesafe.config.Config
import org.bson.codecs.configuration.CodecRegistries.fromCodecs
import org.bson.codecs.configuration.CodecRegistry
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}

trait IntegrationSpec extends AnyFlatSpec with Matchers {

  val longTimeout: Timeout = Timeout(Span(10, Seconds))

  implicit val codecs: CodecRegistry = {
    val coreCodecs: CodecRegistry = MongoCodecs.get

    MongoCodecs.includeProvider(
      fromCodecs(
        new NullWritableLocalDateTime(coreCodecs)
      )
    )
  }

  protected def collectionName(suffix: String, prefixConfigName: String = "mongo.collection")(implicit config: Config): String = {
    val prefix = config.getString(prefixConfigName)

    s"${prefix}_$suffix"
  }
}
