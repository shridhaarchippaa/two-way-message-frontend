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
import scala.util.Random

class FrontendSpec extends ControllerSpecBase  with MockAuthConnector with HtmlUnitFactory with   OneBrowserPerSuite{

  override lazy val injector = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> false, "testserver.port" -> 8990))
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
      mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some("AB123456C")))

      val result = await(call(enquiryController.onPageLoad("p800"), fakeRequest))
      result.header.status mustBe (200)
    }

    "display error message if nothing entered for Subject" in {
      stubLogin("AB123456C")

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      click on find(id("submit")).value

      eventually { pageSource must include ("Please enter a subject") }
    }

    "display error message if subject is longer than max" in {
      stubLogin("AB123456C")

      go to s"http://localhost:$port/two-way-message-frontend/message/p800/make_enquiry"

      textField("subject").value = Random.nextString(80)

      click on find(id("submit")).value

      eventually { pageSource must include ("Subject has a maximum length of 65 characters") }
    }

  }



  def stubLogin( nino:String): Unit = {
    mockAuthorise(Enrolment("HMRC-NI"), OptionalRetrieval("nino", Reads.StringReads))(Future.successful(Some(nino)))
    mockAuthorise(Enrolment("HMRC-NI"))(Future.successful(Some("AB123456C")))
  }


}