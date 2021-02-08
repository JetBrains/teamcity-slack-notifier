<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

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

<jsp:useBean id="editConnectionUrl" scope="request" type="java.lang.String"/>
<jsp:useBean id="user" scope="request" type="jetbrains.buildServer.users.SUser"/>
<jsp:useBean id="rootProject" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>

<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>

<p>
    Specify the builds and events you want to be notified about via Slack messages.
    <br/>

    Slack Notifier requires configuring a Slack connection.
    To receive notifications for all projects, configure a connection to Slack in the
    <c:choose>
        <c:when test='${user.isPermissionGrantedForProject(rootProject.projectId, Permission.EDIT_PROJECT)}'>
            <a href="${editConnectionUrl}">Root project's settings</a>.
        </c:when>
        <c:otherwise>
            Root project's settings.
        </c:otherwise>
    </c:choose>
    <br/>
    To receive notifications for a specific project, configure the connection directly in this project's settings instead.
    <br/>
    Consider using <em>Notifications</em> build feature to receive channel-level notifications. <bs:help file="Notifications"/>
</p>
