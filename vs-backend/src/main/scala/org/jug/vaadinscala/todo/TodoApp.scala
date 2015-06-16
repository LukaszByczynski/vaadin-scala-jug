package org.jug.vaadinscala.todo

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.{ComponentScan, Configuration}

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
      classOf[DataSourceTransactionManagerAutoConfiguration],
      classOf[JmxAutoConfiguration]
    )
  )
  class TodoConfiguration
}