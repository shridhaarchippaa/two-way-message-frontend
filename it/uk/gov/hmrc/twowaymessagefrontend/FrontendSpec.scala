package uk.gov.hmrc.twowaymessagefrontend


import org.scalatest._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContentAsEmpty, Results}
import uk.gov.hmrc.twowaymessagefrontend.util.AuthUtil
//import play.routing.Router
import play.api.routing.Router
import play.api.test._
//import play.api.test.Helpers.{GET => GET_REQUEST, _}
import play.api.test.Helpers.GET


import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ExampleSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {


  "Routes" should {
    implicit lazy val app1: Application = new GuiceApplicationBuilder().build()

    val auth: (String, String) = AuthUtil.buildNinoUserToken().get//.getOrElse{throw new Exception("Auth Failed"); ("","")}


    val x = ("Authorization", " Bearer BXQ3/Treo4kQCZvVcCqKPvsUI22i8aG3Wd+eEc/HH9szQdM5Spxc7RPKoN8ZZzs5GwdTG7FQy5kLbNJ716V+i5HH8L3C1EisnAszlWilfuOy2XB0rFUYwRWbyQU/paN18y84mDE+pb2207N0VmGE5dq02cqBfqycU8pfFEVbA439KwIkeIPK/mMlBESjue4V")

    val auths: Seq[(String, String)] =  x :: Nil

    "send 200 on a good request1" in  {
      println(s">>>>>>>auth $auth")
      val fakeFrequest = FakeRequest(GET, "/two-way-message-frontend/message/p800/make_enquiry", FakeHeaders(auths), AnyContentAsEmpty)
      route(app1, fakeFrequest).map(status(_)) mustBe Some(OK)
    }

//    "send 200 on a good request" in  {
//      route(app, FakeRequest(GET, "/two-way-message-frontend/message/p800/make_enquiry")).map(status(_)) mustBe Some(OK)
//    }

  }

}



