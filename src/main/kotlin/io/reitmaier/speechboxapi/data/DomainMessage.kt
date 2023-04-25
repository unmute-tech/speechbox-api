package io.reitmaier.speechboxapi.data

import com.github.michaelbull.result.Result
import io.reitmaier.speechboxapi.plugins.FORM_PARAMETER_EXTENSION

typealias DomainResult<T> = Result<T, DomainMessage>
/**
 * All possible things that can happen in the use-cases
 */
sealed class DomainMessage(val message: String)

object RequestFileMissing : DomainMessage("The request is missing a file item")
object ExtensionMissing : DomainMessage("The request is missing the file extension parameter: $FORM_PARAMETER_EXTENSION")
object BoxNotFound : DomainMessage("Box not found")
object StoryNotFound : DomainMessage("Story not found")
object SessionIdRequired : DomainMessage("A sessionId parameter is required")
object SessionIdInvalid : DomainMessage("The sessionId parameter not valid UUID")
object BoxIdRequired : DomainMessage("A boxId parameter is required")
object BoxIdInvalid : DomainMessage("The boxId parameter is invalid")
object StoryIdRequired : DomainMessage("A storyId parameter is required")
object MobileNumberRequired : DomainMessage("A mobile number is required in request body")
object ConfirmationAnswerRequired : DomainMessage("A confirmation answer is required in request body")
object RecordingLengthRequired : DomainMessage("A recording length (ms) is required in request body")
object RecordingStopReasonRequired : DomainMessage("A reason for stopping the recording (String) is required in request body")
object MobileNumberInvalid : DomainMessage("The mobile number is invalid")
object StoryIdInvalid : DomainMessage("The storyId parameter is invalid")
object NoTokensLeft : DomainMessage("There are no tokens remaining")

/* internal errors */

object DatabaseError : DomainMessage("An Internal Error Occurred")
