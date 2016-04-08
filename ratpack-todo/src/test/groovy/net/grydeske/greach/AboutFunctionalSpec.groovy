package net.grydeske.greach

import geb.spock.GebReportingSpec
import net.grydeske.greach.pages.AboutPage
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import spock.lang.Shared
import spock.lang.Stepwise
import org.openqa.selenium.JavascriptExecutor

@Stepwise
class AboutFunctionalSpec extends GebReportingSpec {

    //tag::ratpack-geb[]
    @Shared
    def aut = new GroovyRatpackMainApplicationUnderTest()

    def setup() {
        URI base = aut.address
        browser.baseUrl = base.toString()
    }
    //end::ratpack-geb[]


    def "Go to About page"() {
        when: 'Go to About'
        to AboutPage

        then: 'Verify at About Page'
        at AboutPage
    }

    def "Test retrieving information"() {
        expect: 'Query p element'
        //tag::retrieving-info[]
        $("p").text() == "Sample text"
        $("#sample").tag() == "p"
        $("p").@title == "Sample p element"
        $("p").classes() == ["class-a", "class-b"]
        //end::retrieving-info[]
    }

    def cleanupSpec() {
        aut.stop()
    }

    void "Demo pause button"() {
        // tag::pause[]
        when: 'Demoing pause'
        pause() // Pause Geb until button pressed
        // end::pause[]

        then: 'Accept when pause is over'
        true
    }

    def js( String script ){
        (driver as JavascriptExecutor).executeScript( script )
    }

    // https://github.com/tomaslin/grails-test-recipes
    // tag::pause-geb[]
    private void pause() {
        js.exec """(function() {
          window.__gebPaused = true;
          var div = document.createElement("div");
          div.setAttribute('style',
            "position: absolute; top:0px;right: 0px;z-index: 3000;\\
            padding: 10px; background-color: red;");
          var button = document.createElement("button");
          button.innerHTML = "Unpause Geb";
          button.onclick = function() {
              window.__gebPaused = false;
          }
          div.appendChild(button);
          document.getElementsByTagName("body")[0].appendChild(div);
        })();"""
        waitFor(300) { !js.__gebPaused }
    }
    // end::pause-geb[]

}
