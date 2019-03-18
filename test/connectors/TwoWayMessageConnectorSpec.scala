///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package connectors
//
//import base.SpecBase
//import com.google.inject.AbstractModule
//import models.{ContactDetails, EnquiryDetails, TwoWayMessage}
//import net.codingwell.scalaguice.ScalaModule
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.when
//import play.api.Mode.Mode
//import play.api.http.Status
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.libs.json.{Json, Writes}
//import play.api.{Application, Mode}
//import play.mvc.Http
//import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
//import uk.gov.hmrc.play.bootstrap.http.HttpClient
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.{ExecutionContext, Future}
//
//class TwoWayMessageConnectorSpec extends SpecBase {
//
//  lazy implicit val hc = new HeaderCarrier()
//  lazy val mockHttpClient = mock[HttpClient]
//
//  override def fakeApplication(): Application = {
//
//    new GuiceApplicationBuilder()
//      .overrides(new AbstractModule with ScalaModule {
//        override def configure(): Unit = {
//          bind[Mode].toInstance(Mode.Test)
//          bind[HttpClient].toInstance(mockHttpClient)
//        }
//      })
//      .build()
//  }
//
//  val twoWayMessageConnector = injector.instanceOf[TwoWayMessageConnector]
//
//  "postMessage" should {
//
//    val twmPostMessageResponse = Json.parse(
//      """
//        |    {
//        |     "id":"5c18eb166f0000110204b160"
//        |    }""".stripMargin)
//
//    val details = EnquiryDetails(
//      "queue",
//      "my subject",
//      "my question",
//      "email@test.com",
//      "email@test.com"
//    )
//
//    val message = TwoWayMessage(
//      ContactDetails(details.email),
//      details.subject,
//      details.content
//    )
//
//    "respond with a mongo id after a successful call to two-way-message service results in a message creation from a valid payload" in {
//
//      when(mockHttpClient.POST[TwoWayMessage, HttpResponse](
//        any[String],
//        ArgumentMatchers.eq(message),
//        any[Seq[(String, String)]])(any[Writes[TwoWayMessage]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
//        .thenReturn(
//          Future.successful(HttpResponse(Http.Status.CREATED, Some(twmPostMessageResponse))
//        )
//      )
//
//      val result = await(twoWayMessageConnector.postMessage(details))
//      result.status shouldBe Status.CREATED
//    }
//
//    "respond with a 504 (GATEWAY TIMEOUT) after an unsuccessful call to two-way-message service results in the error message being propagated back up the chain" in {
//
//      when(mockHttpClient.POST[TwoWayMessage, HttpResponse](
//        any[String],
//        ArgumentMatchers.eq(message),
//        any[Seq[(String, String)]])(any[Writes[TwoWayMessage]],any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
//        .thenReturn(
//          Future.successful(HttpResponse(Http.Status.GATEWAY_TIMEOUT)
//        )
//      )
//
//      val result = await(twoWayMessageConnector.postMessage(details))
//      result.status shouldBe Status.GATEWAY_TIMEOUT
//    }
//  }
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

package connectors
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

class IndexControllerSpec extends ControllerSpecBase with Matchers with MockAuthConnector {

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
      mockAuthorise(Enrolment("HMRC-NI"), Retrievals.nino)(Future.successful(Some("AB123456C")))

//      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some("")))

      val result = await(call(enquiryController.onPageLoad("p800"), fakeRequest))
      result.header.status shouldBe (200)
    }
  }

}