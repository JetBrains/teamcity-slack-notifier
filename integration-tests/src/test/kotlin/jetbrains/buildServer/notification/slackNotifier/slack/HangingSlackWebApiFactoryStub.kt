

package jetbrains.buildServer.notification.slackNotifier.slack

import jetbrains.buildServer.serverSide.executors.ExecutorServices

class HangingSlackWebApiFactoryStub(methodsThatAreHanging: Set<String>, slackWebApi: SlackWebApi, executorServices: ExecutorServices) : StoringMessagesSlackWebApiFactory {
    private val slackWebApi = StoringMessagesSlackWebApiImpl(CachingSlackWebApi(HangingSlackWebApi(slackWebApi, methodsThatAreHanging), executorServices))
    constructor(methodThatIsHanging: String, slackWebApi: SlackWebApi, executorServices: ExecutorServices): this(setOf(methodThatIsHanging), slackWebApi, executorServices)
    override fun createSlackWebApi(): StoringMessagesSlackWebApi {
        return slackWebApi
    }
}