package io.reitmaier.speechboxapi.services

import com.github.michaelbull.logging.InlineLogger
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber
import io.reitmaier.speechboxapi.data.MobileNumber
import io.reitmaier.speechboxapi.db.Token

class TwilioService(
    val accountSID: String,
    val authToken: String,
    val number: String,

) {
  private val log = InlineLogger()
  init {
    Twilio.init(accountSID, authToken)
  }

  fun send(mobileNumber: MobileNumber, token: Token) {
    log.info { "Sending sms to $mobileNumber with ${token.id.value}" }
    if(mobileNumber == MobileNumber.TEST) return
    runCatching {
      Message.creator(
        PhoneNumber(mobileNumber.value),
        PhoneNumber(number),
        """
            Message
      """.trimIndent()
      )
        .create()
    }
  }


}
