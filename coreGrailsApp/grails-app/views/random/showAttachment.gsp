<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main" />
    <g:set var="entityName" value="${message(code: 'attachment.label', default: 'Attachment')}" />
    <title><g:message code="default.show.label" args="[entityName]" /></title>
    <title>Show attachment</title>
</head>

<body>
<div id="content" role="main">
    <div class="container">
        <section class="row">
            <div id="show-attacment" class="col-12 content scaffold-show" role="main">
                <h1><g:message code="default.show.label" args="[entityName]" /></h1>
                <g:if test="${flash.message}">
                    <div class="message" role="status">${flash.message}</div>
                </g:if>
                <f:display bean="attachment" />
            </div>
        </section>
    </div>
</div>
</body>
</html>