package org.jug.vaadinscala.todo.async

import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

import akka.util.Timeout
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

import scala.beans.BeanProperty

@ConfigurationProperties("jug.async")
@Configuration
class AsyncConfiguration {

  @BeanProperty var timeout: Int = _

  @PostConstruct def init() {
    Async.timeout = new Timeout(timeout, TimeUnit.SECONDS).duration
  }

}
