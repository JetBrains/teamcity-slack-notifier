<%@ taglib prefix="ext" tagdir="/WEB-INF/tags/ext" %>
<%@ taglib prefix="et" tagdir="/WEB-INF/tags/eventTracker" %>
<%@ taglib prefix="queue" tagdir="/WEB-INF/tags/queue" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="p" tagdir="/WEB-INF/tags/p" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags/tags" %>
<%@ taglib prefix="n" tagdir="/WEB-INF/tags/notifications" %>
<%@ taglib prefix="profile" tagdir="/WEB-INF/tags/userProfile" %>
<%@ taglib prefix="ufn" uri="/WEB-INF/functions/user" %>
<%@ taglib prefix="changefn" uri="/WEB-INF/functions/change" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="problems" tagdir="/WEB-INF/tags/problems" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>


<%--
  ~  Copyright 2000-2020 JetBrains s.r.o.
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

<jsp:useBean id="propertiesBean" type="jetbrains.buildServer.controllers.BasePropertiesBean" scope="request"/>
<jsp:useBean id="availableConnections"
             type="java.util.List<jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor>" scope="request"/>
<jsp:useBean id="properties" type="jetbrains.buildServer.notification.slackNotifier.SlackProperties" scope="request"/>
<jsp:useBean id="buildTypeId" type="java.lang.String" scope="request"/>
<jsp:useBean id="createConnectionUrl" type="java.lang.String" scope="request"/>

<c:set var="autocompletionUrl" value="/admin/notifications/jbSlackNotifier/autocompleteUserId.html"/>

<script type="text/javascript">
    BS.SlackNotifierSettings = {
        createFindUserFunction: function () {
            return function (request, response) {
                var term = request.term;
                var connectionId = $j("#${properties.connectionKey.replace(':', '-')} option:selected").val();

                if (!connectionId) {
                    response([]);
                    return;
                }

                var url = '${autocompletionUrl}' + '?term=' + encodeURIComponent(term) +
                    '&' + encodeURIComponent('plugin:notificator:jbSlackNotifier:connection') +
                    '=' + encodeURIComponent(connectionId);

                $j.getJSON(window["base_uri"] + url, function (data) {
                    response(data);
                    $j("#channel-autocomplete-loader").hide();
                });
            };
        },

        createSearchUserFunction: function () {
            return function () {
                $j("#channel-autocomplete-loader").show();
            };
        },

        onMessageFormatChange: function () {
            var select = document.getElementById("${properties.messageFormatKey}")
            var selectedFormat = select.options[select.selectedIndex].value;

            $j(".messageFormatOption").hide();
            $j("." + selectedFormat + "FormatOption").show();

            if (selectedFormat === "verbose") {
                this.showVerboseOptions();
            } else {
                this.hideVerboseOptions();
            }
        },

        showVerboseOptions: function() {
            var maximumNumberOfChanges = document.getElementById("${properties.maximumNumberOfChangesKey}");
            if (!maximumNumberOfChanges.value) {
                maximumNumberOfChanges.value = "${propertiesBean.defaultProperties[properties.maximumNumberOfChangesKey]}";
            }
            this.onAddChanges();
        },

        hideVerboseOptions: function() {
            document.getElementById("${properties.addChangesKey}").checked = false;
            document.getElementById("${properties.addBranchKey}").checked = false;
            document.getElementById("${properties.addBuildStatusKey}").checked = false;
            document.getElementById("${properties.maximumNumberOfChangesKey}").value = "";
            this.onAddChanges();
        },

        onAddChanges: function() {
            var addChanges = document.getElementById("${properties.addChangesKey}");
            var maximumNumberOfChanges = document.getElementById("${properties.maximumNumberOfChangesKey}");
            maximumNumberOfChanges.disabled = !addChanges.checked;
        }
    };
</script>

<tr>
    <th>
        <label for="${properties.connectionKey}">
            Connection:<l:star/>
        </label>
    </th>
    <td>
        <c:choose>
            <c:when test="${empty availableConnections}">
                No suitable Slack connections were found. You can configure a connection in the
                <a href="${createConnectionUrl}"> parent project's settings</a>.
            </c:when>
            <c:otherwise>
                <props:selectProperty
                        name="${properties.connectionKey}"
                        id="${properties.connectionKey.replace(':', '-')}"
                        className="longField"
                >
                    <props:option value="">-- Select Slack connection --</props:option>
                    <c:forEach var="connection" items="${availableConnections}">
                        <props:option value="${connection.id}">
                            <c:out
                                    value="${connection.connectionDisplayName}"/>
                        </props:option>
                    </c:forEach>
                </props:selectProperty>
            </c:otherwise>
        </c:choose>

        <span class="error" id="error_${properties.connectionKey}"></span>
    </td>
</tr>

<tr>
    <th>
        <label for="${properties.channelKey}">
            Channel or user ID:<l:star/>
        </label>
    </th>
    <td>
        <props:textProperty
                name="${properties.channelKey}"
                id="${properties.channelKey}"
                value="${propertiesBean.properties[properties.channelKey]}"
                className="longField"
        />
        <forms:saving id="channel-autocomplete-loader" style="display: none;" savingTitle="Fetching autocomplete data"/>
        <span class="error" id="error_${properties.channelKey}"></span>
        <span class="smallNote">
            Specify where messages should be sent.
            <br/>
            Channel IDs should start with the '#' symbol. Bot should be added to the provided channel to be able to send notifications.
        </span>
    </td>
</tr>

<tr>
    <th>
        <label for="${properties.messageFormatKey}">
            Message format:
        </label>
    </th>
    <td>
        <props:selectProperty name="${properties.messageFormatKey}"
                              onchange="BS.SlackNotifierSettings.onMessageFormatChange()">
            <props:option value="simple">Simple</props:option>
            <props:option value="verbose">Verbose</props:option>
        </props:selectProperty>
    </td>
</tr>

<tr class="messageFormatOption verboseFormatOption">
    <th>
    </th>
    <td>
        <props:checkboxProperty name="${properties.addBuildStatusKey}"/> <label for="${properties.addBuildStatusKey}">Add status text</label>
        <br/>
        <props:checkboxProperty name="${properties.addBranchKey}"/> <label for="${properties.addBranchKey}">Add branch name</label>
        <br/>
        <props:checkboxProperty name="${properties.addChangesKey}" onclick="BS.SlackNotifierSettings.onAddChanges();"/> <label for="${properties.addChangesKey}">Add changes</label>
        <br/>
        <label for="${properties.maximumNumberOfChangesKey}">Maximum number of changes:</label>
        <br/>
        <props:textProperty name="${properties.maximumNumberOfChangesKey}" maxlength="4"/>
        <span class="error" id="error_${properties.maximumNumberOfChangesKey}"></span>
    </td>
</tr>

<script>
    $j(document.getElementById("${properties.channelKey}")).autocomplete({
        source: BS.SlackNotifierSettings.createFindUserFunction(),
        search: BS.SlackNotifierSettings.createSearchUserFunction(),
        minLength: 3
    });

    $j(document.getElementById("${properties.channelKey}")).placeholder();

    BS.SlackNotifierSettings.onMessageFormatChange();
    BS.SlackNotifierSettings.onAddChanges();
</script>