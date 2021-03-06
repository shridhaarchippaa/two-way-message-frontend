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

package utils

import assets.Fixtures
import models.MessageFormat._
import models.ConversationItem
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Ignore, Matchers, WordSpec}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json

@Ignore
class MessageRendererSpec extends WordSpec  with MockitoSugar with  Fixtures with Matchers  {

  val injector = new GuiceApplicationBuilder()
    .injector()
    val messageRenderingService = injector.instanceOf[MessageRenderer]

    "MessageRenderingService.renderMessage" should {
            "render one message " in {
            val messageId = "12345"
            val messages = List(Json.parse(conversationItem(messageId)).validate[ConversationItem].get)

            val result = messageRenderingService.renderMessages(messages)
            result should be("<h1>")
            }
        }
}
