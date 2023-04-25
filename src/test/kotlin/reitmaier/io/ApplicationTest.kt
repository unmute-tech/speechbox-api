package reitmaier.io

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.reitmaier.speechboxapi.data.*
import java.io.File
import kotlinx.datetime.Instant

class ApplicationTest {
  private val sessionId = SessionId.TEST
  private val storyId = StoryId.TEST
  private val file = File("PTT-20210804-WA0002.opus")
  @Test
  fun testRoot() = testApplication {
    val response = client.get("/")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Hello SpeechBox!", response.bodyAsText())
  }

  @Test
  fun testGetValidBoxParameter() = testApplication {
    val response = client.get("/box/${BoxId.TEST.value}")
    assertEquals(HttpStatusCode.OK, response.status)
    val boxId = Json.decodeFromString<BoxId>(response.bodyAsText())
    assertEquals(BoxId.TEST, boxId)
  }

  @Test
  fun `test unknown boxId parameter`() = testApplication {
    val client = createClient {
      expectSuccess = false
    }
    val response = client.get("/box/0")
    assertEquals(HttpStatusCode.NotFound, response.status)
  }
  @Test
  fun `test invalid boxId parameter`() = testApplication {
    val client = createClient {
      expectSuccess = false
    }
    val response = client.get("/box/invalid")
    assertEquals(HttpStatusCode.BadRequest, response.status)
  }

  @Test
  fun `test submit mobile info`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/mobile") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(MobileInfo.TEST))
//      setBody(Json.encodeToString(MobileNumber("9783099058")))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    val mobileInfo = Json.decodeFromString<MobileInfo>(response.bodyAsText())
    assertNotNull(mobileInfo)
  }

  @Test
  fun `test create Story`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/story")
    assertEquals(HttpStatusCode.Created, response.status)
    val storyId = Json.decodeFromString<StoryId>(response.bodyAsText())
    assertNotNull(storyId)
  }

  @Test
  fun `test set session audio error`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/audioError")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session init state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/init")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session idle state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/idle")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session welcome state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/welcome")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session recoding state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/recording")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session confirmation state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/confirmation")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session questionnaire share state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/questionnaireShare")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session questionnaire no share state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/questionnaireNoShare")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session token prompt state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/tokenPrompt")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test set session no token prompt state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/noTokenPrompt")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }

  @Test
  fun `test set session thank you prompt state`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/thankYouPrompt")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }

  @Test
  fun `test set session increase replay count`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/replay")
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }


  @Test
  fun `test issue token for session`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/token") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(MobileNumber.TEST))
//      setBody(Json.encodeToString(MobileNumber("9783099058")))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    val token = Json.decodeFromString<ParticipationToken>(response.bodyAsText())
    assertNotNull(token)
  }

  @Test
  fun `test session record stop reason`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/recordStopReason") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(RecordingStopReason.TEST))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }
  @Test
  fun `test session recording length`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/recordingLength") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(1L))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }

  @Test
  fun `test box ping`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/ping") {

    }
    assertEquals(HttpStatusCode.OK, response.status)
    val pingTime = Json.decodeFromString<Instant>(response.bodyAsText())
    assertNotNull(pingTime)
  }

  @Test
  fun `test session confirmation answer`() = testApplication {
    val response = client.post("/box/${BoxId.TEST.value}/sessions/${sessionId.value}/confirmationAnswer") {
      contentType(ContentType.Application.Json)
      setBody(Json.encodeToString(1))
    }
    assertEquals(HttpStatusCode.Created, response.status)
    val returnedSessionId = Json.decodeFromString<SessionId>(response.bodyAsText())
    assertNotNull(returnedSessionId)
    assertEquals(sessionId,returnedSessionId)
  }

  // currently timing out
//  @Test
//  fun `test upload story audio`() = testApplication {
//    val boundary = "WebAppBoundary"
//    val response = client.post("/story/${storyId.value}/file") {
//      setBody(
//        MultiPartFormDataContent(
//          formData {
//            append("extension", ".mp3")
//            append("file", file.readBytes(), Headers.build {
//              append(HttpHeaders.ContentDisposition, "filename=${file.name}")
//            })
//          },
//          boundary,
//          ContentType.MultiPart.FormData.withParameter("boundary", boundary)
//        )
//      )
//    }
//    assertEquals(HttpStatusCode.Created, response.status)
//  }
}
