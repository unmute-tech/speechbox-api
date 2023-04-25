package io.reitmaier.speechboxapi.plugins

import com.mysql.cj.jdbc.Driver
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import kotlinx.datetime.*
import io.reitmaier.speechboxapi.data.*
import io.reitmaier.speechboxapi.db.*

import java.util.UUID
import javax.sql.DataSource

fun Application.configureDB(): SpeechBoxDb {
  val dbConfig = environment.config.config("database")
  val host = dbConfig.property("host").getString()
  val port = dbConfig.property("port").getString()
  val database = dbConfig.property("db").getString()
  val user = dbConfig.property("user").getString()
  val passwd = dbConfig.property("password").getString()
  val maxPoolSize = dbConfig.property("maxPoolSize").getString().toInt()
  val testing = dbConfig.propertyOrNull("testing")

  val url = "jdbc:mysql://$host:$port/$database"

  val datasourceConfig = HikariConfig().apply {
    jdbcUrl = url
    username = user
    password = passwd
    maximumPoolSize = maxPoolSize

    // Driver needs to be explicitly set in order to produce fatjar
    // https://github.com/brettwooldridge/HikariCP/issues/540
    driverClassName = Driver::class.java.name
  }
  val dataSource : DataSource = HikariDataSource(datasourceConfig)
  val driver : SqlDriver = dataSource.asJdbcDriver()


  val db = SpeechBoxDb(
    driver = driver,
    boxAdapter = Box.Adapter(
      idAdapter = boxIdAdapter,
      last_seenAdapter = timestampAdapter,
      deployed_atAdapter = timestampAdapter
    ),
    storyAdapter = Story.Adapter(
      idAdapter = storyIdAdapter,
      box_idAdapter = boxIdAdapter,
      created_atAdapter = timestampAdapter,
      tokenAdapter = participationTokenAdapter,
      updated_atAdapter = timestampAdapter,
      session_idAdapter = sessionIdAdapter,
    ),
    tokenAdapter = Token.Adapter(
      idAdapter = participationTokenAdapter,
      issued_atAdapter = timestampAdapter,
      sent_atAdapter = timestampAdapter,
      completed_atAdapter = timestampAdapter,
    ),
    sessionAdapter = Session.Adapter(
      idAdapter = sessionIdAdapter,
      box_idAdapter = boxIdAdapter,
      idle_stateAdapter = timestampAdapter,
      confirmation_stateAdapter = timestampAdapter,
      questionnaire_no_share_stateAdapter = timestampAdapter,
      questionnaire_share_stateAdapter = timestampAdapter,
      recording_stateAdapter = timestampAdapter,
      thank_you_promt_stateAdapter = timestampAdapter,
      token_promt_stateAdapter = timestampAdapter,
      no_token_prompt_stateAdapter = timestampAdapter,
      welcome_stateAdapter = timestampAdapter,
      tokenAdapter = participationTokenAdapter,
      audio_error_stateAdapter = timestampAdapter,
      init_stateAdapter = timestampAdapter,
    ),
    mobileAdapter = Mobile.Adapter(
      box_idAdapter = boxIdAdapter,
      created_atAdapter = timestampAdapter,
      numberAdapter = mobileNumberAdapter,
      session_idAdapter = sessionIdAdapter,
      idAdapter = mobileIdAdapter,
    ),
  )

  driver.migrate(db)

  environment.monitor.subscribe(ApplicationStopped) { driver.close() }

  return db
}
private fun SqlDriver.migrate(database: SpeechBoxDb) {
  // Settings table is version 2
  SpeechBoxDb.Schema.migrate(this, 1, 2)
  val settings = database.settingsQueries.getSettings().executeAsOne()
  val dbVersion = settings.version
  val schemaVersion = SpeechBoxDb.Schema.version
  println("Current db version: $dbVersion")
  for (version in (dbVersion until schemaVersion)) {
    println("Migrating to ${version + 1}")
    SpeechBoxDb.Schema.migrate(this, version, version + 1)
    database.settingsQueries.setVersion(version + 1)
  }
}

private val boxIdAdapter = object : ColumnAdapter<BoxId, Int> {
  override fun decode(databaseValue: Int): BoxId = BoxId(databaseValue)
  override fun encode(value: BoxId): Int = value.value
}

private val storyIdAdapter = object : ColumnAdapter<StoryId, Int> {
  override fun decode(databaseValue: Int): StoryId = StoryId(databaseValue)
  override fun encode(value: StoryId): Int = value.value
}

private val mobileIdAdapter = object : ColumnAdapter<MobileId, Int> {
  override fun decode(databaseValue: Int): MobileId = MobileId(databaseValue)
  override fun encode(value: MobileId): Int = value.value
}

private val sessionIdAdapter = object : ColumnAdapter<SessionId, String> {
  override fun decode(databaseValue: String): SessionId = SessionId(UUID.fromString(databaseValue))
  override fun encode(value: SessionId): String = value.value.toString()
}

private val participationTokenAdapter = object : ColumnAdapter<ParticipationToken, String> {
  override fun decode(databaseValue: String) = ParticipationToken(databaseValue)
  override fun encode(value: ParticipationToken) = value.value
}

private val mobileNumberAdapter = object : ColumnAdapter<MobileNumber, String> {
  override fun decode(databaseValue: String) = MobileNumber(databaseValue)
  override fun encode(value: MobileNumber) = value.value
}

val timestampAdapter = object : ColumnAdapter<Instant, Long> {
  override fun decode(databaseValue: Long) =
    Instant.fromEpochMilliseconds(databaseValue)
  override fun encode(value: Instant) = value.toEpochMilliseconds()
}

