<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>



<jsp:useBean id="reason" type="java.lang.String" scope="request"/>

<div>
    <c:choose>
        <c:when test="${not empty buildTypeName and not empty editUrl}">
            Slack notifications build feature in
            <a href="${editUrl}">
                <bs:out value="${buildTypeName}"/>
            </a>
            is invalid.
        </c:when>
        <c:otherwise>
            Slack notifications build feature health check is incomplete.
            <c:if test="${not empty failedFeaturesCount}">
                Failed checks: <bs:out value="${failedFeaturesCount}"/>.
            </c:if>
        </c:otherwise>
    </c:choose>
    <br/>
    <bs:out value="${reason}"/>
</div>
