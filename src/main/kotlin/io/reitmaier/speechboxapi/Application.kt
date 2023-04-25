package io.reitmaier.speechboxapi

import com.github.michaelbull.logging.InlineLogger
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.reitmaier.speechboxapi.data.CovidStoriesRepo
import io.reitmaier.speechboxapi.plugins.configureDB
import io.reitmaier.speechboxapi.plugins.configureRouting
import io.reitmaier.speechboxapi.plugins.configureSerialization
import io.reitmaier.speechboxapi.services.TwilioService

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
  val log = InlineLogger()
  val db = configureDB()
  val sms = TwilioService("TODO", "TODO", "TODO")
  val repo = CovidStoriesRepo(db,sms)
  configureRouting(repo)
  configureSerialization()
  install(CallLogging)
}
