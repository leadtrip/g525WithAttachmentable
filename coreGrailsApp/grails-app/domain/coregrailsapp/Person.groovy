package coregrailsapp

import g5attachmentable.Attachment
import g5attachmentable.core.Attachmentable

class Person implements Attachmentable {

    String forename
    String surname
    Date dob

    String fullName() {
        "${forename} ${surname}"
    }

    static constraints = {
    }

    def onAddAttachment = { Attachment attachment ->
        println attachment.inputName + ' ' + attachment.filename
    }
}
