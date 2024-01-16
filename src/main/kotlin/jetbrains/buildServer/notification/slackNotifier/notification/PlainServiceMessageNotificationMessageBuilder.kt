

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.serverSide.SRunningBuild
import org.springframework.stereotype.Service

@Service
class PlainServiceMessageNotificationMessageBuilder(
    private val detailsFormatter: DetailsFormatter
): ServiceMessageNotificationMessageBuilder {
    override fun buildRelatedNotification(
        build: SRunningBuild,
        message: String
    ): MessagePayload {
        val payloadBuilder = MessagePayloadBuilder()

        payloadBuilder.contextBlock {
            add(
                "Sent by ${detailsFormatter.buildUrl(build)}"
            )
        }
        payloadBuilder.textBlock { add(message) }

        return payloadBuilder.build()
    }
}