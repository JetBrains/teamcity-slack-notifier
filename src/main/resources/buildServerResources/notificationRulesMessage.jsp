<jsp:useBean id="editConnectionUrl" scope="request" type="java.lang.String"/>

<p class="notificationRulesMessage">
    Specify the builds and events you want receive Slack messages about.
    <br/>
    In order to receive notifications, you need to
    <a href="${editConnectionUrl}">
        configure Slack connection in Root project
    </a>
    or any other project you'd like to limit notifications to.
</p>

