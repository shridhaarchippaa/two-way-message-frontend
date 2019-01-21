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

package controllers

import com.google.inject.AbstractModule
import connectors.TwoWayMessageConnector
import connectors.mocks.MockAuthConnector
import models.InquiryDetails
import net.codingwell.scalaguice.ScalaModule
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.mvc.Http
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class InquiryControllerSpec extends ControllerSpecBase with MockAuthConnector {

  lazy val mockTwoWayMessageConnector = mock[TwoWayMessageConnector]

  override def fakeApplication(): Application = {

    new GuiceApplicationBuilder()
      .overrides(new AbstractModule with ScalaModule {
        override def configure(): Unit = {
          bind[TwoWayMessageConnector].toInstance(mockTwoWayMessageConnector)
          bind[AuthConnector].toInstance(mockAuthConnector)
        }
      })
      .build()
  }

  val controller = injector.instanceOf[InquiryController]

  // Please see integration tests for auth failure scenarios as these are handled by the ErrorHandler class
  "calling onPageLoad()" should {

    "return 200 (OK) when presented with a valid Nino (HMRC-NI) enrolment from auth-client" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      val result = call(controller.onPageLoad(), fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  // Please see integration tests for auth failure scenarios as these are handled by the ErrorHandler class
  "calling onSubmit()" should {

    val fakeRequestWithForm = FakeRequest(routes.InquiryController.onSubmit())
    val requestWithFormData: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequestWithForm.withFormUrlEncodedBody(
      "queue" -> "queue1",
      "email" -> "test@test.com",
      "subject" -> "test subject",
      "content" -> "test content"
    )
    val inquiryDetails = InquiryDetails(
      "queue1",
      "test@test.com",
      "test subject",
      "test content"
    )
    val badRequestWithFormData: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequestWithForm.withFormUrlEncodedBody(
      "bad" -> "balue"
    )

    "return 201 (CREATED) when presented with a valid Nino (HMRC-NI) credentials and valid payload" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      when(mockTwoWayMessageConnector.postMessage(ArgumentMatchers.eq(inquiryDetails))(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.CREATED, Some(Json.parse("{\"id\":\"5c18eb2e6f0000100204b161\"}")))
        )
      )
      val result = call(controller.onSubmit(), requestWithFormData)
      status(result) shouldBe Status.SEE_OTHER
    }

    "return 400 (BAD_REQUEST) when presented with invalid form data" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      val result = call(controller.onSubmit(), badRequestWithFormData)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "return 502 (BAD_GATEWAY) when two-way-message service returns a different status than 201 (CREATED)" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      when(mockTwoWayMessageConnector.postMessage(ArgumentMatchers.eq(inquiryDetails))(any[HeaderCarrier])).thenReturn(
        Future.successful(
          HttpResponse(Http.Status.CONFLICT)
        )
      )
      val result = call(controller.onSubmit(), requestWithFormData)
      status(result) shouldBe Status.BAD_GATEWAY
    }

    "return 400 (BAD_REQUEST) when provided with an invalid form" in {
      val nino = Nino("AB123456C")
      mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some(nino.value)))
      val result = call(controller.onSubmit(), fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }
  }
}
