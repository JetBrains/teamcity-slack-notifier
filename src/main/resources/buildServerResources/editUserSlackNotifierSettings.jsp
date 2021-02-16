<%@ taglib prefix="ext" tagdir="/WEB-INF/tags/ext" %>
<%@ taglib prefix="et" tagdir="/WEB-INF/tags/eventTracker" %>
<%@ taglib prefix="queue" tagdir="/WEB-INF/tags/queue" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/p" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags/tags" %>
<%@ taglib prefix="n" tagdir="/WEB-INF/tags/notifications" %>
<%@ taglib prefix="profile" tagdir="/WEB-INF/tags/userProfile" %>
<%@ taglib prefix="ufn" uri="/WEB-INF/functions/user" %>
<%@ taglib prefix="changefn" uri="/WEB-INF/functions/change" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="problems" tagdir="/WEB-INF/tags/problems" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>

<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>

<%--
  ~  Copyright 2000-2021 JetBrains s.r.o.
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License");
  ~  you may not use this file except in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~  https://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing, software
  ~  distributed under the License is distributed on an "AS IS" BASIS,
  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~  See the License for the specific language governing permissions and
  ~  limitations under the License.
  --%>

<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="connectionsBean"
             type="jetbrains.buildServer.notification.slackNotifier.SlackConnectionsBean" scope="request"/>

<jsp:useBean id="properties" type="jetbrains.buildServer.notification.slackNotifier.SlackProperties" scope="request"/>
<jsp:useBean id="user" type="jetbrains.buildServer.users.SUser" scope="request"/>
<jsp:useBean id="slackUsername" type="java.lang.String" scope="request"/>
<jsp:useBean id="selectedConnection" type="java.lang.String" scope="request"/>
<jsp:useBean id="displaySettings" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="rootUrl" type="java.lang.String" scope="request"/>
<jsp:useBean id="editConnectionUrl" scope="request" type="java.lang.String"/>
<jsp:useBean id="rootProject" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>
<jsp:useBean id="editNotificationSettingsUrl" scope="request" type="java.lang.String"/>

<c:choose>
    <c:when test="${displaySettings}">
        <tr>
            <td style="vertical-align: top">
                <label class="notifierSettingControls__label">
                    Connection:<l:star/>
                </label>
            </td>
            <td>
                <c:choose>
                    <c:when test="${empty connectionsBean.connections}">
                        No suitable Slack connections were found.
                        <br/>
                        To receive notifications for all projects, configure Slack connection in the
                        <c:choose>
                            <c:when test='${user.isPermissionGrantedForProject(rootProject.projectId, Permission.EDIT_PROJECT)}'>
                                <a href="${editConnectionUrl}">Root project's settings</a>.
                            </c:when>
                            <c:otherwise>
                                Root project's settings.
                            </c:otherwise>
                        </c:choose>
                        To receive notifications for a specific project, configure the connection directly in that project's settings instead.
                        <br/>
                    </c:when>
                    <c:otherwise>
                        <props:selectProperty
                                name="${properties.connectionKey}"
                                id="${properties.connectionKey.replace(':', '-')}"
                                className="longField"
                        >
                            <props:option value="">-- Select Slack connection --</props:option>
                            <c:forEach var="connection" items="${connectionsBean.connections}">
                                <props:option value="${connection.id}">
                                    <c:out value="${connection.connectionDisplayName}"/>
                                </props:option>
                            </c:forEach>
                        </props:selectProperty>
                    </c:otherwise>
                </c:choose>

                <span class="error" id="error_${properties.connectionKey}"></span>
            </td>
        </tr>


        <tr class="userSection" style="vertical-align: top">
            <td>
                <label class="notifierSettingControls__label" for="${properties.channelKey}">
                    User:
                </label>
            </td>

            <td>
                <span id="signedInUserNote"></span>
            </td>

            <span style="display: none">
                <props:textProperty name="${properties.channelKey}" style="display: none; "/>
            </span>
        </tr>

        <tr class="userSection notificationSection">
            <td>
                <label class="notifierSettingControls__label" for="${properties.messageFormatKey}">
                    Message format:
                </label>
            </td>
            <td>
                <props:selectProperty name="${properties.messageFormatKey}"
                                      onchange="BS.UserSlackNotifierSettings.onMessageFormatChange()">
                    <props:option value="simple">Simple</props:option>
                    <props:option value="verbose">Verbose</props:option>
                </props:selectProperty>
            </td>
        </tr>

        <tr class="messageFormatOption verboseFormatOption userSection notificationSection">
            <td>
            </td>
            <td>
                <props:checkboxProperty name="${properties.addBuildStatusKey}"/> <label
                    for="${properties.addBuildStatusKey}">Add status text</label>
                <br/>
                <props:checkboxProperty name="${properties.addBranchKey}"/> <label for="${properties.addBranchKey}">Add
                branch name</label>
                <br/>
                <props:checkboxProperty name="${properties.addChangesKey}"
                                        onclick="BS.UserSlackNotifierSettings.onAddChanges();"/> <label
                    for="${properties.addChangesKey}">Add changes</label>
                <br/>
                <label for="${properties.maximumNumberOfChangesKey}">Maximum number of changes:</label>
                <br/>
                <props:textProperty name="${properties.maximumNumberOfChangesKey}" maxlength="4"/>
                <span class="error" id="error_${properties.maximumNumberOfChangesKey}"></span>
            </td>
        </tr>

        <tr>
            <td colspan="2" style="padding-top: 6px;">
                <div id="connectionWarning" class="attentionComment" style="display: none;"></div>
            </td>
        </tr>

        <a rel="noreferrer"
           class="saveSlackSettings btn btn_mini submitButton"
           style="display: none; margin-right: 5px;"
           onclick="BS.UserSlackNotifierSettings.saveNotificationSettings(); return false"
        >
            Save
        </a>

        <span id="singInWithSlackWrapper" style="display:none;">
            <a rel="noreferrer" class="signInWithSlack btn btn_mini submitButton">
                Sign In
            </a>
        </span>

        <script type="text/javascript">
            $j(document).ready(function () {
                var connectionId = "#${properties.connectionKey.replace(':', '-')}";
                var slackUsername = "${util:forJS(slackUsername, true, false)}";

                var signOutButton = $j("#saveNotifierSettings");

                BS.UserSlackNotifierSettings = {
                    connections: {},

                    updateSignInUrl: function (selectedConnectionId) {

                        var signInButton = $j(".signInWithSlack");
                        var saveButton = $j(".saveSlackSettings");

                        var connection = this.connections[selectedConnectionId];
                        if (!connection) {
                            $j(".userSection").hide();
                            signInButton.hide();
                            saveButton.hide();
                            signOutButton.attr("value", "Save");
                            return;
                        } else {
                            signOutButton.attr("value", "Sign Out");

                            $j(".userSection").show();
                            if (selectedConnectionId === "${selectedConnection}" && slackUsername) {
                                $j("#signedInUserNote").text('You are signed in as ' + slackUsername + '.');
                                signInButton.hide()
                                signOutButton.show();
                                saveButton.show();
                                saveButton.insertBefore(signOutButton);
                                $j(".notificationSection").show();

                                BS.UserSlackNotifierSettings.onMessageFormatChange();
                                BS.UserSlackNotifierSettings.onAddChanges();
                            } else {
                                $j("#signedInUserNote").text("You are not signed in.");
                                signInButton.show();
                                signOutButton.hide();
                                saveButton.hide();
                                $j(".notificationSection").hide();
                            }
                        }

                        var state = encodeURIComponent(JSON.stringify({
                            userId: "${user.id}",
                            connectionId: selectedConnectionId
                        }));

                        var redirectUrl = encodeURIComponent("${rootUrl}/slack/oauth.html");
                        var clientId = connection.clientId;
                        var teamDomain = connection.teamDomain;

                        signInButton.attr("href",
                            "https://" + teamDomain + ".slack.com/oauth/authorize?scope=identity.basic,identity.team" +
                            "&client_id=" + clientId +
                            "&state=" + state +
                            "&redirect_uri=" + redirectUrl
                        );

                        var projectId = connection.projectId;
                        if (projectId && projectId !== "_Root") {
                            $j("#connectionWarning").text("The selected connection is configured for the " + connection.projectName +
                                " project. You will receive notifications about builds and events in this project and its subprojects.");
                            $j("#connectionWarning").show();
                        } else {
                            $j("#connectionWarning").hide();
                        }
                    },

                    onMessageFormatChange: function () {
                        var select = document.getElementById("${properties.messageFormatKey}")
                        var selectedFormat = select.options[select.selectedIndex].value;

                        $j(".messageFormatOption").hide();
                        $j("." + selectedFormat + "FormatOption").show();

                        if (selectedFormat === "verbose") {
                            this.showVerboseOptions();
                        } else {
                            this.hideVerboseOptions();
                        }
                    },

                    showVerboseOptions: function () {
                        var maximumNumberOfChanges = document.getElementById("${properties.maximumNumberOfChangesKey}");
                        if (!maximumNumberOfChanges.value) {
                            maximumNumberOfChanges.value = "${propertiesBean.defaultProperties[properties.maximumNumberOfChangesKey]}";
                        }
                        this.onAddChanges();
                    },

                    hideVerboseOptions: function () {
                        document.getElementById("${properties.addChangesKey}").checked = false;
                        document.getElementById("${properties.addBranchKey}").checked = false;
                        document.getElementById("${properties.addBuildStatusKey}").checked = false;
                        document.getElementById("${properties.maximumNumberOfChangesKey}").value = "";
                        this.onAddChanges();
                    },

                    onAddChanges: function () {
                        var addChanges = document.getElementById("${properties.addChangesKey}");
                        var maximumNumberOfChanges = document.getElementById("${properties.maximumNumberOfChangesKey}");
                        maximumNumberOfChanges.disabled = !addChanges.checked;
                    },

                    saveNotificationSettings: function () {
                        var settings = [
                            "${properties.messageFormatKey}",
                            "${properties.addBranchKey}",
                            "${properties.addBuildStatusKey}",
                            "${properties.addChangesKey}",
                            "${properties.maximumNumberOfChangesKey}"
                        ];

                        var parameters = settings.map(function (setting) {
                            var element = document.getElementById(setting);
                            if (element.value === "true") {
                                return setting + "=" + element.checked;
                            } else {
                                return setting + "=" + element.value;
                            }
                        }).concat(["holderId=${user.id}"]).join("&");

                        BS.ajaxRequest(window['base_uri'] + "${editNotificationSettingsUrl}", {
                            method: "post",
                            parameters: parameters,
                            onComplete: function () {
                                BS.reload();
                            }
                        });
                    }
                };

                <c:forEach items="${connectionsBean.connections}" var="connection">
                BS.UserSlackNotifierSettings.connections["${connection.id}"] = {
                    clientId: "${util:forJS(connection.parameters["clientId"], true, false)}",
                    team: "${connectionsBean.getTeamForConnection(connection)}",
                    teamDomain: "${connectionsBean.getTeamDomainForConnection(connection)}",
                    projectId: "${connection.project.externalId}",
                    projectName: "${connection.project.fullName}"
                };
                </c:forEach>

                BS.UserSlackNotifierSettings.onMessageFormatChange();
                BS.UserSlackNotifierSettings.onAddChanges();
                BS.UserSlackNotifierSettings.updateSignInUrl($j(connectionId + " option:selected").val());

                $j(connectionId).on("change", function () {
                    BS.UserSlackNotifierSettings.updateSignInUrl(this.value);
                });

                var additionalButtons = $j("span#additionalNotifierButtonsBefore");
                if (additionalButtons.length) {
                    additionalButtons.empty();
                    additionalButtons.append($j("span#singInWithSlackWrapper *"));
                }
            });

        </script>
    </c:when>
    <c:otherwise>
        <script>
            $j(".notifierSettings").hide()
        </script>
    </c:otherwise>
</c:choose>