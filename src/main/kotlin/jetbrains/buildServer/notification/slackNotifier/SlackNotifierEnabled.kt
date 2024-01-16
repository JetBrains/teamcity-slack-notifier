

package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.serverSide.TeamCityProperties
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class SlackNotifierEnabled : Condition {
    override fun matches(context: ConditionContext?, metadata: AnnotatedTypeMetadata?): Boolean {
        return TeamCityProperties.getBooleanOrTrue(SlackNotifierProperties.enable)
    }
}