package io.reitmaier.speechboxapi.data

import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import io.reitmaier.speechboxapi.db.SpeechBoxDb
import io.reitmaier.speechboxapi.db.*
import io.reitmaier.speechboxapi.services.TwilioService

class CovidStoriesRepo(private val db: SpeechBoxDb, private val sms: TwilioService) {
  private val log = InlineLogger()
  private val boxes = db.boxQueries
  private val stories = db.storiesQueries
  private val mobiles = db.mobilesQueries
  private val settings = db.settingsQueries
  private val sessions = db.sessionsQueries
  private val tokens = db.tokensQueries

  fun getBox(boxId: BoxId) : DomainResult<Box> =
    boxes.getBox(boxId).executeAsOneOrNull().toResultOr { BoxNotFound }
  fun getBoxes() : DomainResult<List<BoxInfo>> =
    runCatching {
      val boxes = boxes.getBoxes().executeAsList()
      boxes
    }
      .andThen { list ->
      runCatching {
        list.map {
          val latestStory = stories.getLatestStoryByBoxId(it.id).executeAsOneOrNull()?.updated_at
          BoxInfo.from(it,latestStory)
        }
      }
    }.mapError { DatabaseError }

  fun getStoriesByDate() : DomainResult<List<StoriesByDate>> =
    runCatching {
      stories.getStoriesByDate().executeAsList()
    }.mapError { DatabaseError }


  fun getDuplicates() : DomainResult<Long> =
    runCatching {
      mobiles.getDuplicateMobiles().executeAsOne()
    }.mapError { DatabaseError }

  fun getUnPaid() : DomainResult<Long> =
    runCatching {
      mobiles.getUnpaidMobiles().executeAsOne()
    }.mapError { DatabaseError }

  fun pingFromBox(boxId: BoxId) : DomainResult<Instant> =
    runCatching {
      val timestamp = Clock.System.now()
      boxes.setLastSeen(timestamp, boxId)
      timestamp
    }.mapError { DatabaseError }

  fun getStory(storyId: StoryId) : DomainResult<Story> =
    stories.getStory(storyId).executeAsOneOrNull().toResultOr { StoryNotFound }

  fun setSessionRecordingLength(boxId: BoxId, sessionId: SessionId, recordingLength: Long): DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setRecordingLength(recordingLength, session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionRecordStopReason(boxId: BoxId, sessionId: SessionId, recordStopReason: RecordingStopReason): DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setRecordStopReason(recordStopReason.value,sessionId)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionConfirmationAnswer(boxId: BoxId, sessionId: SessionId, confirmationAnswer: Int): DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setConfirmationAnswer(confirmationAnswer, session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun addAudioToStory(storyId: StoryId, path: String) : DomainResult<Story>  =
    runCatching {
      stories.addAudioToStory(filename = path, updated_at = Clock.System.now(), id = storyId)
      storyId
    }.mapError { DatabaseError }
      .andThen { getStory(it) }

  fun issueToken(boxId: BoxId, sessionId: SessionId, mobileNumber: MobileNumber) : DomainResult<ParticipationToken> =
    tokens.transactionWithResult {
      val token = tokens.issueToken().executeAsOneOrNull() ?: return@transactionWithResult Err(NoTokensLeft)
      runCatching {
        val story = stories.getStoryBySessionId(sessionId).executeAsOneOrNull()
        if (story != null) {
          stories.setToken(token.id, story.id)
        }
        val session = getOrInsertSession(boxId, sessionId)
        sessions.setToken(token.id, session.id)
        val box = boxes.getBox(boxId).executeAsOneOrNull() ?: throw Throwable("Could not find box with id: $boxId")
        // TODO handle outside of this method
        if(mobileNumber != MobileNumber.TEST) {
          val mobileWithCountryCode = MobileNumber("+${box.country_code}${mobileNumber.value.trimStart('0')}")
          sms.send(mobileWithCountryCode, token)
        }
        tokens.markTokenIssued(Clock.System.now(),token.id)
        token.id
      }.mapError { DatabaseError }
    }

  fun createStory(boxId: BoxId, sessionId: SessionId): DomainResult<Story> {
    return runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      stories.transactionWithResult<Story> {
        val timestamp = Clock.System.now()
        stories.createStory(boxId, timestamp, timestamp, session.id)
        val storyId = StoryId(lastId())
        stories.getStory(storyId).executeAsOne()
      }
    }.mapError { DatabaseError }
  }

  fun createMobileInfo(boxId: BoxId, sessionId: SessionId, mobileInfo: MobileInfo): DomainResult<Mobile> {
    return runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      mobiles.transactionWithResult<Mobile> {
        val timestamp = Clock.System.now()
        val duplicate = mobiles.getMobilesByNumber(mobileInfo.number).executeAsList().isNotEmpty()
        mobiles.createMobile(
          session_id = sessionId,
          created_at = timestamp,
          network = mobileInfo.network,
          box_id = boxId,
          number = mobileInfo.number,
          duplicate = duplicate,
          payment = if(duplicate) -1 else 0
        )
        val mobileId = MobileId(lastId())
        mobiles.getMobileById(mobileId).executeAsOne()
      }
    }.mapError { DatabaseError }
  }

  fun increaseSessionReplayCount(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.increaseReplayCount(session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionTokenPromptState(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setTokenPromptState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionThankYouPromptState(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setThankYouPromptState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionQuestionnaireShareState(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setQuestionnaireShareState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionQuestionnaireNoShareState(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setQuestionnaireNoShareState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionConfirmationState(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setConfirmationState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionRecordingState(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setRecordingState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionWelcomeState(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setWelcomeState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionNoTokenPromptState(boxId: BoxId, sessionId: SessionId): DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setNoTokenPromptState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  fun setSessionAudioError(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setAudioErrorState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }
  fun setSessionInit(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setInitState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }
  fun setSessionIdle(boxId: BoxId, sessionId: SessionId) : DomainResult<Session> =
    runCatching {
      val session = getOrInsertSession(boxId, sessionId)
      sessions.transactionWithResult<Session> {
        sessions.setIdleState(Clock.System.now(), session.id)
        sessions.getSession(session.id).executeAsOne()
      }
    }.mapError { DatabaseError }

  private fun getOrInsertSession(boxId: BoxId, sessionId: SessionId) : Session {
    return sessions.transactionWithResult {
      val session = sessions.getSession(sessionId).executeAsOneOrNull()
      if(session != null){ // check if session exists
        return@transactionWithResult session
      }
      // insert session and return it
      sessions.insertSession(sessionId,boxId) // insert session
      sessions.getSession(sessionId).executeAsOne()
    }
  }

  private fun lastId() : Int = settings.lastInsertedIdAsLong().executeAsOne().toInt()
}

