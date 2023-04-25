package io.reitmaier.speechboxapi.data

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object BoxIdSerializer : KSerializer<BoxId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.BoxId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: BoxId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): BoxId {
    return BoxId(decoder.decodeInt())
  }
}

object MobileIdSerializer : KSerializer<MobileId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.MobileId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: MobileId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): MobileId {
    return MobileId(decoder.decodeInt())
  }
}
object StoryIdSerializer : KSerializer<StoryId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.StoryId", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: StoryId) {
    encoder.encodeInt(value.value)
  }

  override fun deserialize(decoder: Decoder): StoryId {
    return StoryId(decoder.decodeInt())
  }
}

object RecordingStopReasonSerializer : KSerializer<RecordingStopReason> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.RecordingStopReason", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: RecordingStopReason) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): RecordingStopReason {
    return RecordingStopReason(decoder.decodeString())
  }
}

object SessionIdSerializer : KSerializer<SessionId> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.SessionId", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: SessionId) {
    encoder.encodeString(value.value.toString())
  }

  override fun deserialize(decoder: Decoder): SessionId {
    return SessionId(UUID.fromString(decoder.decodeString()))
  }
}
object ParticipationTokenSerializer : KSerializer<ParticipationToken> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.ParticipationToken", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: ParticipationToken) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): ParticipationToken {
    return ParticipationToken(decoder.decodeString())
  }
}

object MobileNumberSerializer : KSerializer<MobileNumber> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("data.MobileNumber", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: MobileNumber) {
    encoder.encodeString(value.value)
  }

  override fun deserialize(decoder: Decoder): MobileNumber {
    return MobileNumber(decoder.decodeString())
  }
}

object InstantEpochSerializer : KSerializer<Instant> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

  override fun serialize(encoder: Encoder, value: Instant) =
    encoder.encodeLong(value.toEpochMilliseconds())

  override fun deserialize(decoder: Decoder): Instant =
    Instant.fromEpochMilliseconds(decoder.decodeLong())
}
