@Grapes([
		@Grab("org.gebish:geb-core:0.12.2"),
		@GrabExclude('org.codehaus.groovy:groovy-all'),
		@Grab("org.seleniumhq.selenium:selenium-firefox-driver:2.53.0"),
		@Grab("org.seleniumhq.selenium:selenium-support:2.53.0")
])

import geb.Browser
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.Keys

Browser browser = new Browser(driver: new FirefoxDriver())

browser.with {
// tag::standalone1[]
	go "http://duckduckgo.com"

	$('input', name: 'q').value("Greach Conference")
	$('input', name: 'q') << Keys.ENTER

	waitFor(10, 1) { $("#links").displayed }
    sleep(3000) // For demo reasons

	$("h2.result__title").first().click()

	waitFor { title.startsWith "Greach" }
// end::standalone1[]
}

sleep(10000)

browser.close()

