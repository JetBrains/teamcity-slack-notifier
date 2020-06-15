package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.messages.DefaultMessagesInfo
import jetbrains.buildServer.messages.Status
import jetbrains.buildServer.notification.*
import jetbrains.buildServer.notification.slackNotifier.notification.*
import jetbrains.buildServer.notification.slackNotifier.slack.MockSlackWebApi
import jetbrains.buildServer.notification.slackNotifier.slack.MockSlackWebApiFactory
import jetbrains.buildServer.notification.slackNotifier.slack.SlackMessageFormatter
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.impl.NotificationRulesConstants
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.vcs.ModificationDataForTest
import org.testng.annotations.BeforeMethod
import java.util.function.BooleanSupplier

open class BaseSlackTestCase : BaseNotificationRulesTestCase() {
    protected lateinit var mySlackApiFactory: MockSlackWebApiFactory
    private lateinit var mySlackApi: MockSlackWebApi
    protected lateinit var myDescriptor: SlackNotifierDescriptor
    private lateinit var myNotifier: SlackNotifier
    private lateinit var myUser: SUser
    private lateinit var myAssignerUser: SUser

    protected lateinit var myConnectionManager: OAuthConnectionsManager
    protected lateinit var myConnection: OAuthConnectionDescriptor

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()

        setInternalProperty("teamcity.notifications.quiet.period.seconds", "0")
        myFixture.notificationProcessor.setBuildFailingDelay(700)
        myFixture.notificationProcessor.setCheckHangedBuildsInterval(50)

        myConnectionManager = OAuthConnectionsManager(myFixture.getSingletonService(ExtensionHolder::class.java))

        mySlackApiFactory =
                MockSlackWebApiFactory()
        mySlackApi = mySlackApiFactory.createSlackWebApi()

        val messageFormatter = SlackMessageFormatter()
        val detailsFormatter = DetailsFormatter(
                messageFormatter,
                myFixture.webLinks,
                myFixture.projectManager
        )

        val simpleMessageBuilderFactory = SimpleMessageBuilderFactory(
                messageFormatter,
                myFixture.webLinks,
                detailsFormatter
        )

        myDescriptor = SlackNotifierDescriptor(myFixture.getSingletonService(NotificatorRegistry::class.java))
        myNotifier = SlackNotifier(
                myFixture.notificatorRegistry,
                mySlackApiFactory,
                ChoosingMessageBuilderFactory(
                        simpleMessageBuilderFactory,
                        VerboseMessageBuilderFactory(
                            simpleMessageBuilderFactory,
                            messageFormatter,
                            myFixture.webLinks,
                            myFixture.getSingletonService(NotificationBuildStatusProvider::class.java),
                            myServer
                        )
                ),
                myProjectManager,
                myConnectionManager,
                myDescriptor
        )

        myFixture.addService(myNotifier)

        myConnection = myConnectionManager.addConnection(
            myProject,
            SlackConnection.type,
            mapOf(
                "secure:token" to "test_token",
                "clientId" to "test_clientId",
                "secure:clientSecret" to "test_clientSecret"
            )
        )

        myUser = createUser("test_user")
        myUser.setUserProperty(SlackProperties.channelProperty, "#test_channel")
        myUser.setUserProperty(SlackProperties.connectionProperty, myConnection.id)
        makeProjectAccessible(myUser, myProject.projectId)

        myAssignerUser = createUser("investigation_assigner")
        makeProjectAccessible(myUser, myProject.projectId)
    }

    fun `given user is subscribed to`(vararg events: NotificationRule.Event) {
        storeRules(myUser, myNotifier, newRule(*events))
    }

    fun `given build feature is subscribed to`(vararg events: NotificationRule.Event, additionalParameters: Map<String, String> = emptyMap()) {
        myBuildType.addBuildFeature(
                FeatureProviderNotificationRulesHolder.FEATURE_NAME,
                mapOf(
                        "notifier" to myNotifier.notificatorType,
                        SlackProperties.channelProperty.key to "#test_channel",
                        SlackProperties.connectionProperty.key to myConnection.id,
                        *(events.map { NotificationRulesConstants.getName(it) to "true" }).toTypedArray()
                ) + additionalParameters
        )
    }

    fun `when build starts`(): SBuild = startBuild()
    fun `when build is triggered manually`(): SBuild {
        return build().`in`(myBuildType).by(createUser("user_who_triggered_the_build")).run()
    }

    fun `when build finishes`(): SBuild {
        startBuild()
        return finishBuild()
    }

    fun `when build finishes in master`(): SBuild {
        startBuildInBranch("master")
        return finishBuild()
    }

    fun `when build finishes with custom status`(): SBuild {
        startBuild()
        myFixture.logBuildMessages(
                runningBuild,
                listOf(DefaultMessagesInfo.createTextMessage("##teamcity[buildStatus text='Custom build status']"))
        )
        runningBuild.updateBuild()
        return finishBuild()
    }

    fun `when build finishes with changes`(): SBuild {
        val vcsRoot = myFixture.addVcsRoot("vcs", "")
        startBuildWithChanges(myBuildType, ModificationDataForTest.forTests("Commit message", "committer1", vcsRoot, "1"))
        return finishBuild()
    }

    fun `when build finishes with multiple changes`(n: Int): SBuild {
        val vcsRoot = myFixture.addVcsRoot("vcs", "")

        for (i in 0 until n) {
            publishModifications(
                    myBuildType,
                    ModificationDataForTest.forTests("Commit message $i", "committer1", vcsRoot, "$i")
            )
        }
        startBuild()
        return finishBuild()
    }

    fun `when build fails`(): SBuild {
        return createFailedBuild()
    }

    fun `when build fails in master`(): SBuild {
        return createBuildInBranch("master", Status.FAILURE)
    }

    fun `when build fails with custom status`(): SBuild {
        startBuild()
        myFixture.logBuildMessages(
                runningBuild,
                listOf(DefaultMessagesInfo.createTextMessage("##teamcity[buildStatus text='Custom build status']"))
        )
        runningBuild.updateBuild()
        return finishBuild(true)
    }

    fun `when build fails with changes`(): SBuild {
        val vcsRoot = myFixture.addVcsRoot("vcs", "")
        startBuildWithChanges(myBuildType, ModificationDataForTest.forTests("Commit message", "committer1", vcsRoot, "1"))
        return finishBuild(true)
    }

    fun `when build newly fails`(): SBuild {
        startBuild()
        finishBuild()
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

    fun `when build problem occurs`(): SBuild {
        return build().`in`(myBuildType).withProblem(
            BuildProblemData.createBuildProblem("1", "TC_COMPILATION_ERROR_TYPE", "compilation error")
        ).finish()
    }

    fun `when responsibility changes`(): SUser {
        myBuildType.setResponsible(myUser, "will fix", myAssignerUser)
        return myUser
    }

    fun `then message should contain`(vararg strs: String) {
        waitForAssert(BooleanSupplier {
            mySlackApi.messages.isNotEmpty()
        }, 2000L)

        for (str in strs) {
            assertContains(mySlackApi.messages.last().text, str)
        }
    }

    fun `then message should not contain`(vararg strs: String) {
        waitForAssert(BooleanSupplier {
            mySlackApi.messages.isNotEmpty()
        }, 2000L)

        for (str in strs) {
            assertNotContains(mySlackApi.messages.last().text, str, false)
        }
    }

    fun `then no messages should be sent`() {
        assertEmpty(mySlackApi.messages)
    }


    fun Int.`messages should be sent`() {
        waitForAssert(BooleanSupplier {
            mySlackApi.messages.size == this
        }, 2000L)
    }

    private fun <T> Iterable<T>.last(): T {
        return reversed().first()
    }

    fun invalidProperty(name: String): InvalidProperty = InvalidProperty(name, "")
}