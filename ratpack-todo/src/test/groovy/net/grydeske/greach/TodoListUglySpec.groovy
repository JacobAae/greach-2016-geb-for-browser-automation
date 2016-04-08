package net.grydeske.greach

import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.remote.RemoteControl
import spock.lang.IgnoreIf
import spock.lang.Specification

import geb.spock.GebReportingSpec
import net.grydeske.greach.pages.IndexPage

import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Ignore

@Ignore
@Stepwise
class TodoListUglySpec extends GebReportingSpec {

    @Shared
    def aut = new GroovyRatpackMainApplicationUnderTest()

    def setup() {
        URI base = aut.address
        browser.baseUrl = base.toString()
    }

    //tag::ugly-1[]
    def "Go to index page"() {
        when: 'Go to index url'
        go '/'

        then: 'Verify we are there'
        title == "Todo List"
    }
    //end::ugly-1[]

    //tag::ugly-2[]
    def "Create new todo"() {
        when: 'Input text and submit'
        $("#new-todo") << "Do this"
        $("#create-btn").click()

        then: 'Verify new item present in list'
        waitFor { $("#count").text() == '4'}
        $('li.todo-item').any{ it.text().contains 'Do this' }

        and: 'Verify input field empty'
        !$("#new-todo").text()
    }
    //end::ugly-2[]


    //tag::ugly-3[]
    def "Delete todo item"() {
        when: 'Click delete and accept'
        withConfirm {
            $('button', 4).click()
        }

        then: 'Verify item deleted'
        waitFor { $("#count").text() == '3'}
        $('li.todo-item').every{ !( it.text().contains('Do this')) }
    }
    //end::ugly-3[]

    def cleanupSpec() {
        aut.stop()
    }

}
