package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class ChoosingMessageBuilderFactory(
        private val simpleMessageBuilderFactory: SimpleMessageBuilderFactory,
        private val verboseMessageBuilderFactory: VerboseMessageBuilderFactory
) : MessageBuilderFactory {
    override fun get(user: SUser): MessageBuilder {
        val messageFormat = user.getPropertyValue(SlackProperties.messageFormatProperty)
        if (messageFormat == "verbose") {
            return verboseMessageBuilderFactory.get(user)
        }

        return simpleMessageBuilderFactory.get(user)
    }
}