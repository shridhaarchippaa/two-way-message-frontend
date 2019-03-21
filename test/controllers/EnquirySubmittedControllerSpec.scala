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
//package controllers
//
//import com.google.inject.AbstractModule
//import connectors.TwoWayMessageConnector
//import connectors.mocks.MockAuthConnector
//import models.{Identifier, MessageError}
//import net.codingwell.scalaguice.ScalaModule
//import org.jsoup.Jsoup
//import play.api.Application
//import play.api.http.Status
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.test.Helpers._
//import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment}
//import uk.gov.hmrc.domain.Nino
//
//import scala.concurrent.Future
//
//class EnquirySubmittedControllerSpec extends ControllerSpecBase with MockAuthConnector {
//
//  lazy val mockTwoWayMessageConnector = mock[TwoWayMessageConnector]
//
//  override def fakeApplication(): Application = {
//
//    new GuiceApplicationBuilder()
//      .overrides(new AbstractModule with ScalaModule {
//        override def configure(): Unit = {
//          bind[TwoWayMessageConnector].toInstance(mockTwoWayMessageConnector)
//          bind[AuthConnector].toInstance(mockAuthConnector)
//        }
//      })
//      .build()
//  }
//
//  val controller = injector.instanceOf[EnquirySubmittedController]
//
//  // Please see integration tests for auth failure scenarios as these are handled by the ErrorHandler class
//  "calling onPageLoad() with a valid Nino (HMRC-NI) enrolment" should {
//
//    "return 200 (OK) when presented with a valid identifier" in {
//      val nino = Nino("AB123456C")
//      val identifier = Identifier("5c18eb166f0000110204b160")
//      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
//      val result = call(controller.onPageLoad(Some(identifier), None), fakeRequest)
//      status(result) shouldBe Status.OK
//      val document = Jsoup.parse(contentAsString(result))
//      document.getElementsByClass("content__body")
//        .text()
//        .contains("Message Sent HMRC received your message and will reply within 5 working days") shouldBe true
//    }
//
//    "return 200 (OK) when presented with a missing identifier and a valid error message" in {
//      val nino = Nino("AB123456C")
//      val identifier = Identifier("5c18eb166f0000110204b160")
//      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
//      val result = call(controller.onPageLoad(None, Some(MessageError("Error 123"))), fakeRequest)
//      status(result) shouldBe Status.OK
//      val document = Jsoup.parse(contentAsString(result))
//      document.getElementsByClass("page-header").text() shouldBe "There was an error:"
//      document.getElementById("content").text().contains("Error 123") shouldBe true
//    }
//
//    "return 200 (OK) when presented with a missing identifier and a missing error message" in {
//      val nino = Nino("AB123456C")
//      val identifier = Identifier("5c18eb166f0000110204b160")
//      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
//      val result = call(controller.onPageLoad(None, None), fakeRequest)
//      status(result) shouldBe Status.OK
//      val document = Jsoup.parse(contentAsString(result))
//      document.getElementsByClass("page-header").text() shouldBe "Missing reference number!"
//    }
//  }
//}
