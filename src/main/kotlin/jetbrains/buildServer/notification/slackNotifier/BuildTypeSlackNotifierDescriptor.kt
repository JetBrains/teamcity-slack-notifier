package jetbrains.buildServer.notification.slackNotifier

import jetbrains.buildServer.notification.BuildTypeNotifierDescriptor
import jetbrains.buildServer.parameters.ParametersUtil
import jetbrains.buildServer.serverSide.ControlDescription
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.parameters.WellknownParameterArguments
import jetbrains.buildServer.serverSide.parameters.types.TextParameter
import jetbrains.buildServer.web.openapi.PluginDescriptor
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(SlackNotifierEnabled::class)
class BuildTypeSlackNotifierDescriptor(
    private val descriptor: SlackNotifierDescriptor,
    private val pluginDescriptor: PluginDescriptor,
    private val connectionSelectOptionsProvider: SlackConnectionSelectOptionsProvider
) : BuildTypeNotifierDescriptor {
    override fun validate(properties: MutableMap<String, String>): MutableCollection<InvalidProperty> =
        descriptor.validate(properties)

    override fun describeParameters(parameters: MutableMap<String, String>): String {
        val to = parameters[SlackProperties.channelProperty.key]
        if (to != null) {
            return "To: $to"
        }

        return ""
    }

    override fun getParameters(): Map<String, ControlDescription> {
        return mapOf(
            SlackProperties.connectionProperty.key to ParametersUtil.createControlDescription(
                "selection",
                mapOf(
                    WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to "Connection",
                    WellknownParameterArguments.REQUIRED.name to "true",
                    "selectOptionsProviderId" to connectionSelectOptionsProvider.id
                )
            ),
            SlackProperties.channelProperty.key to ParametersUtil.createControlDescription(
                TextParameter.KEY,
                mapOf(
                    WellknownParameterArguments.ARGUMENT_DESCRIPTION.name to "#channel or user id",
                    WellknownParameterArguments.REQUIRED.name to "true",
                    "hintUrl" to SlackNotifierChannelCompletionController.url
                )
            )
        )
    }

    override fun getType(): String = descriptor.getType()

    override fun getDisplayName(): String = descriptor.getDisplayName()

    override fun getEditParametersUrl(): String =
        pluginDescriptor.getPluginResourcesPath("editBuildTypeSlackNotifierSettings.html")
}