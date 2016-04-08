import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import net.grydeske.greach.TodoItem
import net.grydeske.greach.TodoItemHandler
import org.h2.Driver
import org.springframework.context.support.GenericApplicationContext
import org.springframework.jdbc.datasource.DriverManagerDataSource
import ratpack.groovy.template.TextTemplateModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.exec.Blocking

import org.slf4j.Logger
import org.slf4j.LoggerFactory

final Logger logger = LoggerFactory.getLogger(this.class)


ratpack {
    bindings {
        bindInstance new Service() {
            void onStart(StartEvent e) {
                Blocking.exec {
                    GenericApplicationContext appCtx = new GenericApplicationContext()

                    def dataSource = new DriverManagerDataSource("jdbc:h2:mem:grailsDb1;DB_CLOSE_DELAY=-1", 'sa', '')
                    dataSource.driverClassName = Driver.name
                    appCtx.beanFactory.registerSingleton 'dataSource', dataSource

                    def initializer = new HibernateDatastoreSpringInitializer(TodoItem)

                    initializer.configureForBeanDefinitionRegistry(appCtx)
                    appCtx.refresh()

                    TodoItem.withNewSession {
                        if( !TodoItem.count() ) {
                            new TodoItem(text: "Give Geb presentation",completed: false).save()
                            new TodoItem(text: "Have fun",completed: false).save()
                            new TodoItem(text: "Sangria",completed: false).save()
                        }
                    }
                }
            }
        }
        add(new TodoItemHandler())
        module(TextTemplateModule) {
            it.staticallyCompile  = true
        }
        bindInstance ratpack.remote.RemoteControl.handlerDecorator()
    }

    handlers {
        files {
            dir("public")
        }

        prefix('items/:id?') {
            all(TodoItemHandler)
        }

        prefix('about') {
            get {
                render groovyTemplate([:], 'about.html')
            }
        }

        get {
            def model = [x:'y' ]
            render groovyTemplate(model, 'main.html')
        }

    }
}
