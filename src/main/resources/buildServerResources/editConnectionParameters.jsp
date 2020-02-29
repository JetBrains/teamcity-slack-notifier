<%@ taglib prefix="props" uri="http://www.springframework.org/tags/form" %>
<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>

<tr>
    <td><label for="displayName">Display name:</label><l:star/></td>
    <td>
        <props:textProperty name="displayName" className="longField"/>
        <span class="smallNote">Provide some name to distinguish this connection from others.</span>
        <span class="error" id="error_displayName"></span>
    </td>
</tr>

<tr>
    <td><label for="secure:token">Slack OAuth token:</label><l:star/></td>
    <td>
        <props:passwordProperty name="secure:token" className="longField"/>
        <span class="error" id="error_secure:token"></span>
    </td>
</tr>
