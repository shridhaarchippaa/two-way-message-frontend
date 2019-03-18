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

package uk.gov.hmrc.twowaymessagefrontend


import uk.gov.hmrc.twowaymessagefrontend.util.ControllerSpecBase
import play.api.libs.json.Reads
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, OptionalRetrieval, SimpleRetrieval}
import com.google.inject.AbstractModule
import controllers.EnquiryController
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.Matchers
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.twowaymessagefrontend.util.MockAuthConnector
import play.api.test.Helpers._

import scala.concurrent.Future

class FrontendSpec extends ControllerSpecBase  with MockAuthConnector with HtmlUnitFactory with   OneBrowserPerSuite{

  override lazy val injector = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> false))
    .overrides(new AbstractModule with ScalaModule {
      override def configure(): Unit = {
        bind[AuthConnector].toInstance(mockAuthConnector)
      }
    })
    .injector()

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(Configuration("metrics.enabled" -> false))
      .overrides(new AbstractModule with ScalaModule {
        override def configure(): Unit = {
          bind[AuthConnector].toInstance(mockAuthConnector)
        }
      }).build()
  }

  val enquiryController = injector.instanceOf[EnquiryController]

  "Frontend test" should {
    "find the home page ok" in {
      val nino = Nino(true, Some("AB123456C"))
      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))

      val result = await(call(enquiryController.onPageLoad("p800"), fakeRequest))
      result.header.status mustBe (200)
    }

    "display no subject if nothing entered" in {
      val nino = Nino(true, Some("AB123456C"))
      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      // PRESS SUBMIT

      pageSource.toString must include ("Please enter a subject")

    }

  }




}