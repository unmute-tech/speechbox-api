package io.reitmaier.speechboxapi.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import io.reitmaier.speechboxapi.db.StoriesByBox
import java.util.*


data class BoxInfo(
  val id: BoxId,
  val description: String,
  val country_code: String,
  val last_seen: Instant?,
  val deployed_at: Instant?,
  val timezone: String,
  val num_stories: Long,
  val longitude: Double?,
  val latitude: Double?,
  val photo: String?,
  val latestStory: Instant?,
) {
  companion object {
    fun from(storiesByBox: StoriesByBox, latestStory: Instant?) : BoxInfo =
      BoxInfo(
        storiesByBox.id,
        storiesByBox.description,
        storiesByBox.country_code,
        storiesByBox.last_seen,
        storiesByBox.deployed_at,
        storiesByBox.timezone,
        storiesByBox.num_stories,
        storiesByBox.longitude,
        storiesByBox.latitude,
        storiesByBox.photo,
        latestStory
      )
  }

}
@Serializable(with = BoxIdSerializer::class)
@JvmInline
value class BoxId(val value: Int) {
  companion object {
    val TEST = BoxId(1)
  }
}

@Serializable
data class MobileInfo(
  val number: MobileNumber,
  val network: String,
) {
  companion object {
    val TEST = MobileInfo(MobileNumber.TEST, network = "TEST")
  }
}


@Serializable(with = MobileIdSerializer::class)
@JvmInline
value class MobileId(val value: Int) {
  companion object {
    val TEST = MobileId(1)
  }
}
@Serializable(with = StoryIdSerializer::class)
@JvmInline
value class StoryId(val value: Int) {
  companion object {
    val TEST = StoryId(1)
  }
}
@Serializable(with = ParticipationTokenSerializer::class)
@JvmInline
value class ParticipationToken(val value: String) {
  companion object {
    val TEST = ParticipationToken("TOKEN")
  }
}

@Serializable(with = MobileNumberSerializer::class)
@JvmInline
value class MobileNumber(val value: String) {
  companion object {
    val TEST = MobileNumber("0111111111")
  }
}

@Serializable(with = SessionIdSerializer::class)
@JvmInline
value class SessionId(val value: UUID) {
  companion object {
    val TEST = SessionId(UUID.randomUUID())
  }
}

@Serializable(with = RecordingStopReasonSerializer::class)
@JvmInline
value class RecordingStopReason(val value: String) {
  companion object {
    val TEST = RecordingStopReason("MainButton (Test)")
  }
}

