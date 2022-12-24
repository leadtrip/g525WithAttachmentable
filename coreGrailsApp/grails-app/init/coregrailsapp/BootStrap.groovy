package coregrailsapp

import grails.gorm.transactions.Transactional

class BootStrap {

    def init = { servletContext ->
        addPeople()
    }

    @Transactional
    def addPeople() {
        new Person( forename: 'Dave', surname: 'White', dob: new Date() - 20000 ).save(failOnError: true)
        new Person( forename: 'Chris', surname: 'Brown', dob: new Date() - 15000 ).save(failOnError: true)
        new Person( forename: 'Mary', surname: 'Green', dob: new Date() - 10000 ).save(failOnError: true)
        new Person( forename: 'Sue', surname: 'Red', dob: new Date() - 5000 ).save(failOnError: true)
        new Person( forename: 'Rob', surname: 'Black', dob: new Date() - 2000 ).save(failOnError: true)
    }

    def destroy = {
    }
}
