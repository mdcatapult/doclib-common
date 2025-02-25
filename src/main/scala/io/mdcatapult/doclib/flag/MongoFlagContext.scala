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

package io.mdcatapult.doclib.flag

import java.time.temporal.TemporalAmount

import com.typesafe.config.Config
import io.mdcatapult.klein.mongo.ResultConverters.toUpdatedResult
import io.mdcatapult.doclib.models.{DoclibDoc, DoclibFlag, DoclibFlagState}
import io.mdcatapult.util.models.Version
import io.mdcatapult.util.models.result.UpdatedResult
import io.mdcatapult.util.time.{ImplicitOrdering, Now}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{BsonNull, ObjectId}
import org.mongodb.scala.model.Filters.{and, equal, in, nin}
import org.mongodb.scala.model.Updates.{combine, currentDate, pullByFilter, push, set}

import scala.concurrent.{ExecutionContext, Future}

/**
  * [[FlagContext]] implemented for MongoDB.
  *
  * @param key        The flag key. This should be the consumer name under most circumstances.
  * @param version    The version of the current consumer.
  * @param collection Mongo doclib document collection
  * @param time       qgives current time (is an argument to help with testing).
  * @param config     the consumer config.
  * @param ec         execution context.
  * @note flag deduplication occurs to avoid the rare circumstance when multiple flags are added with the same key.
  *       This assumes however that all flags have a different started timestamp.
  *       If not true then it is possible for all flags to be removed.
  */
class MongoFlagContext(
                        key: String,
                        version: Version,
                        collection: MongoCollection[DoclibDoc],
                        time: Now,
                      )(implicit config: Config, ec: ExecutionContext) extends FlagContext {

  private val flagField = "doclib"
  private val flagKey = s"$flagField.key"
  private val flagVersion = s"$flagField.$$.version"
  private val flagStarted = s"$flagField.$$.started"
  private val flagEnded = s"$flagField.$$.ended"
  private val flagErrored = s"$flagField.$$.errored"
  private val flagReset = s"$flagField.$$.reset"
  private val flagState = s"$flagField.$$.state"
  private val flagSummary = s"$flagField.$$.summary"
  private val flagQueued = s"$flagField.$$.queued"

  val recentRunTolerance: TemporalAmount =
    if (config.hasPath("doclib.tolerance")) {
      config.getTemporal("doclib.tolerance")
    } else {
      java.time.Duration.ofSeconds(10)
    }

  private def notStarted: (String, DoclibDoc) => Future[Nothing] =
    (flag: String, doc: DoclibDoc) => Future.failed(NotStartedException(key)(flag, doc))

  override def isRunRecently(doc: DoclibDoc): Boolean = {
    doc.getFlag(key)
      .exists(doclibFlag =>
        doclibFlag.started.exists(localDateTime =>
          localDateTime.plus(recentRunTolerance).isAfter(time.now())
        )
      )
  }

  /**
    * Set the started timestamp to the current time, the version, and set the summary to "started".
    *
    * @param doc the doc to start
    * @return
    */
  override def start(doc: DoclibDoc)(implicit ec: ExecutionContext): Future[UpdatedResult] = {
    if (doc.hasFlag(key))
      restart(doc)
    else
      for {
        _ <- deDuplicate(doc)
        result <- collection.updateOne(
          combine(
            equal("_id", doc._id),
            nin(flagKey, List(key))),
          combine(push(flagField, DoclibFlag(
            key = key,
            version = version,
            started = Some(time.now()),
            summary = Some("started"),
            queued = Some(true)
          )))
        ).toFuture().map(toUpdatedResult)
      } yield result
  }

  /**
    * Set the ended value to now and nullify other end timestamps. Set queued to false and summary "ended".
    * Set the state to be the passed in state.
    *
    * @param doc     the doc for which processing has completed.
    * @param state   the state to add to the flag
    * @param noCheck if true, executes mongo operations whether the document contains the correct flag key or not.
    * @return
    */
  override def end(
                    doc: DoclibDoc,
                    state: Option[DoclibFlagState] = None,
                    noCheck: Boolean = false
                  )(implicit ec: ExecutionContext): Future[UpdatedResult] = {
    if (noCheck || doc.hasFlag(key)) {

      val stateUpdates = state.map(s => set(flagState, s)).view.toSeq

      val updates =
        Seq(
          currentDate(flagEnded),
          set(flagReset, BsonNull()),
          set(flagSummary, "ended"),
          set(flagErrored, BsonNull()),
          set(flagQueued, false)
        ) ++ stateUpdates

      for {
        _ <- deDuplicate(doc)
        result <- collection.updateOne(
          and(
            equal("_id", doc._id),
            equal(flagKey, key)),
          combine(
            updates: _*
          )
        ).toFuture().map(toUpdatedResult)
      } yield result

    } else notStarted("end", doc)
  }

  /**
    * Set the ended and reset times to null. Set errored to current time.
    * Set queued to false and state to "errored"
    *
    * @param doc the doc to restart
    * @return
    */
  override def error(
                      doc: DoclibDoc,
                      noCheck: Boolean = false
                    )(implicit ec: ExecutionContext): Future[UpdatedResult] = {
    if (noCheck || doc.hasFlag(key)) {
      for {
        _ <- deDuplicate(doc)
        result <- collection.updateOne(
          and(
            equal("_id", doc._id),
            equal(flagKey, key)),
          combine(
            set(flagEnded, BsonNull()),
            set(flagReset, BsonNull()),
            set(flagSummary, "errored"),
            currentDate(flagErrored),
            set(flagQueued, false)
          )).toFuture().map(toUpdatedResult)
      } yield result
    } else notStarted("error", doc)
  }

  /**
    * Sets queued = true on the flag which matches the context flag key
    *
    * @param doc the doc to queue
    * @return
    */
  def queue(doc: DoclibDoc): Future[UpdatedResult] = {
    if (doc.hasFlag(key))
      for {
        _ <- deDuplicate(doc)
        result <- if (doc.getFlag(key).head.isNotQueued) {
          collection.updateOne(
            and(
              equal("_id", doc._id),
              equal(flagKey, key)),
            combine(
              set(flagQueued, true)
            )
          ).toFuture().map(toUpdatedResult)
        } else Future.successful(UpdatedResult.nothing)
      } yield result
    else
      for {
        _ <- deDuplicate(doc)
        result <- collection.updateOne(
          combine(
            equal("_id", doc._id),
            nin(flagKey, List(key))),
          combine(push(flagField, DoclibFlag(
            key = key,
            // version not optional but doesn't really matter since it gets set on start, end etc.
            version = version,
            queued = Some(true)
          )))
        ).toFuture().map(toUpdatedResult)
      } yield result
  }

  /**
    * Resets the flag state to null and sets queued to true.
    *
    * @param doc the doc to restart
    * @return
    */
  def reset(doc: DoclibDoc): Future[UpdatedResult] = {
    if (doc.hasFlag(key)) {
      for {
        _ <- deDuplicate(doc)
        result <- collection.updateOne(
          and(
            equal("_id", doc._id),
            equal(flagKey, key)),
          combine(
            currentDate(flagReset),
            set(flagState, None.orNull),
            set(flagVersion, version),
            set(flagQueued, true)
          )
        ).toFuture().map(toUpdatedResult)
      } yield result
    } else notStarted("reset", doc)
  }

  private def getFlags(id: ObjectId): Future[List[DoclibFlag]] = {
    collection.find(equal("_id", id)).toFuture()
      .map(_.toList.flatMap(_.doclib).filter(_.key == key))
  }

  /**
    * function to self heal in the event duplicate flags appear. Assumes the latest flag is the most relevant and
    * retains that while removing flags with older started timestamps.
    *
    * @param doc the doc to deduplicate flags on
    * @return
    */
  private def deDuplicate(doc: DoclibDoc): Future[UpdatedResult] = {

    import ImplicitOrdering.localDateOrdering

    val timeOrderedFlags: Future[List[DoclibFlag]] =
      getFlags(doc._id).map(_.sortBy(_.started).reverse)

    timeOrderedFlags.flatMap {
      case _ :: Nil =>
        Future.successful(UpdatedResult.nothing)
      case _ :: old =>
        collection.updateOne(
          equal("_id", doc._id),
          pullByFilter(combine(
            equal("doclib",
              combine(
                equal("key", key),
                in("started", old.map(_.started.orNull): _*)
              )
            )
          ))).toFuture().map(toUpdatedResult)
      case _ =>
        Future.successful(UpdatedResult.nothing)
    }
  }

  /**
    * Set the started timestamp to the current time. Clear the
    * ended and errored timestamps.
    *
    * @param doc the doc to restart
    * @return
    */
  private def restart(doc: DoclibDoc): Future[UpdatedResult] = {
    if (doc.hasFlag(key)) {
      for {
        _ <- deDuplicate(doc)
        result <- collection.updateOne(
          and(
            equal("_id", doc._id),
            equal(flagKey, key)),
          combine(
            currentDate(flagStarted),
            set(flagVersion, version),
            set(flagEnded, BsonNull()),
            set(flagErrored, BsonNull()),
            set(flagQueued, true)
          )
        ).toFuture().map(toUpdatedResult)
      } yield result
    } else notStarted("restart", doc)
  }
}
