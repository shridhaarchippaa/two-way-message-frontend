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

import java.util.Base64

import config.AppConfig
import connectors.{PreferencesConnector, TwoWayMessageConnector}
import forms.EnquiryFormProvider
import javax.inject.{Inject, Singleton}
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolment}
import play.api.mvc.{Action, AnyContent, QueryStringBindable}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.{enquiry, enquiry_submitted, error_template}

import scala.concurrent.{ExecutionContext, Future}
import models.{EnquiryDetails, Identifier, MessageError}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HttpResponse

import ExecutionContext.Implicits.global
import scala.util.Try

@Singleton
class EnquiryController @Inject()(appConfig: AppConfig,
                                  override val messagesApi: MessagesApi,
                                  formProvider: EnquiryFormProvider,
                                  val authConnector: AuthConnector,
                                  twoWayMessageConnector: TwoWayMessageConnector,
                                  preferencesConnector: PreferencesConnector)
  extends FrontendController with AuthorisedFunctions with I18nSupport {

  private final val BACKCODE = "backCode"

  val form: Form[EnquiryDetails] = formProvider()

  def onPageLoad(queue: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(Enrolment("HMRC-NI")).retrieve(Retrievals.nino) {
        case Some(nino) =>
          val backCode:Option[String] = request.queryString.get(BACKCODE).map( _.head)//.map( s => new String(Base64.getDecoder.decode(s)))

          preferencesConnector.getPreferredEmail(nino).map(preferredEmail => {
              Ok(enquiry(appConfig, form, EnquiryDetails(queue, "", "", preferredEmail, backCode)))
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
            Future.successful(BadRequest(enquiry(appConfig, formWithErrors, rebuildFailedForm(formWithErrors))))
          },
            enquiryDetails => {
                twoWayMessageConnector.postMessage(enquiryDetails).map(response => response.status match {
                  case CREATED => extractId(response) match {
                    case Right(id) => Ok(enquiry_submitted(appConfig, id.id))
                    case Left(error) => Ok(error_template("Error", "There was an error:", error.text, appConfig))
                  }
                  case _ =>
                    Ok(error_template("Error", "There was an error:", "Error sending enquiry details", appConfig))
                })
              }
        )
      }
  }

  def messagesRedirect = Action {
    Redirect(appConfig.messagesFrontendUrl)
  }

  def personalAccountRedirect = Action {
    Redirect(appConfig.personalAccountUrl)
  }

  def extractId(response: HttpResponse): Either[MessageError,Identifier] = {
    response.json.validate[Identifier].asOpt match {
      case Some(identifier) => Right(identifier)
      case None => Left(MessageError("Missing reference"))
    }
  }

  private def rebuildFailedForm(formWithErrors: Form[EnquiryDetails]) = {
      EnquiryDetails(
        formWithErrors.data.getOrElse("queue", ""),
        formWithErrors.data.getOrElse("subject", ""),
        formWithErrors.data.getOrElse("question", ""),
        formWithErrors.data.getOrElse("email", ""))
    }



  case class QueryParams( backCode: Option[String] = None)

  implicit def queryStringBindable(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[QueryParams] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, QueryParams]] = {
      for {
        backCode <- stringBinder.bind("backCode", params)
      } yield {
        backCode match {
          case Right(backCode) => Right(QueryParams(Some(backCode)))
          case _ => Left("Unable to bind an AgeRange")
        }
      }
    }
    override def unbind(key: String, ageRange: QueryParams): String = {
      stringBinder.unbind("backCode", ageRange.backCode.getOrElse(""))
    }
  }


}
