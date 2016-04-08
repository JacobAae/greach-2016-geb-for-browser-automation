package net.grydeske.greach

import grails.persistence.Entity

@Entity
class TodoItem {

    Long id
    Long version

    String text
    Boolean completed

    Date dateCreated
    Date lastUpdated

    static constraints = {
        text minSize: 3
    }

}