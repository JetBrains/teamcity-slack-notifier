<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<jsp:useBean id="editConnectionUrl" scope="request" type="java.lang.String"/>
<jsp:useBean id="user" scope="request" type="jetbrains.buildServer.users.SUser"/>
<jsp:useBean id="rootProject" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>

<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>

<p>
    Specify the builds and events you want to be notified about via Slack messages.
    <br/>
    To receive notifications for all projects, configure a connection to Slack in the
    <c:choose>
        <c:when test='${user.isPermissionGrantedForProject(rootProject.projectId, Permission.EDIT_PROJECT)}'>
            <a href="${editConnectionUrl}">
                Root project's settings
            </a>
        </c:when>
        <c:otherwise>
            Root project's settings
        </c:otherwise>
    </c:choose>

    and select it in the drop-down menu below.
    <br/>
    To receive notifications for a specific project, configure the Slack connection directly in this project's settings
    instead.
</p>
