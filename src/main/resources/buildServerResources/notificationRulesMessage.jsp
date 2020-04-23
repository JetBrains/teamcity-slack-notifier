<jsp:useBean id="editConnectionUrl" scope="request" type="java.lang.String"/>

<p>
    Specify the builds and events you want to be notified about via Slack messages.
    <br/>
    To receive notifications for all projects, configure a connection to Slack in the
    <a href="${editConnectionUrl}">
        Root project's settings
    </a>
    and select it in the drop-down menu below.
    <br/>
    To receive notifications for a specific project, configure the Slack connection directly in this project's settings
    instead.
</p>
