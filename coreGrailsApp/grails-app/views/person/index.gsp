<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main" />
    <title>Random</title>
</head>

<body>
    <table>
        <tr>
            <th>Person</th>
            <th>Total attachments</th>
            <th>Attachments</th>
        </tr>
        <g:each in="${people}" var="person">
            <tr>
                <td><g:link action="show" id="${person.id}">${person.fullName()}</g:link></td>
                <td>${person.attachments.size()}</td>
                <td>${person.attachments*.name.join(',')}</td>
            </tr>
        </g:each>
    </table>
</body>
</html>