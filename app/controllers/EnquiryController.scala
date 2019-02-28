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
import connectors.{PreferencesConnector, TwoWayMessageConnector}
import forms.EnquiryFormProvider
import javax.inject.{Inject, Singleton}

import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolment}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.enquiry

import scala.concurrent.{ExecutionContext, Future}
import models.{EnquiryDetails, Identifier, MessageError}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HttpResponse

import ExecutionContext.Implicits.global

@Singleton
class EnquiryController @Inject()(appConfig: AppConfig,
                                  override val messagesApi: MessagesApi,
                                  formProvider: EnquiryFormProvider,
                                  val authConnector: AuthConnector,
                                  twoWayMessageConnector: TwoWayMessageConnector,
                                  preferencesConnector: PreferencesConnector)
  extends FrontendController with AuthorisedFunctions with I18nSupport {

  val form: Form[EnquiryDetails] = formProvider()

  def onPageLoad(queue: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(Enrolment("HMRC-NI")).retrieve(Retrievals.nino and Retrievals.email) {
        case nino ~ defaultEmail =>
          preferencesConnector.getPreferredEmail(nino.get, defaultEmail.get)
            .map(preferredEmail => {
              Ok(enquiry(appConfig, form, EnquiryDetails(queue, "", "", preferredEmail, preferredEmail)))
            }
            )
        case _ => Future.successful(Forbidden)
      }
    }

  def onSubmit(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(Enrolment("HMRC-NI")) {
        form.bindFromRequest().fold(
          (formWithErrors: Form[EnquiryDetails]) => {
            var returnedErrorForm = formWithErrors
            if(emailConfirmationError(formWithErrors)) {
              returnedErrorForm = appendEmailConfirmationError(formWithErrors)
            }
            Future.successful(BadRequest(enquiry(appConfig, returnedErrorForm, rebuildFailedForm(formWithErrors))))
          },
            enquiryDetails => {
              if(enquiryDetails.email != enquiryDetails.confirmEmail) {
                val errorForm = appendEmailConfirmationError(form)
                Future.successful(BadRequest(enquiry(appConfig, errorForm, enquiryDetails)))
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

  def emailConfirmationError(form: Form[EnquiryDetails]) = {
    val email = form.data.get("email")
    val confirmEmail = form.data.get("confirmEmail")
    (email.isEmpty || confirmEmail.isEmpty) || email.get != confirmEmail.get
  }

  def appendEmailConfirmationError(form: Form[EnquiryDetails]) = {
    val appendedErrors = form.errors ++ Seq(FormError("email", "Email addresses must match. Check them and try again."))
    form.copy(errors = appendedErrors)
  }

  private def rebuildFailedForm(formWithErrors: Form[EnquiryDetails]) = {
      EnquiryDetails(
        formWithErrors.data.get("queue").getOrElse(""),
        formWithErrors.data.get("subject").getOrElse(""),
        formWithErrors.data.get("content").getOrElse(""),
        formWithErrors.data.get("email").getOrElse(""),
        formWithErrors.data.get("confirmEmail").getOrElse(""))
    }
}