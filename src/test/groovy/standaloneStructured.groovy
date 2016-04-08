@Grapes([
		@Grab("org.gebish:geb-core:0.12.1"),
		@Grab("org.seleniumhq.selenium:selenium-firefox-driver:2.53.0"),
		@Grab("org.seleniumhq.selenium:selenium-support:2.53.0")
])

import geb.Browser
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.Keys

Browser browser = new Browser(driver: new FirefoxDriver())

// tag::standalone1[]
class DuckDuckGoPage extends geb.Page {

    static url = "http://duckduckgo.com"

    static at = { title ==~ /DuckDuckGo/ }

    static content = {
        inputField{ $('input', name: 'q') }
    }

    def submit() {
        inputField << Keys.ENTER
    }
}
// end::standalone1[]

// tag::standalone2[]
class DuckDuckGoResultPage extends geb.Page {

	static url = "http://duckduckgo.com"

	static at = { $("#links").displayed }

	static content = {
		links{ $("h2.result__title") }
	}

	def clickLink(int linkNumber) {
		links[linkNumber].click()
	}
}
// end::standalone2[]

// tag::standalone3[]
class GreachPage extends geb.Page {
	static at = { title.startsWith("Greach") }
}
// end::standalone3[]


// tag::standalone4[]
browser.with {
    to DuckDuckGoPage

    inputField << "Greach Conference"
    submit()

    waitFor(10, 0.5) {
        at DuckDuckGoResultPage
    }

    sleep(3000) // For demo reasons

    clickLink(0)

    waitFor {
        at GreachPage
    }
}
// end::standalone4[]

sleep(10000)

browser.close()