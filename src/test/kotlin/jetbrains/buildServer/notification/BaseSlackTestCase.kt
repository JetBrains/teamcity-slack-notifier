package jetbrains.buildServer.notification

import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.messages.DefaultMessagesInfo
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.SUser
import org.testng.annotations.BeforeMethod
import java.util.function.BooleanSupplier

open class BaseSlackTestCase : BaseNotificationRulesTestCase() {
    protected lateinit var mySlackApi: MockSlackWebApi
    protected lateinit var myDescriptor: SlackNotifierDescriptor
    protected lateinit var myNotifier: SlackNotifier
    protected lateinit var myUser: SUser

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()

        setInternalProperty("teamcity.notifications.quiet.period.seconds", "0")
        myFixture.notificationProcessor.setBuildFailingDelay(700)
        myFixture.notificationProcessor.setCheckHangedBuildsInterval(50)

        val connectionManager = OAuthConnectionsManager(myFixture.getSingletonService(ExtensionHolder::class.java))

        val slackApiFactory = MockSlackWebApiFactory()
        mySlackApi = slackApiFactory.createSlackWebApi()

        myDescriptor = SlackNotifierDescriptor(MockServerPluginDescriptior())
        myNotifier = SlackNotifier(
            myFixture.notificatorRegistry,
            slackApiFactory,
            SimpleMessageBuilder(
                SlackMessageFormatter(),
                myFixture.webLinks,
                myProjectManager
            ),
            myFixture.serverPaths,
            myProjectManager,
            connectionManager,
            myDescriptor
        )

        myFixture.addService(myDescriptor)
        myFixture.addService(myNotifier)

        connectionManager.addConnection(
            myProject,
            SlackConnection.type,
            mapOf("externalId" to "test_connection", "secure:token" to "test_token")
        )

        myUser = createUser("test_user")
        myUser.setUserProperty(myDescriptor.channelProperty, "#test_channel")
        myUser.setUserProperty(myDescriptor.connectionProperty, "test_connection")
        makeProjectAccessible(myUser, myProject.projectId)
    }

    fun `given user is subscribed to`(vararg events: NotificationRule.Event) {
        storeRules(myUser, myNotifier, newRule(*events))
    }

    fun `when build starts`(): SBuild = startBuild()
    fun `when build finishes`(): SBuild {
        startBuild()
        return finishBuild()
    }
    fun `when build fails`(): SBuild {
        return createFailedBuild()
    }
    fun `when build is failing`(): SBuild {
        val build = startBuild()

        myFixture.logBuildMessages(
            build,
            listOf(
                DefaultMessagesInfo.createBlockStart(
                    "test1",
                    DefaultMessagesInfo.BLOCK_TYPE_TEST
                )
            )
        )
        myFixture.logBuildMessages(
            build,
            listOf(
                DefaultMessagesInfo.createTestFailure(
                    "test1",
                    Exception("test1 failed")
                )
            )
        )

        myFixture.logBuildMessages(
            build,
            listOf(
                DefaultMessagesInfo.createBlockEnd(
                    "test1",
                    DefaultMessagesInfo.BLOCK_TYPE_TEST
                )
            )
        )


        Thread.sleep(1500)

        return build
    }
    fun `when build hangs`(): SBuild {
        val build = startBuild()
        makeProjectAccessible(myUser, myBuildType.projectId)
        makeBuildHanging(build)
        return build
    }

    fun `then message should contain`(vararg strs: String) {
        waitForAssert(BooleanSupplier {
            mySlackApi.messages.isNotEmpty()
        }, 2000L)

        for (str in strs) {
            assertContains(mySlackApi.messages.last().text, str)
        }
    }

    fun `then no messages should be sent`() {
        assertEmpty(mySlackApi.messages)
    }

    private fun <T> Iterable<T>.last(): T {
        return reversed().first()
    }
}