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

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import config.AppConfig
import connectors.TwoWayMessageConnector
import forms.InquiryFormProvider
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolment, InsufficientEnrolments}
import play.api.mvc.{Action, AnyContent}
import views.html.inquiry

import scala.concurrent.Future
import utils.InputOption
import models.InquiryDetails

@Singleton
class InquiryController @Inject()(appConfig: AppConfig,
                                  override val messagesApi: MessagesApi,
                                  formProvider: InquiryFormProvider,
                                  val authConnector: AuthConnector,
                                  twoWayMessageConnector: TwoWayMessageConnector)
  extends FrontendController with AuthorisedFunctions with I18nSupport {

  def options: Seq[InputOption] = Seq(
    InputOption("queue1", "inquiry.dropdown.p1", Some("vat_vat-form")),
    InputOption("queue2", "inquiry.dropdown.p2", None),
    InputOption("queue99", "inquiry.dropdown.p3", None)
  )

  val form: Form[InquiryDetails] = formProvider(options)

  def onPageLoad(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(Enrolment("HMRC-NI")) {
        Future.successful(Ok(inquiry(appConfig, form, options)))
      } recoverWith {
        case InsufficientEnrolments(msg) => Future.successful(Unauthorized(msg))
      }
  }

  def onSubmit(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(Enrolment("HMRC-NI")) {
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
            Future.successful(BadRequest("Form error")),

          inquiryDetails => {
            twoWayMessageConnector.postMessage(inquiryDetails).map(response => response.status match {
              case CREATED => Redirect(routes.InquirySubmittedController.onPageLoad())
              case _ => BadGateway("Error sending inquiry details")
            })
          }
        )
      }
  }
}