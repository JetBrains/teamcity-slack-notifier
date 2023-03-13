/*
 *  Copyright 2000-2023 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.util.HTTPRequestBuilder
import java.io.InputStream
import java.net.URI

class ResponseMock(
    private val myStatusCode: Int,
    private val myBodyString: String
): HTTPRequestBuilder.Response {
    override fun close() {
        TODO("Not yet implemented")
    }

    override fun getUri(): URI {
        TODO("Not yet implemented")
    }

    override fun getContentStream(): InputStream? {
        TODO("Not yet implemented")
    }

    override fun getBodyAsString(): String {
        return myBodyString
    }

    override fun getBodyAsString(p0: String?): String {
        return myBodyString
    }

    override fun getBodyAsStringLimit(p0: Int): String? {
        TODO("Not yet implemented")
    }

    override fun getStatusCode(): Int {
        return myStatusCode
    }

    override fun getStatusText(): String {
        TODO("Not yet implemented")
    }

    override fun getHeader(p0: String): String? {
        TODO("Not yet implemented")
    }
}