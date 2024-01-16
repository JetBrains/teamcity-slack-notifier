<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>



<jsp:useBean id="result" type="jetbrains.buildServer.notification.slackNotifier.auth.TestAuthResult" scope="request"/>

<script type="text/javascript">
    window.opener.BS.TestSlackAuthentication.result(${result.success}, "${result.message}");
    window.close();
</script>