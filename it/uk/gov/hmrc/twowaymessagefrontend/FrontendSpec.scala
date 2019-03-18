package uk.gov.hmrc.twowaymessagefrontend


import org.scalatest._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Reads
import play.api.mvc.{Action, AnyContentAsEmpty, Results}
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, OptionalRetrieval, SimpleRetrieval}
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

//class ExampleSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {
//
//
//  "Routes" should {
//    implicit lazy val app1: Application = new GuiceApplicationBuilder().build()
//
//    val auth: (String, String) = AuthUtil.buildNinoUserToken().get//.getOrElse{throw new Exception("Auth Failed"); ("","")}
//
//
//    val x = ("Authorization", " Bearer BXQ3/Treo4kQCZvVcCqKPvsUI22i8aG3Wd+eEc/HH9szQdM5Spxc7RPKoN8ZZzs5GwdTG7FQy5kLbNJ716V+i5HH8L3C1EisnAszlWilfuOy2XB0rFUYwRWbyQU/paN18y84mDE+pb2207N0VmGE5dq02cqBfqycU8pfFEVbA439KwIkeIPK/mMlBESjue4V")
//
//    val auths: Seq[(String, String)] =  x :: Nil
//
//    "send 200 on a good request1" in  {
//      println(s">>>>>>>auth $auth")
//      val fakeFrequest = FakeRequest(GET, "/two-way-message-frontend/message/p800/make_enquiry", FakeHeaders(auths), AnyContentAsEmpty)
//      route(app1, fakeFrequest).map(status(_)) mustBe Some(OK)
//    }
//
////    "send 200 on a good request" in  {
////      route(app, FakeRequest(GET, "/two-way-message-frontend/message/p800/make_enquiry")).map(status(_)) mustBe Some(OK)
////    }
//
//  }
//
//}




/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.google.inject.AbstractModule
import controllers.{ControllerSpecBase, EnquiryController, IndexController}
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.Matchers
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.twowaymessagefrontend.util.MockAuthConnector
//import uk.gov.hmrc.twowaymessageadviserfrontend.connectors.mocks.MockAuthConnector
import play.api.test.Helpers._

import scala.concurrent.Future

class FrontendSpec extends ControllerSpecBase with Matchers with MockAuthConnector {

  override lazy val injector = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> false))
    .overrides(new AbstractModule with ScalaModule {
      override def configure(): Unit = {
        bind[AuthConnector].toInstance(mockAuthConnector)
      }
    })
    .injector()

  val enquiryController = injector.instanceOf[EnquiryController]

  "Index controller" should {
    //    "Given request when missing stride auth then request is redirected" in {
    //      mockAuthorise(AuthProviders(PrivilegedApplication))(Future.failed(UnsupportedAuthProvider()))
    //      val result = await(call(enquiryController.onPageLoad("p800"), fakeRequest))
    //      result.header.status should be 303
    //      result.header.headers.get("Location") should be Some("/stride/sign-in?successURL=http%3A%2F%2F%2F&origin=two-way-message-adviser-frontend")
    //    }

    "Given request when stride auth is present then request is successful" in {

      val nino = Nino(true, Some("AB123456C"))
      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))

      //      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some("")))

      val result = await(call(enquiryController.onPageLoad("p800"), fakeRequest))
      result.header.status shouldBe (200)
    }
  }

}