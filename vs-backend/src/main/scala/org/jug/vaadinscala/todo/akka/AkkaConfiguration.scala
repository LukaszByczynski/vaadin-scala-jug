package org.jug.vaadinscala.todo.akka

import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.Inject

import akka.actor._
import com.typesafe.config.ConfigFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.{Bean, Configuration}

import scala.beans.BeanProperty

@Configuration
@ConfigurationProperties("jug.backend")
class AkkaConfiguration {

  @BeanProperty var host: String = _

  @BeanProperty var port: String = _

  @Inject protected var remoteTodoRepository: RemoteTodoRepository = _

  private var _actorSystem: ActorSystem = _

  private var _typedActorSystem: TypedActorFactory = _

  @PostConstruct
  def init(): Unit = {
    val customConf = ConfigFactory.parseString( s"""
      akka.remote.netty.tcp {
        hostname = "$host"
        port = "$port"
      }
    """
    )

    _actorSystem = ActorSystem("Backend", customConf.withFallback(ConfigFactory.load("backend")))
    _typedActorSystem = TypedActor(_actorSystem)

    _typedActorSystem.typedActorOf(
      TypedProps(classOf[RemoteTodoRepository], remoteTodoRepository), "RemoteTodoRepository"
    )
  }

  @Bean
  def actorSystem(): ActorSystem = _actorSystem

  @Bean
  def typedActorSystem(): TypedActorFactory = _typedActorSystem

  @PreDestroy
  def shutdown(): Unit = {
    _actorSystem.shutdown()
    _actorSystem.awaitTermination()
  }
}
