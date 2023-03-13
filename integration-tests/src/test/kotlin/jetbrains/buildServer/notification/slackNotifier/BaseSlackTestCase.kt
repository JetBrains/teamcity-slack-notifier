/*
 *  Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.messages.DefaultMessagesInfo
import jetbrains.buildServer.messages.Status
import jetbrains.buildServer.notification.*
import jetbrains.buildServer.notification.slackNotifier.notification.*
import jetbrains.buildServer.notification.slackNotifier.slack.*
import jetbrains.buildServer.serverSide.*
import jetbrains.buildServer.serverSide.impl.NotificationRulesConstants
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager
import jetbrains.buildServer.users.SUser
import jetbrains.buildServer.users.impl.UserEx
import jetbrains.buildServer.vcs.ModificationDataForTest
import org.testng.annotations.BeforeMethod
import java.util.*
import java.util.function.BooleanSupplier

open class BaseSlackTestCase : BaseNotificationRulesTestCase() {
    protected lateinit var mySlackApiFactory: StoringMessagesSlackWebApiFactory
    private lateinit var mySlackApi: StoringMessagesSlackWebApi
    protected lateinit var myDescriptor: SlackNotifierDescriptor
    private lateinit var myNotifier: SlackNotifier
    private lateinit var myUser: SUser
    private lateinit var myAssignerUser: SUser
    private lateinit var myAdHocMessageBuilder: PlainAdHocMessageBuilder
    private lateinit var myNotificationCountHandler: BuildPromotionNotificationCountHandler
    private lateinit var myDomainNameFinder: DomainNameFinder

    protected lateinit var myConnectionManager: OAuthConnectionsManager
    protected lateinit var myConnection: OAuthConnectionDescriptor

    private lateinit var messageFormatter: SlackMessageFormatter
    private lateinit var detailsFormatter: DetailsFormatter
    private lateinit var simpleMessageBuilderFactory: SimpleMessageBuilderFactory

    private lateinit var compositeBuildType: SBuildType

    @BeforeMethod(alwaysRun = true)
    override fun setUp() {
        super.setUp()

        setInternalProperty("teamcity.notifications.quiet.period.seconds", "0")
        myFixture.notificationProcessor.setBuildFailingDelay(700)
        myFixture.notificationProcessor.setCheckHangedBuildsInterval(50)

        myConnectionManager = OAuthConnectionsManager(
            myFixture.getSingletonService(ExtensionHolder::class.java)
        )

        mySlackApiFactory =
            StoringMessagesSlackWebApiFactoryStub()
        mySlackApi = mySlackApiFactory.createSlackWebApi()

        messageFormatter = SlackMessageFormatter()
        detailsFormatter = DetailsFormatter(
            messageFormatter,
            myFixture.webLinks,
            myFixture.projectManager
        )

        myAdHocMessageBuilder = PlainAdHocMessageBuilder(detailsFormatter)

        myNotificationCountHandler = myFixture.getSingletonService(
            BuildPromotionNotificationCountHandler::class.java
        )
        myDomainNameFinder = myFixture.getSingletonService(
            DomainNameFinder::class.java
        )

        simpleMessageBuilderFactory = SimpleMessageBuilderFactory(
                messageFormatter,
                myFixture.webLinks,
                detailsFormatter
        )

        myDescriptor = SlackNotifierDescriptor(myFixture.getSingletonService(NotificatorRegistry::class.java))
        myNotifier = createNotifier()

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

    private fun createNotifier(): SlackNotifier {
        val notifier = SlackNotifier(
            myFixture.notificatorRegistry,
            mySlackApiFactory,
            ChoosingMessageBuilderFactory(
                simpleMessageBuilderFactory,
                VerboseMessageBuilderFactory(
                    detailsFormatter,
                    messageFormatter,
                    myFixture.webLinks,
                    myFixture.getSingletonService(NotificationBuildStatusProvider::class.java),
                    myServer,
                    myFixture.getSingletonService(ChangesCalculationOptionsFactory::class.java)
                )
            ),
            myAdHocMessageBuilder,
            myProjectManager,
            myConnectionManager,
            myDescriptor,
            myNotificationCountHandler,
            myDomainNameFinder
        )

        myFixture.addService(notifier)

        return notifier
    }

    fun `given user is subscribed to`(vararg events: NotificationRule.Event) {
        storeRules(myUser, myNotifier, newRule(*events))
    }

    fun `given build feature is subscribed to`(vararg events: NotificationRule.Event) {
        addBuildFeature(*events)
    }

    fun `given build feature with verbose branch is subscribed to`(vararg events: NotificationRule.Event) {
        addBuildFeature(*events, additionalParameters = mapOf(
            SlackProperties.messageFormatProperty.key to "verbose",
            SlackProperties.addBranchProperty.key to "true"
        ))
    }

    fun `given build feature with verbose build status is subscribed to`(vararg events: NotificationRule.Event) {
        addBuildFeature(*events, additionalParameters = mapOf(
            SlackProperties.messageFormatProperty.key to "verbose",
            SlackProperties.addBuildStatusProperty.key to "true"
        ))
    }

    fun `given build feature with verbose changes is subscribed to`(vararg events: NotificationRule.Event) {
        addBuildFeature(*events, additionalParameters = mapOf(
            SlackProperties.messageFormatProperty.key to "verbose",
            SlackProperties.addChangesProperty.key to "true"
        ))
    }

    fun `given build feature in composite build with verbose changes is subscribed to`(vararg events: NotificationRule.Event) {
        compositeBuildType = registerBuildType("Composite Build", myProject.name)
        compositeBuildType.setOption(BuildTypeOptions.BT_SHOW_DEPS_CHANGES, true)
        addDependency(compositeBuildType, myBuildType)

        addBuildFeature(
            *events,
            additionalParameters = mapOf(
                SlackProperties.messageFormatProperty.key to "verbose",
                SlackProperties.addChangesProperty.key to "true"
            ),
            buildType = compositeBuildType
        )
    }

    fun `given build feature in composite build with verbose changes that doesnt show changes from dependecies is subscribed to`(
        vararg events: NotificationRule.Event
    ) {
        compositeBuildType = registerBuildType("Composite Build", myProject.name)
        addDependency(compositeBuildType, myBuildType)

        addBuildFeature(
            *events,
            additionalParameters = mapOf(
                SlackProperties.messageFormatProperty.key to "verbose",
                SlackProperties.addChangesProperty.key to "true"
            ),
            buildType = compositeBuildType
        )
    }

    fun `given build feature with 2 max changes is subscribed to`(vararg events: NotificationRule.Event) {
        addBuildFeature(*events, additionalParameters = mapOf(
            SlackProperties.messageFormatProperty.key to "verbose",
            SlackProperties.addChangesProperty.key to "true",
            SlackProperties.maximumNumberOfChangesProperty.key to "2"
        ))
    }

    fun `given build feature with verbose without settings is subscribed to`(vararg events: NotificationRule.Event) {
        addBuildFeature(*events, additionalParameters = mapOf(
            SlackProperties.messageFormatProperty.key to "verbose"
        ))
    }

    private fun addBuildFeature(
        vararg events: NotificationRule.Event,
        additionalParameters: Map<String, String> = emptyMap(),
        buildType: SBuildType = myBuildType
    ) {
        buildType.addBuildFeature(
            FeatureProviderNotificationRulesHolder.FEATURE_NAME,
            mapOf(
                "notifier" to myNotifier.notificatorType,
                SlackProperties.channelProperty.key to "#test_channel",
                SlackProperties.connectionProperty.key to myConnection.id,
                *(events.map { NotificationRulesConstants.getName(it) to "true" }).toTypedArray()
            ) + additionalParameters
        )
    }

    fun `given build feature has parameterized receiver`() {
        myBuildType.addParameter(SimpleParameter("slack_channel", "#test_channel"))

        myBuildType.addBuildFeature(
            FeatureProviderNotificationRulesHolder.FEATURE_NAME,
            mapOf(
                "notifier" to myNotifier.notificatorType,
                SlackProperties.channelProperty.key to "%slack_channel%",
                SlackProperties.connectionProperty.key to myConnection.id,
                NotificationRulesConstants.BUILD_FINISHED_SUCCESS to "true"
            )
        )
    }

    fun `given authTest api method is hanging`() {
        initHangingSlackWebApi("authTest")
    }

    fun `given conversationsList api method is hanging`() {
        initHangingSlackWebApi("conversationsList")
    }

    fun `given botsInfo api method is hanging`() {
        initHangingSlackWebApi("botsInfo")
    }

    fun `given conversationsMembers api method is hanging`() {
        initHangingSlackWebApi("conversationsMembers")
    }

    private fun initHangingSlackWebApi(methodThatAreHanging: String) {
        mySlackApiFactory =
            HangingSlackWebApiFactoryStub(methodThatAreHanging, mySlackApi, myFixture.executorServices)
        mySlackApi = mySlackApiFactory.createSlackWebApi()
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

    fun `when composite build finishes with changes from dependent build`(): SBuild {
        val vcsRoot = myFixture.addVcsRoot("vcs", "")
        val modificationData = ModificationDataForTest.forTests("Commit Message", "committer1", vcsRoot, "1", Date());
        myFixture.addModification(modificationData)
        compositeBuildType.addToQueue("")
        flushQueueAndWait()
        finishBuild()
        flushQueueAndWait()
        return finishBuild()
    }

    fun `when build finishes with user changes`(): SBuild {
        (myUser as UserEx).setDefaultVcsUsernames(listOf("committer1"))
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

    fun `when build finishes with a long change description`(): SBuild {
        val vcsRoot = myFixture.addVcsRoot("vcs", "")
        publishModifications(
                myBuildType,
                ModificationDataForTest.forTests("very long message".repeat(100), "committer1", vcsRoot, "1")
        )
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

    fun `then message should contain`(vararg strings: String) {
        waitForMessage()
        for (str in strings) {
            val message = mySlackApi.messages.last()
            if (message.blocks.isNotEmpty()) {
                assertContains(message.blocks.joinToString("\n"), str)
            } else {
                assertContains(message.text, str)
            }
        }
    }

    fun `then message should contain user descriptive name`() {
        waitForMessage()
        val message = mySlackApi.messages.last()
        if (message.blocks.isNotEmpty()) {
            assertContains(message.blocks.joinToString("\n"), myUser.descriptiveName)
        } else {
            assertContains(message.text, myUser.descriptiveName)
        }
    }

    fun `then message should not contain`(vararg strings: String) {
        waitForMessage()
        for (str in strings) {
            assertNotContains(mySlackApi.messages.last().text, str, false)
        }
    }

    fun `then view changes button should contain`(vararg strings: String) {
        waitForMessage()
        for (str in strings) {
            val actionsBlock = mySlackApi.messages.last().blocks.last()
            if (actionsBlock !is MessageActions) {
                throw IllegalStateException("Last block $actionsBlock is not MessageActions")
            }
            assertContains(actionsBlock.elements.first().text.text, str)
        }
    }

    fun `then message should be short`() {
        waitForMessage()
        assertTrue(mySlackApi.messages.last().text!!.length < 1000)
    }

    private fun waitForMessage() {
        waitForAssert(BooleanSupplier {
            mySlackApi.messages.isNotEmpty()
        }, 2000L)
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