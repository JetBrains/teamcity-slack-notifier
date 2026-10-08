

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.notification.slackNotifier.SlackProperties
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.users.SUser
import org.springframework.stereotype.Service

@Service
class ChoosingMessageBuilderFactory(
        private val simpleMessageBuilderFactory: SimpleMessageBuilderFactory,
        private val verboseMessageBuilderFactory: VerboseMessageBuilderFactory,
        private val customMessageBuilderFactory: CustomMessageBuilderFactory
) : MessageBuilderFactory {
    override fun get(user: SUser, project: SProject): MessageBuilder {
        when (user.getPropertyValue(SlackProperties.messageFormatProperty)) {
            SlackProperties.verboseMessageFormat -> return verboseMessageBuilderFactory.get(user, project)
            SlackProperties.customMessageFormat -> return customMessageBuilderFactory.get(user, project)
        }

        return simpleMessageBuilderFactory.get(user, project)
    }
}