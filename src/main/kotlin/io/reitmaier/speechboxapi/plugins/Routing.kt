package io.reitmaier.speechboxapi.plugins

import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.binding.binding
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.launch
import io.reitmaier.speechboxapi.data.*
import io.reitmaier.speechboxapi.db.Story
import io.reitmaier.speechboxapi.templates.Layout
import io.reitmaier.speechboxapi.templates.StatusViewTemplate
import java.io.File
import java.util.UUID

const val BOX_ID_PARAMETER = "boxId"
const val SESSION_ID_PARAMETER = "sessionId"
const val STORY_ID_PARAMETER = "storyId"

const val FORM_PARAMETER_EXTENSION = "extension"
fun Application.configureRouting(repo: CovidStoriesRepo) {
  val log = InlineLogger()

  routing {
    static {
      resource( "static/css/status.css")
      resource( "static/js/map.js")
    }
    get("/") {
      call.respondText("Hello SpeechBox!")
    }
    get("/status") {
      val boxes = repo.getBoxes().fold(
        success = {it},
        failure = {return@get call.respondDomainMessage(it)}
      )
      val duplicates = repo.getDuplicates().fold(
        success = {it},
        failure = {return@get call.respondDomainMessage(it)}
      )

      val unpaid = repo.getUnPaid().fold(
        success = {it},
        failure = {return@get call.respondDomainMessage(it)}
      )

      val storiesByDate = repo.getStoriesByDate().fold(
        success = {it},
        failure = {return@get call.respondDomainMessage(it)}
      )

      call.respondHtmlTemplate(Layout()) {
        content {
          insert(StatusViewTemplate(boxes, duplicates, unpaid, storiesByDate)) {}
        }
      }
    }
    route("/story/{$STORY_ID_PARAMETER}") {
      get {
        binding<StoryId,DomainMessage> {
          call.parameters.readStoryId().bind()
        }.fold(
          success = {call.respond{it}},
          failure = {call.respondDomainMessage(it)}
        )
      }

      post("/file") {
        val story = binding<Story,DomainMessage> {
          val storyId = call.parameters.readStoryId().bind()
          repo.getStory(storyId).bind()
        }.fold(
          success = { it },
          failure = {return@post call.respondDomainMessage(it)}
        )
        val multipartData = call.receiveMultipart().readAllParts()

        val fileItem = multipartData.filterIsInstance<PartData.FileItem>().firstOrNull()
          ?: return@post call.respondDomainMessage(RequestFileMissing)

        val formItems = multipartData.filterIsInstance<PartData.FormItem>()
        val extension = formItems.firstOrNull { it.name == FORM_PARAMETER_EXTENSION }?.value?.trimStart('.')
          ?: return@post call.respondDomainMessage(ExtensionMissing).also { fileItem.dispose() }

        val fileName = "${story.session_id.value}.$extension"
        val file = File("data/$fileName")
        log.debug { "Writing to file: ${file.absolutePath}" }

        // use InputStream from part to save file
        fileItem.streamProvider().use { its ->
          // copy the stream to the file with buffering
          file.outputStream().buffered().use {
            // note that this is blocking
            its.copyTo(it)
          }
        }
        fileItem.dispose()
        repo.addAudioToStory(story.id, fileName)
          .fold(
            success = { call.respond(HttpStatusCode.Created,it.id)},
            failure = { call.respondDomainMessage(it)}
          )
      }
    }
    route("/box/{$BOX_ID_PARAMETER}") {
      post("/ping") {
        call.parameters.readBoxId()
          .andThen { boxId -> repo.pingFromBox(boxId).also { launch {
            val file = File("data/box-${boxId.value}.log")
            file.appendText("$it\n")
          } } }
          .fold(
            success = { call.respond(it) },
            failure = { call.respondDomainMessage(it) }
          )
      }

      get {
        call.parameters.readBoxId()
          .andThen { repo.getBox(it) }
          .fold(
            success = { call.respond(it.id) },
            failure = { call.respondDomainMessage(it) }
          )
      }
      route("/sessions/{$SESSION_ID_PARAMETER}")  {
        post("/token") {
          binding<ParticipationToken, DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val mobileNumber = call.receiveOrNull<MobileNumber>().toResultOr { MobileNumberRequired }
              .andThen {
                log.debug { "Received token request for mobile $it" }
//                if(it.value.length == 10 && it.value.startsWith("0")) {
                  Ok(it)
//                } else {
//                  Err(MobileNumberInvalid)
//                }
              }
              .bind()
            val token = repo.issueToken(boxId,sessionId,mobileNumber).bind()
            token
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/story") {
          binding<StoryId, DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val story = repo.createStory(boxId, sessionId).bind()
            story.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/mobile") {
          binding<ParticipationToken, DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val mobileInfo = call.receiveOrNull<MobileInfo>()
              .toResultOr { MobileNumberInvalid }.bind()
            val mobile = repo.createMobileInfo(boxId, sessionId, mobileInfo).bind()
            val token = repo.issueToken(boxId,sessionId,mobileInfo.number).bind()
            token
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/init") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionInit(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/idle") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionIdle(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/audioError") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionAudioError(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/welcome") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionWelcomeState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/recording") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionRecordingState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/confirmation") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionConfirmationState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/noTokenPrompt") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionNoTokenPromptState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/confirmationAnswer") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val confirmationAnswer = call.receiveOrNull<Int>().toResultOr { ConfirmationAnswerRequired }.bind()
            val session = repo.setSessionConfirmationAnswer(boxId,sessionId,confirmationAnswer).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/recordStopReason") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val recordStopReason = call.receiveOrNull<RecordingStopReason>().toResultOr { RecordingStopReasonRequired }.bind()
            val session = repo.setSessionRecordStopReason(boxId,sessionId,recordStopReason).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/recordingLength") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val recordingLength = call.receiveOrNull<Long>().toResultOr { RecordingLengthRequired }.bind()
            val session = repo.setSessionRecordingLength(boxId,sessionId,recordingLength).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/questionnaireShare") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionQuestionnaireShareState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/questionnaireNoShare") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionQuestionnaireNoShareState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }

        post("/replay") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.increaseSessionReplayCount(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/tokenPrompt") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionTokenPromptState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
        post("/thankYouPrompt") {
          binding<SessionId,DomainMessage> {
            val boxId = call.parameters.readBoxId().bind()
            val sessionId = call.parameters.readSessionId().bind()
            val session = repo.setSessionThankYouPromptState(boxId,sessionId).bind()
            session.id
          }.fold(
            success = { call.respond(HttpStatusCode.Created, it) },
            failure = { call.respondDomainMessage(it)}
          )
        }
      }
    }
  }
}

private fun Parameters.readStoryId(): DomainResult<StoryId>{
  return get(STORY_ID_PARAMETER)
    .toResultOr { StoryIdRequired }
    .andThen { it.toIntResult() }
    .mapError { StoryIdInvalid }
    .map { StoryId(it) }
}
private fun Parameters.readBoxId(): DomainResult<BoxId>{
  return get(BOX_ID_PARAMETER)
    .toResultOr { BoxIdRequired }
    .andThen { it.toIntResult() }
    .mapError { BoxIdInvalid }
    .map { BoxId(it) }
}

private fun Parameters.readSessionId(): DomainResult<SessionId>{
  return get(SESSION_ID_PARAMETER)
    .toResultOr { SessionIdRequired }
    .andThen { it.toSessionIdResult() }
    .mapError { SessionIdInvalid }
}

fun String.toIntResult() : Result<Int, Throwable> =
  runCatching {
    this.toInt()
  }

fun String.toSessionIdResult() : Result<SessionId, Throwable> =
  runCatching {
    SessionId(UUID.fromString(this))
  }

private val log = InlineLogger()
suspend fun ApplicationCall.respondDomainMessage(domainMessage: DomainMessage) {
  log.debug { "Responding with Error: $domainMessage" }
  when(domainMessage) {
    DatabaseError ->  respond(HttpStatusCode.InternalServerError,domainMessage.message)
    BoxIdInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    BoxIdRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    BoxNotFound -> respond(HttpStatusCode.NotFound, domainMessage.message)
    SessionIdRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    SessionIdInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    ExtensionMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    RequestFileMissing -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    StoryIdInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    StoryIdRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    MobileNumberRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    MobileNumberInvalid -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    StoryNotFound -> respond(HttpStatusCode.NotFound, domainMessage.message)
    NoTokensLeft -> respond(HttpStatusCode.NotFound, domainMessage.message)
    ConfirmationAnswerRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    RecordingLengthRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
    RecordingStopReasonRequired -> respond(HttpStatusCode.BadRequest, domainMessage.message)
  }
}
