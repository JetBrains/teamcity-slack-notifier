<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
