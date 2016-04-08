package net.grydeske.greach

import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.remote.RemoteControl
import spock.lang.IgnoreIf
import spock.lang.Specification

import geb.spock.GebReportingSpec
import net.grydeske.greach.pages.IndexPage
import net.grydeske.greach.pages.AboutPage

import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class TodoListFunctionalSpec extends GebReportingSpec {

    //tag::remote1[]
    @Shared
    def aut = new GroovyRatpackMainApplicationUnderTest()

    RemoteControl remoteControl

    def setup() {
        URI base = aut.address
        browser.baseUrl = base.toString()
        remoteControl = new RemoteControl(aut)
    }
    //end::remote1[]

    //tag::structured-1[]
    def "Go to index page"() {
        when: 'Go to index url'
        to IndexPage

        then: 'Verify 3 items present'
        at IndexPage
        countValue == '3'
    }
    //end::structured-1[]

    //tag::structured-2[]
    def "Create new todo"() {
        when: 'Input text and submit'
        todoInput = "Do this"
        todoSubmit.click()

        then: 'Verify new item present in list'
        waitFor { countValue == '4'}
        todos.any{ it.label == 'Do this' }

        and: 'Verify input field empty'
        !todoInput.text()
    }
    //end::structured-2[]

    //tag::structured-3[]
    def "Delete todo item"() {
        when: 'Click delete and accept'
        withConfirm {
            todos.find{ it.label == 'Do this' }.deleteBtn.click()
        }

        then: 'Verify item deleted'
        waitFor {countValue == '3'}
        todos.every{ it.label != 'Do this' }
    }
    //end::structured-3[]

    // Ignore for phantomJs as RemoteControl causes: MultipleCompilationErrorsException
    @IgnoreIf({ System.getProperty('geb.env') == 'phantomJs' })
    //tag::remote2[]
    def "Complete item and check database"() {
        when: 'Check item done'
        todos.find{ it.label == 'Give Geb presentation' }
                .checkbox.click()

        then: 'Use remote control to check database'
        remoteControl.exec {
            Boolean completed
            TodoItem.withNewSession {
                TodoItem todoItem = TodoItem
                        .findByText('Give Geb presentation')
                completed = todoItem.completed
            }
            completed
        } == true
    }
    //end::remote2[]

    @IgnoreIf({ System.getProperty('geb.env') == 'phantomJs' })
    def "Create batch items"() {
        setup: 'Generate 97 new items remotely'
        def ids = remoteControl.exec {
            def ids = []
            TodoItem.withNewSession {
                97.times {
                    TodoItem todoItem = new TodoItem(text: "Remote created", completed: (it % 2 == 0) )
                    todoItem.save()
                    ids << todoItem.id
                }
            }
            ids
        }

        when: 'Refresh indexpage'
        to IndexPage

        then: 'Verify count is 100'
        waitFor { countValue == '100'}

        cleanup:
        remoteControl.exec {
            TodoItem.withNewSession {
                TodoItem.list().each {
                    if(it.text == "Remote created" ) {
                        it.delete()
                    }
                }
            }
            true
        }
    }

    //tag::module-1[]
    void "Test navigation to about"() {
        when: 'Click about menu item'
        menubar.about.click()

        then: 'Ensure we end at about page'
        at AboutPage
    }
    //end::module-1[]

    def cleanupSpec() {
        aut.stop()
    }

}
