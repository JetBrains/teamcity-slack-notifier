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


<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="connectionsBean"
             type="jetbrains.buildServer.notification.slackNotifier.SlackConnectionsBean" scope="request"/>

<jsp:useBean id="properties" type="jetbrains.buildServer.notification.slackNotifier.SlackProperties" scope="request"/>
<jsp:useBean id="user" type="jetbrains.buildServer.users.SUser" scope="request"/>
<jsp:useBean id="slackUsername" type="java.lang.String" scope="request"/>
<jsp:useBean id="selectedConnection" type="java.lang.String" scope="request"/>

<c:set var="autocompletionUrl" value="/admin/notifications/jbSlackNotifier/autocompleteUserId.html"/>

<tr>
    <td>
        <label class="notifierSettingControls__label">
            Connection to use:<l:star/>
        </label>
    </td>
    <td>
        <c:choose>
            <c:when test="${empty connectionsBean.connections}">
                No suitable Slack connections found.
            </c:when>
            <c:otherwise>
                <props:selectProperty
                        name="${properties.connectionKey}"
                        id="${properties.connectionKey.replace(':', '-')}"
                        className="longField"
                >
                    <props:option value="">-- Choose Slack connection --</props:option>
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
        <span id="signedInUserNote">
        </span>
        <br/>
        <a id="signInWithSlack">
            Sign in with Slack
        </a>
    </td>

</tr>

<script type="text/javascript">
    var connectionId = "#${properties.connectionKey.replace(':', '-')}";
    var signInWithSlack = $j("#signInWithSlack");

    BS.UserSlackNotifierSettings = {
        connections: {},

        updateSignInUrl: function (selectedConnectionId) {
            var connection = this.connections[selectedConnectionId];
            if (!connection) {
                $j("#userSection").hide();
                return;
            } else {
                $j("#userSection").show();
                if (selectedConnectionId === "${selectedConnection}") {
                    $j("#signedInUserNote").text('You are signed in as ${util:forJS(slackUsername, true, false)}.');
                } else {
                    $j("#signedInUserNote").text("You are not signed in.");
                }
            }

            var team = connection.team;
            var state = encodeURIComponent(JSON.stringify({
                userId: "${user.id}",
                connectionId: selectedConnectionId
            }));

            var redirectUrl = encodeURIComponent(window["base_uri"] + "/admin/slack/oauth.html");
            var clientId = connection.clientId;

            signInWithSlack.attr("href",
                "https://slack.com/oauth/authorize?scope=identity.basic,identity.team" +
                "&client_id=" + clientId +
                "&state=" + state +
                "&redirect_uri=" + redirectUrl +
                "&team=" + team
            );
        }
    };

    <c:forEach items="${connectionsBean.connections}" var="connection">
    BS.UserSlackNotifierSettings.connections["${connection.id}"] = {
        clientId: "${util:forJS(connection.parameters["clientId"], true, false)}",
        team: "${connectionsBean.getTeamForConnection(connection)}"
    };
    </c:forEach>

    BS.UserSlackNotifierSettings.updateSignInUrl($j(connectionId + " option:selected").val());
    $j(connectionId).on("change", function () {
        BS.UserSlackNotifierSettings.updateSignInUrl(this.value);
    });
</script>