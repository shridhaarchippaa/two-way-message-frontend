package uk.gov.hmrc.twowaymessagefrontend.util

import play.api.libs.json.{Json, Reads}

import scala.concurrent.Await

import akka.actor.ActorSystem
import play.api.libs.ws.ahc.AhcWSClient


object AuthUtil {


  implicit val hc = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()
  implicit val materializer = akka.stream.ActorMaterializer()

  val httpClient = AhcWSClient()


  lazy val authPort = 8500
  lazy val ggAuthPort = 8585 // externalServicePorts.get("auth-login-api").get

  implicit val deserialiser: Reads[GatewayToken] = Json.reads[GatewayToken]

  case class GatewayToken( gatewayToken: String)

  private val STRIDE_USER_PAYLOAD =
    """
      | {
      |  "clientId" : "id",
      |  "enrolments" : [],
      |  "ttl": 1200
      | }
    """.stripMargin

  private val GG_NINO_USER_PAYLOAD =
    """
      | {
      |  "credId": "1234",
      |  "affinityGroup": "Individual",
      |  "confidenceLevel": 300,
      |  "credentialStrength": "strong",
      |  "nino": "AA000108C",
      |  "enrolments": []
      |  }
    """.stripMargin

  private val GG_SA_USER_PAYLOAD =
    """
      | {
      |  "credId": "1235",
      |  "affinityGroup": "Organisation",
      |  "confidenceLevel": 100,
      |  "credentialStrength": "none",
      |  "enrolments": [
      |      {
      |        "key": "IR-SA",
      |        "identifiers": [
      |          {
      |            "key": "UTR",
      |            "value": "1234567890"
      |          }
      |        ],
      |        "state": "Activated"
      |      }
      |    ]
      |  }
    """.stripMargin

  private def buildUserToken(payload: String): Option[(String, String)] = {
    val x = httpClient.url(s"http://localhost:8585/government-gateway/session/login")
      .withHeaders(("Content-Type", "application/json"))
      .post(payload)


    val response = Await.result(x, scala.concurrent.duration.Duration.Inf)

    val head = response.header("Authorization").get

    Some( ("Authorization", head ) )

  }

  def buildNinoUserToken(): Option[(String, String)] = buildUserToken(GG_NINO_USER_PAYLOAD)

  def buildSaUserToken(): Option[(String, String)] = buildUserToken(GG_SA_USER_PAYLOAD)

}
