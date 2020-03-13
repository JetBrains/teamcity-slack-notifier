<%@ taglib prefix="ext" tagdir="/WEB-INF/tags/ext"
%>
<%@ taglib prefix="et" tagdir="/WEB-INF/tags/eventTracker"
%>
<%@ taglib prefix="queue" tagdir="/WEB-INF/tags/queue"
%>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin"
%>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/p"
%>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags/tags"
%>
<%@ taglib prefix="n" tagdir="/WEB-INF/tags/notifications"
%>
<%@ taglib prefix="profile" tagdir="/WEB-INF/tags/userProfile"
%>
<%@ taglib prefix="ufn" uri="/WEB-INF/functions/user"
%>
<%@ taglib prefix="changefn" uri="/WEB-INF/functions/change"
%>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop"
%>
<%@ taglib prefix="problems" tagdir="/WEB-INF/tags/problems"
%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props"
%>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="availableConnections"
             type="java.util.List<jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor>" scope="request"/>
<jsp:useBean id="descriptor" type="jetbrains.buildServer.slackNotifications.SlackNotifierDescriptor" scope="request"/>

<tr>
    <th>
        #channel or user id:
    </th>
    <td>
        <props:textProperty name="${descriptor.channelProperty.key}" className="longField"/>
    </td>
</tr>

<tr>
    <th>
        Connection to use:
    </th>
    <td>
        <props:selectProperty name="${descriptor.connectionProperty.key}" className="longField">
            <props:option value="">-- Choose Slack connection --</props:option>
            <c:forEach var="connection" items="${availableConnections}">
                <props:option value="${connection.parameters['externalId']}"><c:out
                        value="${connection.connectionDisplayName}"/></props:option>
            </c:forEach>
        </props:selectProperty>
    </td>
</tr>