<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="rootUrl" type="java.lang.String" scope="request"/>

<tr>
    <td><label for="displayName">Display name:</label><l:star/></td>
    <td>
        <props:textProperty name="displayName" className="longField"/>
        <span class="smallNote">Provide some name to distinguish this connection from others.</span>
        <span class="error" id="error_displayName"></span>
    </td>
</tr>

<tr>
    <td><label for="secure:token">Client ID:</label><l:star/></td>
    <td>
        <props:textProperty name="clientId" className="longField"/>
        <span class="smallNote">
            You can see your apps or create a new one on <a href="https://api.slack.com/apps"
                                                            target="_blank">Your Apps</a> page.
            <br/>
            This application will need the following scopes: <i>channels:read, chat:write, im:read, im:write, users:read</i>
        </span>
        <span class="error" id="error_clientId"></span>
    </td>
</tr>


<tr>
    <td><label for="secure:clientSecret">Client secret:</label><l:star/></td>
    <td>
        <props:passwordProperty name="secure:clientSecret" className="longField"/>
        <span class="error" id="error_secure:clientSecret"></span>
    </td>
</tr>


<tr>
    <td><label for="secure:token">Bot token:</label><l:star/></td>
    <td>
        <props:passwordProperty name="secure:token" className="longField"/>
        <span class="smallNote">
            Provide <a href="https://api.slack.com/docs/token-types#bot" target="_blank">bot token</a> to be used to connect to your Slack workspace.

        </span>
        <span class="error" id="error_secure:token"></span>
    </td>
</tr>

<tr class="noBorder">
    <td colspan="2">
        Make sure to add <bs:out value="${rootUrl}"/> to OAuth & Permissions/Redirect URLs, otherwise OAuth sign in
        won't work in user settings.
    </td>
</tr>

