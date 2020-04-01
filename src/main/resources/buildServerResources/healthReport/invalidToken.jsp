<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="category" type="java.lang.String" scope="request"/>
<jsp:useBean id="connection" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor" scope="request"/>

<div>
    <c:choose>
        <c:when test="${category == 'slackConnectionInvalidToken'}">
            <jsp:useBean id="error" type="java.lang.String" scope="request"/>
            <c:choose>
                <c:when test="${error == 'not_authed' or error == 'invalid_auth'}">
                    Authorization failed for Slack token in
                    <admin:editProjectLink projectId="${connection.project.projectId}" addToUrl="&tab=oauthConnections">
                        <bs:out value="${connection.connectionDisplayName}"/>
                    </admin:editProjectLink>
                    connection.
                    <br/>
                    Provided token is either invalid or expired.
                </c:when>
                <c:otherwise>
                    Slack auth token is invalid in
                    <admin:editProjectLink projectId="${connection.project.projectId}" addToUrl="&tab=oauthConnections">
                        <bs:out value="${connection.connectionDisplayName}"/>
                    </admin:editProjectLink>
                    connection.
                    <br/>
                    Unknown error: <bs:out value="${error}"/>
                </c:otherwise>
            </c:choose>
        </c:when>
        <c:when test="${category == 'slackConnectionMissingToken'}">
            <jsp:useBean id="tokenProperty" type="java.lang.String" scope="request"/>
            Slack connection
            <admin:editProjectLink projectId="${connection.project.projectId}" addToUrl="&tab=oauthConnections">
                <bs:out value="${connection.connectionDisplayName}"/>
            </admin:editProjectLink>
            is missing 'Slack bot token' (<bs:out value="${tokenProperty}"/>) property.
        </c:when>
    </c:choose>

</div>
