package coregrailsapp

import g5attachmentable.Attachment
import grails.gorm.transactions.ReadOnly

class PersonController {

    PersonService personService

    def index() {
        [people: Person.list()]
    }

    def show() {
        [person: personService.get(params.id)]
    }

    @ReadOnly
    def showAttachment() {
        [attachment: Attachment.get(params.id)]
    }
}
