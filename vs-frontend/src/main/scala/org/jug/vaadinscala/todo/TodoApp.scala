package org.jug.vaadinscala.todo

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.{Configuration, ComponentScan}

object TodoApp extends App {
  new SpringApplicationBuilder()
    .sources(classOf[TodoConfiguration])
    .run(args: _*)

  @Configuration
  @ComponentScan
  @EnableConfigurationProperties
  @EnableAutoConfiguration(
    exclude = Array(
      classOf[JacksonAutoConfiguration],
      classOf[JmxAutoConfiguration]
    )
  )
  class TodoConfiguration
}