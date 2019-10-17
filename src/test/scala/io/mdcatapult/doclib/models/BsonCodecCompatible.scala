package io.mdcatapult.doclib.models

import java.nio.ByteBuffer
import java.util

import io.mdcatapult.doclib.util.MongoCodecs
import org.bson._
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.io.{BasicOutputBuffer, ByteBufferBsonInput, OutputBuffer}
import org.mongodb.scala.bson.codecs._
import org.mongodb.scala.bson.collection.immutable.Document

import scala.collection.JavaConverters._
import scala.reflect.ClassTag




trait BsonCodecCompatible {

  val documentCodec: Codec[Document] = DEFAULT_CODEC_REGISTRY.get(classOf[Document])

  def roundTrip[T](value: T, expected: String, provider: CodecProvider, providers: CodecProvider*)(implicit ct: ClassTag[T]): Unit = {
    val codecProviders: util.List[CodecProvider] = (provider +: providers).asJava
    val registry = MongoCodecs.get
    val codec = registry.get(ct.runtimeClass).asInstanceOf[Codec[T]]
    roundTripCodec(value, Document(expected), codec)
  }

  def roundTripCodec[T](value: T, expected: Document, codec: Codec[T]): Unit = {
    val encoded = encode(codec, value)
    val actual = decode(documentCodec, encoded)
    assert(expected == actual, s"Encoded document: (${actual.toJson()}) did not equal: (${expected.toJson()})")

    val roundTripped = decode(codec, encode(codec, value))
    assert(roundTripped == value, s"Round Tripped case class: ($roundTripped) did not equal the original: ($value)")
  }

  def encode[T](codec: Codec[T], value: T): OutputBuffer = {
    val buffer = new BasicOutputBuffer()
    val writer = new BsonBinaryWriter(buffer)
    codec.encode(writer, value, EncoderContext.builder.build)
    buffer
  }

  def decode[T](codec: Codec[T], buffer: OutputBuffer): T = {
    val reader = new BsonBinaryReader(new ByteBufferBsonInput(new ByteBufNIO(ByteBuffer.wrap(buffer.toByteArray))))
    codec.decode(reader, DecoderContext.builder().build())
  }

  def decode[T](value: T, json: String, codec: Codec[T]): Unit = {
    val roundTripped = decode(codec, encode(documentCodec, Document(json)))
    assert(roundTripped == value, s"Round Tripped case class: ($roundTripped) did not equal the original: ($value)")
  }

}
