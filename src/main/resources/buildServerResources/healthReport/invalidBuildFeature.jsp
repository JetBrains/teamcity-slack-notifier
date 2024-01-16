<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>



<jsp:useBean id="feature" type="jetbrains.buildServer.serverSide.SBuildFeatureDescriptor" scope="request"/>
<jsp:useBean id="editUrl" type="java.lang.String" scope="request"/>
<jsp:useBean id="reason" type="java.lang.String" scope="request"/>
<jsp:useBean id="buildTypeName" type="java.lang.String" scope="request"/>

<div>
    Slack notifications build feature in
    <a href="${editUrl}">
        <bs:out value="${buildTypeName}"/>
    </a>
    is invalid.
    <br/>
    <bs:out value="${reason}"/>
</div>