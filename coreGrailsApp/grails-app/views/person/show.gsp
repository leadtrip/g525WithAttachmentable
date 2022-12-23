<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main" />
    <title>Show person</title>
</head>

<body>
    <div class="container">

        <section class="row" style="padding-top: 10px">
            <div class="border border-info rounded">
                <g:if test="${person.attachments}">
                    Click an attachment icon to view the Attachment domain details, click the [X] to delete the attachment or click the filename to download the file
                </g:if>
                <g:else>
                    Select choose files and add as many attachments as you like then upload
                </g:else>
            </div>
        </section>

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