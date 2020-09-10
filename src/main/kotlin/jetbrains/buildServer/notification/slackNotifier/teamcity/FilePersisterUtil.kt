/*
 *  Copyright 2000-2020 JetBrains s.r.o.
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

package jetbrains.buildServer.notification.slackNotifier.teamcity

import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.XmlUtil
import org.jdom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

object FilePersisterUtil {
    /**
     * Same as [FileUtil.processXmlFile], but saves result
     * via temp file.
     *
     * @param file file to process
     * @param p    processor
     */
    fun processXmlFile(file: File, p: FileUtil.Processor) {
        if (file.exists()) {
            try {
                val document = FileUtil.parseDocument(file).document
                p.process(document.rootElement)
                saveDocument(document, file)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
    /**
     * Saves the specified document to the target file via the temp file
     *
     * @param document          document to save
     * @param targetFile        file where document should be saved
     * @param beforeWriteAction action to run before renaming temp file to the targetFile
     * @throws IOException in case of IO error
     * @see FileUtil.writeViaTmpFile
     */
    /**
     * Saves the specified document to the target file via the temp file
     *
     * @param document   document to save
     * @param targetFile file where document should be saved
     * @throws IOException in case of IO error
     * @see .saveDocument
     */
    fun saveDocument(
        document: Document,
        targetFile: File,
        beforeWriteAction: FileUtil.IOAction = FileUtil.IOAction.DO_NOTHING
    ) {
        val content = ByteArrayOutputStream()
        XmlUtil.saveDocument(document, content)
        FileUtil.writeViaTmpFile(
            targetFile,
            ByteArrayInputStream(content.toByteArray()),
            beforeWriteAction
        )
    }
}