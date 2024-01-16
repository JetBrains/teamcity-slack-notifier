<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>



<jsp:useBean id="connection" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor" scope="request"/>
<jsp:useBean id="reason" type="java.lang.String" scope="request"/>

<div>
    Slack connection
    <admin:editProjectLink projectId="${connection.project.externalId}" addToUrl="&tab=oauthConnections">
        <bs:out value="${connection.connectionDisplayName}"/>
    </admin:editProjectLink>
    is invalid.
    <br/>
    <bs:out value="${reason}"/>
</div>