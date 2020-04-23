<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="rootUrl" type="java.lang.String" scope="request"/>

<c:set var="testConnectionUrl" value="/admin/slack/testConnection.html"/>

<bs:linkScript>
    /js/bs/forms.js
    /js/bs/editBuildType.js
</bs:linkScript>

<script>
  BS.SlackConnectionDialog = OO.extend(BS.PluginPropertiesForm, {
    formElement: function () {
      return $j("#OAuthConnection")[0];
    },

    testConnection: function () {
      var that = this;
      var info = "";
      var success = true;

      BS.PasswordFormSaver.save(that, window['base_uri'] + "${testConnectionUrl}",
        OO.extend(BS.ErrorsAwareListener, {
          onBeginSave: function (form) {
            form.setSaving(true);
            form.disable();
          },

          onTestConnectionFailedError: function (elem) {
            if (success) {
              info = "";
            } else if ("" !== info) {
              info += "\n";
            }
            info += elem.textContent || elem.text;
            success = false;
          },

          onCompleteSave: function (form, responseXML, err) {
            BS.XMLResponse.processErrors(responseXML, that, function (id, elem) {
              success = false;
              info = elem.textContent || elem.text;
            });
            BS.TestConnectionDialog.show(success, success ? "" : info, null);
            form.setSaving(false);
            form.enable();
          }
        }));
    }
  });
</script>


<tr>
    <td><label for="displayName">Display name:</label><l:star/></td>
    <td>
        <props:textProperty name="displayName" className="longField"/>
        <span class="smallNote">Provide some name to distinguish this connection from others.</span>
        <span class="error" id="error_displayName"></span>
    </td>
</tr>

<tr>
    <td colspan="2">
        <div class="attentionComment">
            TeamCity connection to Slack requires creating a respective
            <a href="https://api.slack.com/apps"
               target="_blank">Slack app</a>
            with the following scopes:
            <i>channels:read, chat:write, im:read, im:write, users:read</i>.
            <br/>
            For proper authentication, set the Redirect URL in <b> OAuth & Permissions | App Management </b> to <bs:out
                value="${rootUrl}"/>
            <br/>
            Copy the Client ID and Secret from the app's Basic Information page to the respective fields in the form
            below.
            <br/>
            Specify a
            <a href="https://api.slack.com/docs/token-types#bot" target="_blank"> bot user token </a>
            associated with your Slack app in the <i>Bot token</i> field.
        </div>
    </td>
</tr>

<tr>
    <td><label for="secure:token">Client ID:</label><l:star/></td>
    <td>
        <props:textProperty name="clientId" className="longField"/>
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
        <span class="error" id="error_secure:token"></span>
    </td>
</tr>

<span id="testConnectionButtonWrapper" style="display:none;">
  <forms:submit id="testConnectionButton" type="button" label="Test connection" onclick="BS.SlackConnectionDialog.testConnection();"/>
</span>

<bs:dialog dialogId="testConnectionDialog" title="Test Connection" closeCommand="BS.TestConnectionDialog.close();"
           closeAttrs="showdiscardchangesmessage='false'">
    <div id="testConnectionStatus"></div>
    <div id="testConnectionDetails" class="mono"></div>
</bs:dialog>
