package org.jug.vaadinscala.todo.persist

import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.sql.DataSource

import org.springframework.stereotype.Component

import scala.slick.driver.PostgresDriver.simple._

@Component
class DBProvider {

  @Inject protected var dataSource: DataSource = _

  private var _database: Database = _

  @PostConstruct def init(): Unit = {
    _database = Database.forDataSource(dataSource)
  }

  def apply(): Database = _database

}
