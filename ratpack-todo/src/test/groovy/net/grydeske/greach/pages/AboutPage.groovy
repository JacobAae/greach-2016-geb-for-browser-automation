package net.grydeske.greach.pages

import geb.Page

class AboutPage extends Page {

    static url = "/about"

    static at = {
        title == "About"
    }

    static content = {
        header { $('h1', 0) }
    }
}
