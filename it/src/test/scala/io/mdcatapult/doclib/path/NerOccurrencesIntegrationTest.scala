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

import java.util.UUID.randomUUID

import com.typesafe.config.{Config, ConfigFactory}
import io.mdcatapult.doclib.models.ner.Fixture.docId
import io.mdcatapult.doclib.models.ner.{NerDocument, Occurrence}
import io.mdcatapult.klein.mongo.Mongo
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.{equal => Mequal}
import org.mongodb.scala.model.Updates.combine
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class NerOccurrencesIntegrationTest  extends IntegrationSpec with BeforeAndAfterAll with ScalaFutures {

  implicit val config: Config = ConfigFactory.load()

  implicit val mongo: Mongo = new Mongo()

  val nerCollection: MongoCollection[NerDocument] =
    mongo.getDatabase(config.getString("mongo.doclib-database")).getCollection(collectionName(suffix = "ner", prefixConfigName = "mongo.ner-collection"))
  val occurrenceCollection: MongoCollection[Occurrence] =
    mongo.getDatabase(config.getString("mongo.doclib-database")).getCollection(collectionName(suffix = "occurrences", prefixConfigName = "mongo.ner-collection"))

  override def beforeAll(): Unit = {
    Await.result(nerCollection.deleteMany(combine()).toFuture(), Duration.Inf) // empty collection
    Await.result(occurrenceCollection.deleteMany(combine()).toFuture(), Duration.Inf) // empty collection
  }

  "An NER doc" can "be written to disk" in {
    val nerDoc = NerDocument(
      _id =  randomUUID(),
      value = "value",
      hash = "01234567890",
      entityType = Some("entity-type"),
      entityGroup = Some("entity-group"),
      resolvedEntity = Some("resolved-entity"),
      resolvedEntityHash = Some("resolved-entity-hash"),
      document = docId,
    )
    val written = nerCollection.insertOne(nerDoc).toFutureOption()
    val read = written.flatMap(_ => nerCollection.find(Mequal("_id", nerDoc._id)).toFuture())

    whenReady(read, longTimeout) { doc =>
      doc.headOption.get._id should be(nerDoc._id)
      doc.headOption.get.entityType should be(nerDoc.entityType)
      doc.headOption.get.entityGroup should be(nerDoc.entityGroup)
    }
  }

  "An occurrence document" can "be written to disk" in {

    val occurrence = Occurrence(
      _id = randomUUID(),
      nerDocument = randomUUID(),
      characterStart = 12,
      characterEnd = 15,
      fragment = Option(randomUUID()),
      correctedValue = Option("fixed!"),
      correctedValueHash = Option("5e185e300268642a0fcbc964")
    )
    val written = occurrenceCollection.insertOne(occurrence).toFutureOption()
    val read = written.flatMap(_ => occurrenceCollection.find(Mequal("_id", occurrence._id)).toFuture())

    whenReady(read, longTimeout) { doc =>
      doc.headOption.get._id should be(occurrence._id)
      doc.headOption.get.nerDocument should be(occurrence.nerDocument)
      doc.headOption.get.`type` should be("fragment")
    }
  }

  "An occurrence fragment" can "be written to disk" in {
    val occurrence = Occurrence(
      _id = randomUUID(),
      nerDocument = randomUUID(),
      characterStart = 12,
      characterEnd = 15,
      fragment = Option(randomUUID()),
      correctedValue = Option("fixed!"),
      correctedValueHash = Option("5e185e300268642a0fcbc964"),
      wordIndex = Some(10)
    )
    val written = occurrenceCollection.insertOne(occurrence).toFutureOption()
    val read = written.flatMap(_ => occurrenceCollection.find(Mequal("_id", occurrence._id)).toFuture())

    whenReady(read, longTimeout) { doc =>
      doc.headOption.get._id should be(occurrence._id)
      doc.headOption.get.wordIndex.get should be(10)
      doc.headOption.get.fragment should be(occurrence.fragment)
      doc.headOption.get.`type` should be("fragment")
    }
  }

}
