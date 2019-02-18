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

package forms

import forms.mappings.Mappings
import javax.inject.Inject
import models.EnquiryDetails
import play.api.data.Form
import play.api.data.Forms._
import utils.InputOption

class EnquiryFormProvider @Inject() extends FormErrorHelper with Mappings {
  private val SUBJECT_MAX_LENGTH = 60

  def apply(queueOptions: Seq[InputOption]): Form[EnquiryDetails] =
    Form(
      mapping(
        "queue" -> nonEmptyText,
        "email" -> tuple(
          "email" -> email,
          "confirm" -> email
        ).verifying(
          "Emails don't match", email => email._1 == email._2
        ),
        "subject" -> nonEmptyText(maxLength = SUBJECT_MAX_LENGTH),
        "content" -> text()
      )(EnquiryDetails.apply)(EnquiryDetails.unapply)
    )

}
