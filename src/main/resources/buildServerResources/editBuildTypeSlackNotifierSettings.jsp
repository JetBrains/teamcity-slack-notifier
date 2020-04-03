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
<jsp:useBean id="descriptor" type="jetbrains.buildServer.notification.slackNotifier.SlackNotifierDescriptor"
             scope="request"/>
<jsp:useBean id="buildTypeId" type="java.lang.String" scope="request"/>
<jsp:useBean id="createConnectionUrl" type="java.lang.String" scope="request"/>

<c:set var="autocompletionUrl" value="/admin/notifications/jbSlackNotifier/autocompleteUserId.html"/>

<script type="text/javascript">
    BS.SlackNotifierSettings = {
        createFindUserFunction: function () {
            return function (request, response) {
                var term = request.term;
                var connectionId = $j("#${descriptor.connectionProperty.key.replace(':', '-')} option:selected").val();

                if (!connectionId) {
                    response([]);
                    return;
                }

                var url = '${autocompletionUrl}' + '?term=' + encodeURIComponent(term) +
                    '&' + encodeURIComponent('plugin:notificator:jbSlackNotifier:connection') +
                    '=' + encodeURIComponent(connectionId);

                $j.getJSON(window["base_uri"] + url, function (data) {
                    response(data);
                    $j("#channel-autocomplete-loader").hide();
                });
            };
        },

        createSearchUserFunction: function () {
            return function () {
                $j("#channel-autocomplete-loader").show();
            };
        }
    };
</script>

<tr>
    <th>
        Connection to use:<l:star/>
    </th>
    <td>
        <c:choose>
            <c:when test="${empty availableConnections}">
                No suitable Slack connections found. You can create one at the <a href="${createConnectionUrl}">Connections
                tab</a>
            </c:when>
            <c:otherwise>
                <props:selectProperty
                        name="${descriptor.connectionProperty.key}"
                        id="${descriptor.connectionProperty.key.replace(':', '-')}"
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

        <span class="error" id="error_${descriptor.connectionProperty.key}"></span>
    </td>
</tr>

<tr>
    <th>
        #channel or user id:<l:star/>
    </th>
    <td>
        <props:textProperty
                name="${descriptor.channelProperty.key}"
                id="${descriptor.channelProperty.key}"
                value="${propertiesBean.properties[descriptor.channelProperty.key]}"
                className="longField"
        />
        <forms:saving id="channel-autocomplete-loader" style="display: none;" savingTitle="Fetching autocomplete data"/>
        <span class="error" id="error_${descriptor.channelProperty.key}"></span>
    </td>
</tr>

<script>
    $j(document.getElementById("${descriptor.channelProperty.key}")).autocomplete({
        source: BS.SlackNotifierSettings.createFindUserFunction(),
        search: BS.SlackNotifierSettings.createSearchUserFunction()
    });

    $j(document.getElementById("${descriptor.channelProperty.key}")).placeholder();
</script>