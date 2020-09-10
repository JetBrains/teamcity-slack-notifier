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
  ~  Copyright 2000-2020 JetBrains s.r.o.
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

<c:set var="autocompletionUrl" value="/admin/notifications/jbSlackNotifier/autocompleteUserId.html"/>

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
                        To receive notifications for a specific project, configure the connection directly in this project's settings instead.
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


    <tr id="userSection" style="vertical-align: top">
        <td>
            <label class="notifierSettingControls__label">
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
    <tr>
        <td colspan="2" style="padding-top: 6px;">
            <div id="connectionWarning" class="attentionComment" style="display: none;"></div>
        </td>
    </tr>

    <span id="singInWithSlackWrapper" style="display:none;">
        <a rel="noreferrer" class="signInWithSlack btn btn_mini submitButton">
            Sign In
        </a>
    </span>

    <script type="text/javascript">
        $j(document).ready(function() {
            var connectionId = "#${properties.connectionKey.replace(':', '-')}";
            var slackUsername = "${util:forJS(slackUsername, true, false)}";

            var signOutButton = $j("#saveNotifierSettings");

            BS.UserSlackNotifierSettings = {
                connections: {},

                updateSignInUrl: function (selectedConnectionId) {
                    var signInButton = $j(".signInWithSlack");

                    var connection = this.connections[selectedConnectionId];
                    if (!connection) {
                        $j("#userSection").hide();
                        signInButton.hide();
                        signOutButton.attr("value", "Save");
                        return;
                    } else {
                        signOutButton.attr("value", "Sign out");

                        $j("#userSection").show();
                        if (selectedConnectionId === "${selectedConnection}" && slackUsername) {
                            $j("#signedInUserNote").text('You are signed in as ' + slackUsername + '.');
                            signInButton.hide()
                            signOutButton.show();
                        } else {
                            $j("#signedInUserNote").text("You are not signed in.");
                            signInButton.show();
                            signOutButton.hide();
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