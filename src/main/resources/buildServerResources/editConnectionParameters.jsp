<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
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
    <td><label for="secure:token">Slack bot token:</label><l:star/></td>
    <td>
        <props:passwordProperty name="secure:token" className="longField"/>
        <span class="smallNote">
            Provide <a href="https://api.slack.com/docs/token-types#bot" target="_blank">bot token</a> to be used to connect to your Slack workspace.
            <br/>
            You can see your apps or create a new one on <a href="https://api.slack.com/apps"
                                                            target="_blank">Your Apps</a> page.
            <br/>
            This bot will need the following scopes: <i>channels:read, chat:write, im:read, im:write, users:read</i>
        </span>
        <span class="error" id="error_secure:token"></span>
    </td>
</tr>
