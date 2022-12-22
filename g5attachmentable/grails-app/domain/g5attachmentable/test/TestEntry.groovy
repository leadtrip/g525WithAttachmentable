package g5attachmentable.test

import g5attachmentable.Attachment
import g5attachmentable.core.Attachmentable


class TestEntry implements Attachmentable {

    String title

    String toString() { "$title" }

    def onAddAttachment = { Attachment attachment ->
        println attachment.inputName + ' ' + attachment.filename
    }

}
