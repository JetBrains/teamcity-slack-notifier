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


<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="availableConnections"
             type="java.util.List<jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor>" scope="request"/>
<jsp:useBean id="properties" type="jetbrains.buildServer.notification.slackNotifier.SlackProperties" scope="request"/>
<jsp:useBean id="user" type="jetbrains.buildServer.users.SUser" scope="request"/>
<jsp:useBean id="slackUsername" type="java.lang.String" scope="request"/>

<c:set var="autocompletionUrl" value="/admin/notifications/jbSlackNotifier/autocompleteUserId.html"/>

<tr>
    <td>
        <label class="notifierSettingControls__label">
            Connection to use:<l:star/>
        </label>
    </td>
    <td>
        <c:choose>
            <c:when test="${empty availableConnections}">
                No suitable Slack connections found.
            </c:when>
            <c:otherwise>
                <props:selectProperty
                        name="${properties.connectionKey}"
                        id="${properties.connectionKey.replace(':', '-')}"
                        className="longField"
                >
                    <props:option value="">-- Choose Slack connection --</props:option>
                    <c:forEach var="connection" items="${availableConnections}">
                        <props:option value="${connection.id}">
                            <c:out
                                    value="${connection.connectionDisplayName}"/>
                        </props:option>
                    </c:forEach>
                </props:selectProperty>
            </c:otherwise>
        </c:choose>

        <span class="error" id="error_${properties.connectionKey}"></span>
    </td>
</tr>

<tr>
    <td>
        <label class="notifierSettingControls__label">
            User:
        </label>
    </td>

    <td>
        <c:choose>
            <c:when test="${not empty slackUsername}">
                You are signed in as <bs:out value="${slackUsername}"/>.
                <a id="signInWithSlack">
                    Sign in again
                </a>
            </c:when>
            <c:otherwise>
                <a id="signInWithSlack">
                    Sign in
                </a>
                to receive Slack notifications
            </c:otherwise>
        </c:choose>
    </td>

</tr>

<script type="text/javascript">
    var connectionId = "#${properties.connectionKey.replace(':', '-')}";
    var signInWithSlack = $j("#signInWithSlack");

    BS.UserSlackNotifierSettings = {
        updateSignInUrl: function (selectedConnection) {
            var state = encodeURIComponent(JSON.stringify({
                userId: "${user.id}",
                connectionId: selectedConnection
            }));

            var redirectUrl = window["base_uri"] + "/admin/slack/oauth.html";

            signInWithSlack.attr("href",
                "https://slack.com/oauth/authorize?scope=identity.basic,identity.team&client_id=2280447103.946465054946&state=" + state +
                "&redirect_uri=" + redirectUrl
            );

            if (selectedConnection) {
                signInWithSlack.show();
            } else {
                signInWithSlack.hide();
            }
        }
    };

    BS.UserSlackNotifierSettings.updateSignInUrl($j(connectionId + " option:selected").val());
    $j(connectionId).on("change", function () {
        BS.UserSlackNotifierSettings.updateSignInUrl(this.value);
    });
</script>