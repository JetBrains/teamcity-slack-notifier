package jetbrains.buildServer.slackNotifications

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.JDOMUtil
import jetbrains.buildServer.configuration.ChangeListener
import jetbrains.buildServer.configuration.FileWatcher
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.ServerPaths
import jetbrains.buildServer.slackNotifications.teamcity.FilePersisterUtil
import jetbrains.buildServer.util.FileUtil
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import java.io.File
import java.io.IOException

class SlackNotifierConfig(
    serverPaths: ServerPaths,
    slackNotifierDescriptor: SlackNotifierDescriptor,

    private val slackNotifier: SlackNotifier
) : ChangeListener {
    private val configFilename = "slack.xml"

    private val logger = Logger.getInstance(
        SlackNotifierConfig::class.java.name
    )

    private val configDir = File(
        serverPaths.configDir,
        "_notifications/${slackNotifierDescriptor.type}"
    )
    private val configFile = File(configDir, configFilename)
    private val myChangeObserver = FileWatcher(configFile).also {
        it.sleepingPeriod = 10000
        it.registerListener(this)
        it.start()
    }

    private val isPausedProperty = "isPaused"
    private val botTokenProperty = "botToken"

    var isPaused = false
        set(value) {
            if (!value) {
                slackNotifier.clearAllErrors()
            }

            field = value
        }

    var botToken: String = ""

    init {
        reloadConfiguration()
    }

    override fun changeOccured(requestor: String?) {
        reloadConfiguration()
    }

    @Synchronized
    fun save() {
        val document = parseFile(configFile)
        if (document == null) {
            // config file is empty or corrupted. Try to overwrite it
            try {
                val rootElement = Element(botTokenProperty)
                FilePersisterUtil.saveDocument(Document(rootElement), configFile)
            } catch (e: IOException) {
                logger.error(
                    "I/O error occurred on attempt to write xml configuration file: " + configFile.absolutePath,
                    e
                )
            }
        }

        myChangeObserver.runActionWithDisabledObserver {
            FilePersisterUtil.processXmlFile(configFile,
                FileUtil.Processor { rootElement ->
                    rootElement.setAttribute(isPausedProperty, isPaused.toString())
                    rootElement.setAttribute(botTokenProperty, botToken)
                })
        }
    }

    @Synchronized
    private fun reloadConfiguration() {
        logger.info("Loading configuration file: " + configFile.absolutePath)
        var document = parseFile(configFile)
        if (document == null) {
            val defaultConfig = loadDefaultConfiguration()
            document = defaultConfig ?: return
        }
        val rootElement = document.rootElement
        isPaused = rootElement.getAttributeValue(isPausedProperty)?.toBoolean() ?: false
        botToken = rootElement.getAttributeValue(botTokenProperty) ?: ""
    }

    @Synchronized
    private fun loadDefaultConfiguration(): Document? {
        val resourceAsStream =
            javaClass.getResourceAsStream("defaultConfig.xml")
        if (resourceAsStream != null) {
            try {
                return JDOMUtil.loadDocument(resourceAsStream)
            } catch (e: JDOMException) {
                reportError("Failed to parse default plugin configuration", e)
            } catch (e: IOException) {
                reportError("I/O error occurred on attempt to parse default plugin configuration", e)
            }
        }
        logger.error("Failed to load default email notifier settings")
        return null
    }

    private fun parseFile(configFile: File): Document? {
        try {
            if (configFile.isFile) {
                return JDOMUtil.loadDocument(configFile)
            }
        } catch (e: JDOMException) {
            reportError("Failed to parse xml configuration file: " + configFile.absolutePath, e)
        } catch (e: IOException) {
            reportError(
                "I/O error occurred on attempt to parse xml configuration file: " + configFile.absolutePath,
                e
            )
        }
        return null
    }

    private fun reportError(message: String, e: Throwable) {
        logger.error(message)
        logger.debug(message, e)
        Loggers.SERVER.error(message)
        Loggers.SERVER.debug(message, e)
    }
}