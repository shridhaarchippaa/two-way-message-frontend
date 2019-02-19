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

import config.AppConfig
import connectors.TwoWayMessageConnector
import forms.EnquiryFormProvider
import javax.inject.{Inject, Singleton}

import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolment}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.enquiry

import scala.concurrent.{ExecutionContext, Future}
import utils.InputOption
import models.{EnquiryDetails, Identifier, MessageError}
import uk.gov.hmrc.http.HttpResponse
import ExecutionContext.Implicits.global

@Singleton
class EnquiryController @Inject()(appConfig: AppConfig,
                                  override val messagesApi: MessagesApi,
                                  formProvider: EnquiryFormProvider,
                                  val authConnector: AuthConnector,
                                  twoWayMessageConnector: TwoWayMessageConnector)
  extends FrontendController with AuthorisedFunctions with I18nSupport {

  def options: Seq[InputOption] = Seq()

  val form: Form[EnquiryDetails] = formProvider()

  def onPageLoad(formType: String): Action[AnyContent] = Action.async {
    implicit request =>
      val e = InputOption(formType, formType.toUpperCase())
      authorised(Enrolment("HMRC-NI")) {
        Future.successful(Ok(enquiry(appConfig, form, Seq(e))))
      }
  }

  def onSubmit(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(Enrolment("HMRC-NI")) {
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            Future.successful(BadRequest(enquiry(appConfig,formWithErrors,options)))
          },
            enquiryDetails => {
              if(enquiryDetails.email != enquiryDetails.confirmEmail) {
                Future.successful(
                  BadRequest(
                    enquiry(
                      appConfig,
                      form.copy(errors = Seq(FormError("email", "The emails provided did not match"))),
                      options
                    )
                  )
                )
              } else {
                twoWayMessageConnector.postMessage(enquiryDetails).map(response => response.status match {
                  case CREATED => extractId(response) match {
                    case Right(id) => Redirect(routes.EnquirySubmittedController.onPageLoad(Some(id), None))
                    case Left(error) => Redirect(routes.EnquirySubmittedController.onPageLoad(None, Some(error)))
                  }
                  case _ => Redirect(routes.EnquirySubmittedController.onPageLoad(None,Some(MessageError("Error sending enquiry details"))))
                })
              }
            }
        )
      }
  }

  def messagesRedirect = Action {
    Redirect(appConfig.messagesFrontend)
  }

  def extractId(response: HttpResponse): Either[MessageError,Identifier] = {
    response.json.validate[Identifier].asOpt match {
      case Some(identifier) => Right(identifier)
      case None => Left(MessageError("Missing reference"))
    }
  }
}