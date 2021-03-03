package io.mdcatapult.doclib.consumer


sealed trait HandlerLogStatus

case object Received extends HandlerLogStatus

case object Completed extends HandlerLogStatus

case object Failed extends HandlerLogStatus

object HandlerLogStatus {

  val NoDocumentError = "error_no_document"
  val UnknownError = "unknown_error"
  val DoclibDocumentException = "doclib_doc_exception"
  val ErrorFlagWriteError = "error_flag_write_error"

  /**
    * @param status        HandlerLogStatus: Received, Completed, or Failed
    * @param loggerMessage The message to log, this should normally be one of the string constants defined above
    * @param documentId    The id of the document
    * @return
    */
  def loggerMessage(status: HandlerLogStatus, loggerMessage: String, documentId: String): String = {
    s"$status - $loggerMessage, identifier: $documentId"
  }

  /**
    * @param status     HandlerLogStatus: Received, Completed, or Failed
    * @param documentId The id of document
    * @return
    */
  def loggerMessage(status: HandlerLogStatus, documentId: String): String = {
    s"$status - identifier: $documentId"
  }
}
