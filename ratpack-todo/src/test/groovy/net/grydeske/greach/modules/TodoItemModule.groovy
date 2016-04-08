package net.grydeske.greach.modules

import geb.Module

class TodoItemModule  extends Module {

    static content = {
        checkbox { $('input', type: 'checkbox') }
        label { $('label').text() }
        deleteBtn { $('button') }
    }

}
