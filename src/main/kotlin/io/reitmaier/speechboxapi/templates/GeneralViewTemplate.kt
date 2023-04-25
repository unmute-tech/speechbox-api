package io.reitmaier.speechboxapi.templates

import io.ktor.server.html.*
import kotlinx.datetime.*
import kotlinx.html.*
import io.reitmaier.speechboxapi.data.BoxId
import io.reitmaier.speechboxapi.data.BoxInfo
import io.reitmaier.speechboxapi.db.StoriesByDate
import kotlin.time.Duration.Companion.minutes

class StatusViewTemplate(
  private val boxes: List<BoxInfo>,
  private val duplicates: Long,
  private val unpaid: Long,
  private val storiesByDate: List<StoriesByDate>,
  private val now: Instant = Clock.System.now(),
): Template<FlowContent> {
  override fun FlowContent.apply() {
    h1(classes = "title") { +"SpeechBox Status"}
    insert(StoriesStatus(storiesByDate)){}
    h2(classes = "title") { +"Mobile/Payments" }
    p {
      +"duplicate mobile number submissions: "
      strong { +"$duplicates" }
    }
    p {
      +"unpaid story submissions: "
      strong { +"$unpaid" }
    }
    insert(BoxesStatus(boxes, now)){}
  }
}

class StoriesByDateRow(
  private val story: StoriesByDate,
) : Template<TR> {
  override fun TR.apply() {
    th { +"${story.year}-${story.month.toString().padStart(2,'0')}-${story.day.toString().padStart(2,'0')}" }
    td { +"${story.num_stories}"}
    }
  }

class StoriesStatus(
  private val stories: List<StoriesByDate>,
) : Template<FlowContent> {
  override fun FlowContent.apply() {
    h2(classes = "title") { +"Stories"}
    table(
      classes = "table is-bordered is-hoverable"
    ) {
      // TODO headers
      // TODO footers
      thead {
        tr {
          th { +"Date"}
          th { +"Stories"}
        }
      }
      tbody {
        for (s in stories) {
          tr {
            insert(StoriesByDateRow(s)) {}
          }
        }
      }
    }
  }

}
class BoxesStatus(
  private val boxes: List<BoxInfo>,
  private val now: Instant
) : Template<FlowContent> {
  override fun FlowContent.apply() {
    h2(classes = "title") { +"Boxes"}
    table(
      classes = "table is-bordered is-hoverable"
    ) {
      // TODO headers
      // TODO footers
      thead {
        tr {
          th { +"ID"}
          th { +"Description"}
          th { +"Stories"}
          th { +"Last Story"}
          th { +"Last Seen"}
        }
      }
      tbody {
        for (b in boxes) {
          tr {
            insert(BoxRow(b, now)) {}
          }
        }
      }
    }
    insert(BoxesMap(boxes)) {}
  }

}
class BoxesMap(
  private val boxes: List<BoxInfo>,
) : Template<FlowContent> {
  private data class BoxWithLocation(
    val id: BoxId,
    val longitude: Double,
    val latitude: Double,
    val photo: String?
  ) {
    fun toJS() : String {
      return """[$longitude, $latitude, "Box ${id.value}" ]"""
    }
    companion object {
      fun from(boxInfo: BoxInfo) : BoxWithLocation? =
        if(boxInfo.longitude == null || boxInfo.latitude == null) {
          null
        } else {
          BoxWithLocation(
            boxInfo.id,
            boxInfo.latitude,
            boxInfo.longitude,
            boxInfo.photo,
          )
        }
    }
  }
  override fun FlowContent.apply() {
    val boxesWithLocations = boxes.mapNotNull { BoxWithLocation.from(it)}
    if(boxesWithLocations.isEmpty()) return
    h3(classes = "title") { +"Locations"}
    div{
      id = "map"
    }
    script(type = ScriptType.textJavaScript) {
      unsafe {
        raw("""
            var markers = [
              ${boxesWithLocations.joinToString(separator = ",") { it.toJS() }}
            ];
            """.trimIndent()
        )
      }
    }
    script(
      src = "/static/js/map.js"
    ) {}
  }
}
class BoxRow(
  private val box: BoxInfo,
  private val now: Instant
) : Template<TR> {
  override fun TR.apply() {
    th { +"${box.id.value}" }
    td { +box.description }
    td { +"${box.num_stories}" }
    if(box.latestStory == null) {
      td { +"Never" }
    } else {
      val duration = now - box.latestStory
      val textColor = when {
        duration <= 15.minutes -> {
          "has-text-success"
        }
        duration <= 30.minutes -> {
          "has-text-warning"
        }
        else -> {
          "has-text-danger"
        }
      }
//      val duration: DateTimePeriod = box.last_seen.periodUntil(now, TimeZone.of(box.timezone))
      td(classes = textColor){
        span {
          attributes["data-tooltip"] = "${box.latestStory}"
          +"${duration.toString().substringBefore(".")}s ago"
        }
      }

    }
    if(box.last_seen == null) {
      td { +"Never" }
    } else {
      val duration = now - box.last_seen
      val textColor = when {
          duration <= 2.minutes -> {
            "has-text-success"
          }
          duration <= 5.minutes -> {
            "has-text-warning"
          }
          else -> {
            "has-text-danger"
          }
      }
//      val duration: DateTimePeriod = box.last_seen.periodUntil(now, TimeZone.of(box.timezone))
      td(classes = textColor){
        span {
          attributes["data-tooltip"] = "${box.last_seen}"
          +"${duration.toString().substringBefore(".")}s ago"
        }
      }

    }
  }
}
class Layout: Template<HTML> {
  val content =  Placeholder<HtmlBlockTag>()
//  val menu = TemplatePlaceholder<NavTemplate>()

  override fun HTML.apply() {
    head{
      title { +"Covid Stories" }
      meta { charset = "UTF-8" }
      meta {
        name = "viewport"
        content = "width=device-width, initial-scale=1"
      }

      link(
        rel = "stylesheet",
        href = "/static/css/status.css",
        type = "text/css"
      )

      link(
        rel = "stylesheet",
        href = "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.9.3/css/bulma.min.css",
        type = "text/css"
      ) {
        this.integrity = "sha512-IgmDkwzs96t4SrChW29No3NXBIBv8baW490zk5aXvhCD8vuZM3yUSkbyTBcXohkySecyzIrUwiF/qV0cuPcL3Q=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      link(
        rel = "stylesheet",
        href = "https://cdnjs.cloudflare.com/ajax/libs/bulma-tooltip/1.2.0/bulma-tooltip.min.css",
        type = "text/css"
      ) {
        this.integrity = "sha512-eQONsEIU2JzPniggWsgCyYoASC8x8nS0w6+e5LQZbdvWzDUVfUh+vQZFmB2Ykj5uqGDIsY7tSUCdTxImWBShYg=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      link(
        rel = "stylesheet",
        href = "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.css",
        type = "text/css"
      ) {
        this.integrity = "sha512-xodZBNTC5n17Xt2atTPuE1HxjVMSvLVW9ocqUKLsCC5CXdbqCmblAshOMAS6/keqq/sMZMZ19scR4PsZChSR7A=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      script(
        src = "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.min.js"
      ) {
        this.integrity = "sha512-SeiQaaDh73yrb56sTW/RgVdi/mMqNeM2oBwubFHagc5BkixSpP1fvqF47mKzPGWYSSy4RwbBunrJBQ4Co8fRWA=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }

      script(
        src = "https://cdnjs.cloudflare.com/ajax/libs/leaflet-providers/1.13.0/leaflet-providers.min.js"
      ) {
        this.integrity = "sha512-5EYsvqNbFZ8HX60keFbe56Wr0Mq5J1RrA0KdVcfGDhnjnzIRsDrT/S3cxdzpVN2NGxAB9omgqnlh4/06TvWCMw=="
        this.attributes["crossorigin"] = "anonymous"
        this.attributes["referrerpolicy"]="no-referrer"
      }
    }

    body{
      section(classes = "section") {
        div(classes = "container") {
          div(classes = "content") {
            insert(content)
          }
        }
      }

    }
  }

}