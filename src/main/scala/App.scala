package esbook10

import javax.security.auth.login.Configuration

import akka.pattern.after
import esbook10.main
import scala.collection.mutable.{ArrayBuffer, Queue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Try}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps


/*
TODO:
monitor individual requests (queue:mutable, threadsafe)
retry individual connection attempts
retry individual requests
wait before retry
graceful shutdown-how to cancel future?
signals
timeouts on connection and operation
crc of received and sent data for testing
abstract away consumer from underlying server implementation to be able to use bulk or one that provides better granular error handling
config file

throttle pending requests based on: server errors, performance, producer/consumer starvation
report number of open connections to the server

Future:
-what's the cost of creating a new one?
-how to use custom or build int scheduler?
-how to recover?

Later:
bulk api

error handling:
---rejected execution (queue capacity 200)
---duration
 */


object main extends App{

  implicit val system       = ActorSystem("esbook10")
  val MonitorActor          = system.actorOf(Props[Monitor], name = "Monitor")
  implicit val config       = Configuration

  val GlobalDelayFactor                     = 1.5
  val GlobalMaxDelay:FiniteDuration         = 1024 milliseconds

  val init  = Try {
    Consumer.Queue = Queue[String]()
    Producer.process(Consumer.Queue, args(0))
    Consumer.start
    sendEvent(StartSession)

  }
  init match{
    case Success(s)=>showStats
    case Failure(e)=>
  }

  def showStats():Unit={
    implicit val timeout  = Timeout(5.seconds)
    val statsFuture       = MonitorActor ? Get
    val data              = Try{Await.result(statsFuture,timeout.duration)}
    data match{
      case Success(s)=>{
        s match {
          case s:String=>println(s)
          case IsFinished=>system.shutdown();println("exiting...shutting down ActorSystem");return
          case _=>println(s)
        }
      }
      case Failure(f)=>
    }
    //get stats after sec
    after(1 second, using = system.scheduler){
      Future{ showStats }
    }
  }

  //below methods hide tha fact that producer and consumer talk tor actor whilst sending events
  def eventNewJob(id:String)={
    MonitorActor!NewJob(id)
  }
  def eventJobDone(id:String)={
    MonitorActor!JobFinished(id)
  }
  def eventRequestError(id:String)={
    MonitorActor!RequestError(id)
  }
  def getJobsCounter():Int={
    implicit val timeout = Timeout(1.second)
    val future  = MonitorActor ? GetJobsCount
    Await.result(future, timeout.duration).asInstanceOf[Int]
  }

  //todo:rewrite all events in this way
  def sendEvent(event: Any): Unit ={
    event match{
      case e:SetQueueLength=>MonitorActor!e
      case e:IsFinished=>{
        implicit var timeout  = Timeout(5.seconds)
        var future            = MonitorActor ?  e
        future                = Await.result(future,timeout.duration).asInstanceOf[Future[Boolean]]
        timeout               = Timeout(0.second)
        Await.result(future,timeout.duration).asInstanceOf[Boolean]
      }
      case e:Any => MonitorActor! e
      //blocking an waiting for monitor

    }
  }
  //turn into lambda currentDelay.map(currentDelay)etc
  def getDelay(implicit currentDuaration:FiniteDuration): FiniteDuration ={
    currentDuaration match {
      case d:FiniteDuration if d >= GlobalMaxDelay => GlobalMaxDelay
      case d:FiniteDuration if d < GlobalMaxDelay => (d * 1.5).asInstanceOf[FiniteDuration]
    }
  }
  def resetDelay()=1 millisecond
}





