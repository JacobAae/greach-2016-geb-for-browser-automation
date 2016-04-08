package net.grydeske.greach

import io.netty.handler.codec.http.HttpResponseStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.exec.Blocking
import ratpack.form.Form
import ratpack.groovy.handling.GroovyContext
import ratpack.groovy.handling.GroovyHandler
import ratpack.handling.internal.DefaultByMethodSpec
import static ratpack.jackson.Jackson.json

class TodoItemHandler extends GroovyHandler {

    final Logger logger = LoggerFactory.getLogger(this.class)

    @Override
    void handle(GroovyContext context) throws Exception {
        context.byMethod { DefaultByMethodSpec spec -> spec
            spec.get() {
                Blocking.get {
                    TodoItem.withNewSession {
                        TodoItem.list().collect { todoItem ->
                            convertTodoItem(todoItem)
                        }
                    }
                } then { todoList ->
                    context.render(json(todoList))
                }
            }

            spec.post() {
                context.parse(Form).then{ Form form ->
                    Blocking.get {
                        TodoItem.withNewSession {
                            TodoItem todoItem = new TodoItem(text: form['text'],completed: false).save()
                            convertTodoItem(todoItem)
                        }
                    } then { todoItem ->
                        context.render(json(todoItem))
                    }
                }
            }

            spec.patch() {
                context.parse(Form).then{ Form form ->
                    Blocking.get {
                        TodoItem.withNewSession {
                            TodoItem todoItem = TodoItem.get(form['id'] as Long)
                            todoItem.completed = form['completed'] == 'true'
                            todoItem.save(flush:true)
                            convertTodoItem(todoItem)
                        }
                    } then { todoItem ->
                        context.render(json(todoItem))
                    }
                }
            }

            spec.delete() {
                Blocking.get {
                    TodoItem.withNewSession {
                        TodoItem todoItem = TodoItem.get(Long.parseLong(context.allPathTokens['id']) )
                        todoItem.delete(flush:true)
                    }
                } then {
                    context.response.status(HttpResponseStatus.NO_CONTENT.code())
                    context.render(json(['OK']))
                }

            }
        }
    }

    private Map convertTodoItem(TodoItem todoItem) {
        [
                id: todoItem.id,
                version: todoItem.version,
                text: todoItem.text,
                completed: todoItem.completed,
                created: todoItem.dateCreated,
                updated: todoItem.lastUpdated
        ]
    }


}
