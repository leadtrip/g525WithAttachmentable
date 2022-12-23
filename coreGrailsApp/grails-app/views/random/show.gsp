<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main" />
    <title>Show random</title>
</head>

<body>
    <div class="container">

        <div id="updateRes"></div>

        <ol class="property-list">
            <li class="fieldcontain">
                <span class="property-label">Name</span>
                <span class="property-value">${person.fullName()}</span>
            </li>

            <li class="fieldcontain">
                <span class="property-label">Total attachments</span>
                <span class="property-value"><attachments:total bean="${person}"/></span>
            </li>

            <attachments:each bean="${person}" inputNames="attachment">
                <li class="fieldcontain">
                    <span class="property-label">
                        <g:link action="showAttachment" id="${attachment.id}">
                            <attachments:icon attachment="${attachment}" />
                        </g:link>
                    </span>
                    <span class="property-value">
                    <attachments:deleteLink
                        attachment="${attachment}"
                        returnPageURI="${createLink(action: actionName, id: person.id)}">[X]
                    </attachments:deleteLink>
                    <attachments:downloadLink
                            attachment="${attachment}"
                            withContentType="true"/>
                    </span>
                </li>
            </attachments:each>
            <li class="fieldcontain">
                <span class="property-label"></span>
                <span class="property-value">
                    <attachments:uploadForm
                            bean="${person}"
                            styleClass="uploadFormContainer"/>
                    <attachments:script
                            updateInterval="100"
                            updateElemId="updateRes"
                            redirect="${createLink(action: actionName, id: person.id)}"/>
                </span>
            </li>

        </ol>
    </div>
</body>
</html>