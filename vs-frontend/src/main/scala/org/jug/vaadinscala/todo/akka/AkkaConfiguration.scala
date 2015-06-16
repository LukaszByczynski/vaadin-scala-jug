package org.jug.vaadinscala.todo.akka

import javax.annotation.{PostConstruct, PreDestroy}

import akka.actor._
import com.typesafe.config.ConfigFactory
import org.jug.vaadinscala.todo.TodoRepository
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.{Scope => BeanScope, _}

import scala.beans.BeanProperty

@Configuration
@ConfigurationProperties("jug.backend")
class AkkaConfiguration {

  private lazy val _backendUri = s"akka.tcp://Backend@$host:$port/user"

  private lazy val _actorSystem = ActorSystem("WebActorSystem", ConfigFactory.load("frontend"))

  private lazy val _typedActorSystem = TypedActor(_actorSystem)

  @BeanProperty var host: String = _

  @BeanProperty var port: String = _

  @PostConstruct
  def init() {
    _actorSystem.isTerminated
  }

  @PreDestroy
  def shutdown() {
    _actorSystem.shutdown()
    _actorSystem.awaitTermination()
  }

  @Lazy
  @Bean
  def actorSystem(): ActorSystem = {
    _actorSystem
  }

  @Lazy
  @Bean
  @BeanScope("vaadin-ui")
  def remoteTodoRepositoryProducer(): TypedActorProxy[TodoRepository] = {
    _resolveActor("RemoteTodoRepository", classOf[TodoRepository])
  }


  private def _resolveActor[T <: AnyRef](actorName: String, actorClass: Class[T]): TypedActorProxy[T] = {
    lazy val remoteActor = _actorSystem.actorOf(
      Props(classOf[PerRequestSelectionActor], s"${_backendUri}/$actorName")
    )
    new TypedActorProxy[T](
      () => _typedActorSystem.typedActorOf(TypedProps(actorClass), remoteActor),
      _ => {
        _typedActorSystem.stop _
        _actorSystem.stop(remoteActor)
      }
    )
  }

  case class BackendConnectionException(msg: String) extends RuntimeException(msg)

}
