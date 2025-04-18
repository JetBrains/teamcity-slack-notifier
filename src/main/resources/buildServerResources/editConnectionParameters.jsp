<%@ page import="jetbrains.buildServer.web.util.WebUtil" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop"%>



<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<c:set var="currentRootUrl" value="${WebUtil.getRootUrl(pageContext.request)}"/>
<jsp:useBean id="rootUrl" type="java.lang.String" scope="request"/>

<c:url var="testConnectionUrl" value="/admin/slack/testConnection.html"/>
<c:set var="testAuthRedirectUrl" value="${currentRootUrl}/admin/slack/auth/test.html"/>
<c:url var="prepareForAuthTest" value="/admin/slack/auth/prepareForTest.html"/>

<c:url var="slackNotifierSettingsUrl" value="/profile.html?notificatorType=jbSlackNotifier&item=userNotifications"/>

<bs:linkScript>
    /js/bs/forms.js
    /js/bs/editBuildType.js
</bs:linkScript>

<script>
    BS.SlackConnectionDialog = OO.extend(BS.PluginPropertiesForm, {
        results: [],

        formElement: function () {
            return $j("#OAuthConnection")[0];
        },

        testConnection: function () {
            this.results = [];

            this.testBotToken();
            this.testClientIdAndSecret();
        },

        testBotToken: function () {
            var that = this;
            var info = "";
            var success = true;

            BS.PasswordFormSaver.save(that, "${testConnectionUrl}",
                OO.extend(BS.ErrorsAwareListener, {
                    onBeginSave: function (form) {
                        form.setSaving(true);
                        form.disable();
                    },

                    onTestConnectionFailedError: function (elem) {
                        if (success) {
                            info = "";
                        } else if ("" !== info) {
                            info += "\n";
                        }
                        info += elem.textContent || elem.text;
                        success = false;
                    },

                    onCompleteSave: function (form, responseXML, err) {
                        BS.XMLResponse.processErrors(responseXML, that, function (id, elem) {
                            success = false;
                            info += elem.textContent || elem.text;
                            info += "\n";
                        });

                        that.newTestConnectionResult(success, info, form)
                    }
                })
            );
        },

        newTestConnectionResult: function(success, info, form) {
            this.results.push({
                success: success,
                message: info
            });

            this.displayDialogIfFinished(form);
        },

        displayDialogIfFinished: function(form) {
            if (form) {
                this.form = form;
            }

            // Two results are expected:
            // 1. check for the bot token correctness
            // 2. check for client id and client secret correctness
            // These checks are executed in parallel using two different flows. The only was to sync them is to
            // store their results and then check if both are finished
            if (this.results.length < 2) {
                return;
            }

            var firstResult = this.results[0];
            var secondResult = this.results[1];

            var success = firstResult.success && secondResult.success;
            var message;
            if (success) {
                message = (firstResult.message + "\n" + secondResult.message).trim();
            } else {
                if (!firstResult.success) {
                    message = firstResult.message
                } else {
                    message = secondResult.message;
                }
            }

            BS.TestConnectionDialog.show(success, message, null);
            this.form.setSaving(false);
            this.form.enable();
        },

        testClientIdAndSecret: function () {
            var clientId = document.getElementById("clientId").value;
            var that = this;
            this.prepareForAuthTest(function (success, data, form) {
                if (!success) {
                    that.newTestConnectionResult(success, data, form);
                    return;
                }

                var teamDomain = data.teamDomain;

                window.open(
                    "https://" + teamDomain + ".slack.com/oauth/authorize?scope=identity.basic,identity.team" +
                    "&client_id=" + clientId +
                    "&redirect_uri=" + "${testAuthRedirectUrl}",
                    "_blank"
                );

                // If clientId is incorrect, Slack will show error page and will not redirect back to TeamCity
                // To handle this case, form is always enabled after redirecting to Slack
                if (form) {
                    form.setSaving(false);
                    form.enable();
                }
            });
        },

        prepareForAuthTest: function (callback) {
            callback = callback || function(){};
            var form;

            BS.PasswordFormSaver.save(this, "${prepareForAuthTest}",
                OO.extend(BS.ErrorsAwareListener, {
                    onBeginSave: function (f) {
                        form = f;
                    },

                    onTestConnectionFailedError: function (responseXML) {
                        var error = responseXML.documentElement.getElementsByTagName("error").item(0);
                        callback(false, error, form);
                    },

                    onSuccessfulSave: function (responseXML) {
                        try {
                            var teamId = responseXML.documentElement.getElementsByTagName("teamId").item(0).innerHTML;
                            var teamDomain = responseXML.documentElement.getElementsByTagName("teamDomain").item(0).innerHTML;
                            callback(true, {
                                teamId: teamId,
                                teamDomain: teamDomain
                            }, form);
                        } catch (e) {
                            callback(false, "", form);
                        }
                    }
                })
            )
        },
    });

    BS.TestSlackAuthentication = {
        result: function (success, message) {
            BS.SlackConnectionDialog.newTestConnectionResult(success, message);
        }
    };
</script>


<tr>
    <td><label for="displayName">Display name:</label><l:star/></td>
    <td>
        <props:textProperty name="displayName" className="longField"/>
        <span class="smallNote">Provide some name to distinguish this connection from others.</span>
        <span class="error" id="error_displayName"></span>
    </td>
</tr>

<tr>
    <td colspan="2">
        <div class="attentionComment">
            TeamCity connection to Slack requires creating a respective
            <a href="https://api.slack.com/apps"
               target="_blank" rel="noreferrer">Slack app</a>
            with the following scopes:
            <i>channels:read, chat:write, im:read, im:write, users:read, team:read, groups:read</i>.
            <br/>
            For proper authentication, add <bs:out value="${rootUrl}"/>,
            <c:if test="${!rootUrl.equals(currentRootUrl)}">
                <bs:out value="${currentRootUrl}"/>
            </c:if>
            and any other server urls
            to the Redirect URLs in <b> OAuth & Permissions | App Management </b>.
            <br/>
            Copy the Client ID and Secret from the app's Basic Information page to the respective fields in the form
            below.
            <br/>
            Specify a
            <a href="https://api.slack.com/docs/token-types#bot" target="_blank" rel="noreferrer"> bot user token </a>
            associated with your Slack app in the <i>Bot token</i> field.
        </div>
    </td>
</tr>

<tr>
    <td><label for="clientId">Client ID:</label><l:star/></td>
    <td>
        <props:textProperty name="clientId" className="longField"/>
        <span class="error" id="error_clientId"></span>
    </td>
</tr>


<tr>
    <td><label for="secure:clientSecret">Client secret:</label><l:star/></td>
    <td>
        <props:passwordProperty name="secure:clientSecret" className="longField"/>
        <span class="error" id="error_secure:clientSecret"></span>
    </td>
</tr>


<tr>
    <td><label for="secure:token">Bot token:</label><l:star/></td>
    <td>
        <props:passwordProperty name="secure:token" className="longField"/>
        <span class="error" id="error_secure:token"></span>
    </td>
</tr>

<c:if test="${intprop:getBooleanOrTrue('teamcity.notifications.adhoc.slack.ui.enabled')}">
    <l:settingsGroup title="Service message notifications">
        <tr>
            <td colspan="2">
                <bs:smallNote>
                    Use these settings to configure notifications sent via service messages.
                    <bs:help file="send-custom-slack-messages"/>
                </bs:smallNote>
            </td>
        </tr>
        <tr>
            <td><label for="serviceMessageMaxNotificationsPerBuild">Notifications limit:</label></td>
            <td>
                <props:textProperty name="serviceMessageMaxNotificationsPerBuild"
                                    value="${empty propertiesBean.properties[\"serviceMessageMaxNotificationsPerBuild\"]
                                            ? propertiesBean.defaultProperties[\"serviceMessageMaxNotificationsPerBuild\"]
                                            : propertiesBean.properties[\"serviceMessageMaxNotificationsPerBuild\"]}"/>
                <bs:smallNote>Limits the number of service message notifications per build run. Set to '0' to disable service message notifications, or '-1' to allow unlimited notifications.</bs:smallNote>
                <span class="error" id="error_serviceMessageMaxNotificationsPerBuild"></span>
            </td>
        </tr>
        <tr>
            <td><label for="serviceMessageAllowedDomainNames">Allowed hostnames:</label></td>
            <td>
                <props:textProperty name="serviceMessageAllowedDomainNames" expandable="true" style="width: 20em"/>
                <bs:smallNote>For security reasons, only links to this TeamCity server are allowed in notifications. Notifications with URLs to external web resources are automatically blocked. This setting allows you to specify comma-separated list of trusted hostnames that can be referenced in notifications. Use the asterisk (*) as a wildcard for any string (for example, *.test.co.uk). You can use a single asterisk (*) to disable these checks if you trust incoming notification messages.</bs:smallNote>
            </td>
        </tr>
    </l:settingsGroup>
</c:if>

<span id="testConnectionButtonWrapper" style="display:none;">
  <forms:submit id="testConnectionButton" type="button" label="Test connection"
                onclick="BS.SlackConnectionDialog.testConnection();"/>
</span>

<bs:dialog dialogId="testConnectionDialog" title="Test Connection" closeCommand="BS.TestConnectionDialog.close();"
           closeAttrs="showdiscardchangesmessage='false'">
    <div id="testConnectionStatus"></div>
    <div id="testConnectionDetails" class="mono"></div>
</bs:dialog>

<script>
    $j(document).ready(function () {
        var additionalButtons = $j("span#editConnectionAdditionalButtons");
        if (additionalButtons.length) {
            additionalButtons.empty();
            additionalButtons.append($j("span#testConnectionButtonWrapper *"));
        }
    });
</script>