<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags"%>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout"%>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms"%>
<%@ taglib prefix="authz" tagdir="/WEB-INF/tags/authz"%>
<%@ taglib prefix="afn" uri="/WEB-INF/functions/authz"%>
<%@ taglib prefix="graph" tagdir="/WEB-INF/tags/graph"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props"%>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util"%>

<jsp:useBean id="slackSettings" scope="request" type="jetbrains.buildServer.notification.slackNotifier.SlackSettingsBean"/>

<bs:linkScript>
    /js/bs/testConnection.js
</bs:linkScript>

<c:url value="/admin/jbSlackNotifier/notifierSettings.html" var="url"/>

<div id="settingsContainer">
    <form action="${url}" method="post" autocomplete="off">
        <div class="editNotificatorSettingsPage">
            <c:choose>
                <c:when test="${slackSettings.paused}">
                    <div class="headerNote">
                        <bs:buildTypePausedIcon/>The notifier is <strong>disabled</strong>. All email notifications are
                        suspended&nbsp;&nbsp;<a class="btn btn_mini" href="#" id="enable-btn">Enable</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="enableNote">
                        The notifier is <strong>enabled</strong>&nbsp;&nbsp;<a class="btn btn_mini" href="#"
                                                                               id="disable-btn">Disable</a>
                    </div>
                </c:otherwise>
            </c:choose>

            <bs:messages key="settingsSaved"/>
            <table class="runnerFormTable">
                <tr>
                    <th><label for="botToken">Bot token:<l:star/></label></th>
                    <td>
                        <forms:passwordField name="botToken" encryptedPassword="${slackSettings.encryptedBotToken}"
                                             className="longField"/>
                        <span class="error" id="errorBotToken"></span>
                        <span class="smallNote">
                            Provide <a href="https://api.slack.com/docs/token-types#bot" target="_blank">bot token</a> to be used to connect to your Slack workspace.
                            <br/>
                            You can see your apps or create a new one on <a href="https://api.slack.com/apps"
                                                                            target="_blank">Your Apps</a> page.
                            <br/>
                            This bot will need the following scopes: <i>channels:read, chat:write, im:read, im:write, users:read</i>
                        </span>
                    </td>
                </tr>
            </table>

            <div class="saveButtonsBlock" id="saveButtons">
                <forms:submit label="Save"/>
                <forms:submit id="testConnection" type="button" label="Test connection" onclick=""/>
                <input type="hidden" id="submitSettings" name="submitSettings" value="store"/>
                <input type="hidden" name="testAddress" id="testAddress" value=""/>
                <forms:saving/>
            </div>
        </div>
    </form>
</div>

<bs:dialog dialogId="testConnectionDialog" title="Test Connection" closeCommand="SlackTestConnectionDialog.close();"
           closeAttrs="showdiscardchangesmessage='false'">
    <div id="testConnectionStatus"></div>
    <div id="testConnectionDetails" class="mono"></div>
</bs:dialog>
<forms:modified/>

<script type="text/javascript">
    (function ($) {
        var sendAction = function (enable) {
            $.post("${url}&action=" + (enable ? 'enable' : 'disable'), function () {
                BS.reload(true);
            });
            return false;
        };

        $("#enable-btn").click(function () {
            return sendAction(true);
        });

        $("#disable-btn").click(function () {
            BS.confirm("Slack notifications will not be sent until enabled. Disable the notifier?", function () {
                return sendAction(false);
            });
            return false;
        });
    })(jQuery);
</script>


<script type="text/javascript">
    $j(function () {
        <c:if test="${not afn:permissionGrantedGlobally('CHANGE_SERVER_SETTINGS')}">
        $j('#saveButtons').addClass("hidden");
        $j('#enable-btn').addClass("hidden");
        $j('#disable-btn').addClass("hidden");
        </c:if>


    });
</script>
