

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class ChoosingMessageBuilderFactory(
        private val simpleMessageBuilderFactory: SimpleMessageBuilderFactory,
        private val verboseMessageBuilderFactory: VerboseMessageBuilderFactory
) : MessageBuilderFactory {
    override fun get(user: SUser, project: SProject): MessageBuilder {
        val messageFormat = user.getPropertyValue(SlackProperties.messageFormatProperty)
        if (messageFormat == "verbose") {
            return verboseMessageBuilderFactory.get(user, project)
        }

        return simpleMessageBuilderFactory.get(user, project)
    }
}