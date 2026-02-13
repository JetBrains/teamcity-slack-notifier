<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="reason" type="java.lang.String" scope="request"/>
<jsp:useBean id="receiver" type="java.lang.String" scope="request"/>
<jsp:useBean id="connectionId" type="java.lang.String" scope="request"/>
<jsp:useBean id="errorCode" type="java.lang.String" scope="request"/>
<jsp:useBean id="isThrottlingFailure" type="java.lang.Boolean" scope="request"/>
<jsp:useBean id="occurrences" type="java.lang.Integer" scope="request"/>

<div>
    Slack notification delivery failed
    <c:if test="${not empty buildTypeName and not empty editUrl}">
        in
        <a href="${editUrl}">
            <bs:out value="${buildTypeName}"/>
        </a>
    </c:if>.
    <br/>
    <bs:out value="${reason}"/>
    <c:if test="${not empty receiver}">
        <br/>
        Receiver: <code><bs:out value="${receiver}"/></code>
    </c:if>
    <c:if test="${not empty connectionId}">
        <br/>
        Connection id: <code><bs:out value="${connectionId}"/></code>
    </c:if>
    <c:if test="${not empty errorCode}">
        <br/>
        Error code: <code><bs:out value="${errorCode}"/></code>
    </c:if>
    <c:if test="${isThrottlingFailure}">
        <br/>
        Slack API is rate limiting requests. Reduce notification volume for this project or build configuration.
    </c:if>
    <c:if test="${occurrences > 1}">
        <br/>
        Occurrences: <bs:out value="${occurrences}"/>
    </c:if>
</div>
