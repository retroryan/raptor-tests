package fauna


import io.gatling.commons.validation.{Validation, Failure => GatlingFailure, Success => GatlingSuccess}
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt
import scala.util.Random


/**
  * *
  * The test time is in minutes
  * *

  export EVENT_SERVER_HOST=localhost
  export EVENT_SERVER_PORT=9090
  export TEST_TIME=5
  export USER_LOAD=5

  *
  */
class EventSourceLoad extends Simulation {

  private val EVENT_SERVER_HOST = sys.env("EVENT_SERVER_HOST")
  private val EVENT_SERVER_PORT = sys.env("EVENT_SERVER_PORT")

  private val eventServerUrl = s"http://${EVENT_SERVER_HOST}:${EVENT_SERVER_PORT}"
  println(s"EVENT SERVER URL: $eventServerUrl")

  private val TEST_TIME = sys.env("TEST_TIME").toInt
  private val TIME_FOR_RAMP = 60.seconds
  private val USER_LOAD = sys.env.get("USER_LOAD").map(_.toInt).getOrElse(5)

  val httpConf = http
    .baseURL(s"$eventServerUrl")

  val accumulate =
    exec(
      exec { session =>
        //println(s"USERID: ${session.userId}")
        //session.attributes.keys.foreach(k => println(s"$k  ${session.attributes(k)} " ))
        session.set("version", 0).set("userid", session.userId)
      },
      //doIf("${clear}")(exec(http("clear").put("/clear/${memberId}"))),
      repeat(10, "n") {
        exec(
          doIfOrElse("${clear}") {
            exec(session => session.set("accrual", scala.math.pow(2, session("n").as[Int].toDouble).toInt))
          } {
            exec(session => session.set("accrual", scala.math.pow(2, (10 + session("n").as[Int]).toDouble).toInt))
          },
          exec {
            http("add")
              .post("/add")
              .body(StringBody("""{ "clientId": ${clientId}, "counter": ${counter}, "type":"${type}","description":"${description}", "amount":${amount} }"""))
              .check(bodyString.transform(x => LoadGenerator.applied(x)).saveAs("event_saved"))
          },
          exec(session => session.set("version", session("version").as[Int] + 3)),
        )
      },
      doIf("${event_saved}") {
        println()
        exec {
          http("versions")
            .get("/all/${memberId}")
            .check(
              bodyString
                .transformOption(x => LoadGenerator.parseVersions(x))
                .is(29.to(0).by(-1).toList))
        }
      }
    )

  val scn = scenario("accumulate and sum")
    .feed(LoadGenerator.rangeGenerator)
    .exec(accumulate)

  setUp(
    scn.inject(
      rampUsersPerSec(1).to(USER_LOAD.toDouble).during(TIME_FOR_RAMP),
      constantUsersPerSec(USER_LOAD.toDouble).during(TEST_TIME.minutes.minus(TIME_FOR_RAMP))
    )
  ).protocols(httpConf.shareConnections)
}

object LoadGenerator {
  import io.circe._
  import io.circe.generic.semiauto._
  import io.circe.parser.decode

  def create(x: Int): Map[String, Any] = Map("memberId" -> x, "clear" -> true)

  def noclear(m: Map[String, Any]): Map[String, Any] = m.updated("clear", false)

  val rangeGenerator: Iterator[Map[String, Any]] = Iterator.range(0, Int.MaxValue).flatMap(x => Seq(create(x), noclear(create(x))).iterator)

  def parseVersions(obody: Option[String]): Validation[Option[Seq[Int]]] = {
    obody match {
      case Some(body) => decode[List[LedgerEvent]](body)
        .right.map(_.map(_.version))
        .fold(ex => GatlingFailure(s"$ex"), vs => GatlingSuccess(Some(vs)))
      case None    => GatlingSuccess(None)
    }
  }

  def applied(body: String): Boolean = {
    decode[WriteResult](body) match {
      case Right(Success(_)) => true
      case Right(Failure(_)) => false
      case Left(ex) => throw new Exception(s"error parsing response: $body  ex: ${ex.get}")
    }
  }

  sealed trait WriteResult
  case class Success(version: Long) extends WriteResult
  case class Failure(expectedVersion: Long) extends WriteResult

  object WriteResult {
    implicit val decoder: Decoder[WriteResult] = deriveDecoder
  }

  case class LedgerEvent(memberId: Int, transactionId: Long, version: Int, event: Seq[Long])
  object LedgerEvent {
    implicit val decoder: Decoder[LedgerEvent] = deriveDecoder
    implicit val encoder: Encoder[LedgerEvent] = deriveEncoder
  }
}
