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
import models.{EnquiryDetails, Identifier, MessageError}
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, QueryStringBindable, Request}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.{enquiry, enquiry_submitted, error_template}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

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
          for {
            waitTime <- twoWayMessageConnector.getWaitTime(queue)
            email <- preferencesConnector.getPreferredEmail(nino)
          } yield {
            val backCode:Option[String] = request.queryString.get(BACKCODE).map( _.head)
            Ok(enquiry(appConfig, form, EnquiryDetails(queue, "", "", email, backCode), waitTime))
          }
        case _ => Future.successful(Forbidden)
      } recoverWith {
        case _ => Future.successful(InternalServerError)
      }
    }

  def onSubmit(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised(Enrolment("HMRC-NI")) {
        val queue = form.bindFromRequest().data("queue")
        twoWayMessageConnector.getWaitTime(queue).flatMap(waitTime =>
          form.bindFromRequest().fold(
            (formWithErrors: Form[EnquiryDetails]) => {
              Future.successful(BadRequest(enquiry(appConfig, formWithErrors, rebuildFailedForm(formWithErrors), waitTime)))
            },
            enquiryDetails => {
              submitMessage(enquiryDetails, waitTime)
            }
          )
        )
      }
  }

  def submitMessage(enquiryDetails: EnquiryDetails, waitTime: String)(implicit request: Request[_]) = {
      twoWayMessageConnector.postMessage(enquiryDetails).map(response => response.status match {
        case CREATED => extractId(response) match {
          case Right(id) => Ok(enquiry_submitted(appConfig, id.id, waitTime))
          case Left(error) => Ok(error_template("Error", "There was an error:", error.text, appConfig))
        }
        case _ => Ok(error_template("Error", "There was an error:", "Error sending enquiry details", appConfig))
      })
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
          case _ => Left("Unable find backCode value")
        }
      }
    }
    override def unbind(key: String, queryParams: QueryParams): String = {
      stringBinder.unbind("backCode", queryParams.backCode.getOrElse(""))
    }
  }
}
