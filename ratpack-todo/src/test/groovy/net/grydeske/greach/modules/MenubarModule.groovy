package net.grydeske.greach.modules

import geb.Module

class MenubarModule  extends Module {

    static base = { $("nav.navbar") }

    static content = {
        home { $('a', text: 'Todo List') }
        about { $('a', text: 'About') }
    }

}
