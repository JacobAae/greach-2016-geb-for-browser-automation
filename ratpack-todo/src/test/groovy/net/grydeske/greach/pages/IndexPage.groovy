package net.grydeske.greach.pages

import geb.Page
import net.grydeske.greach.modules.TodoItemModule
import net.grydeske.greach.modules.MenubarModule

class IndexPage extends Page {
    static url = "/"


    static atCheckWaiting = true
    static at = {
        title == "Todo List"
        $("#count").text()
    }


    static content = {
        count { $("#count") }
        countValue(required: false) { count.text() }

        //tag::module-use[]
        menubar { module MenubarModule }
        todos(required: false) {
            $('li.todo-item').moduleList(TodoItemModule)
        }
        //end::module-use[]

        todo(required: false) { index -> todos[index] }

        todoInput { $("#new-todo") }
        todoSubmit { $("#create-btn") }

      }
}
