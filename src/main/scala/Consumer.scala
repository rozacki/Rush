package esbook10

import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Failure, Try}
import akka.pattern.after
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by chrisrozacki on 16/07/15.
 */
object Consumer{
  var Queue:Queue[String] = null
  var PendingJobs         = 0
  var F:Future[Unit]      = null
  var MaxJobsLimit        = 200

  val MaxRetry            = Integer MAX_VALUE
  val MaxConnTimeout      = 0 second
  val MaxOpTimeout        = 0 second

  def start(){
    Future{process(1.millisecond)}
  }
  //pass each time new callculate retryDelay
  //retryDelay is shared by job control and dequing
  def process(delay:FiniteDuration): Unit = {

    implicit var delayX = delay
    while (true) {

      //after reaching limit check how many jobs are still working
      if(MaxJobsLimit<=PendingJobs){
        //ask how many jobs still working
        PendingJobs  = main.getJobsCounter()
        //if still the same the wait
        if(MaxJobsLimit<=PendingJobs){
          after(delay, using = main.system.scheduler) {
            Future {
              process(main.incDelay(delay))
            }
          }
          return
        }
      }
      val dataTry = Try {
        Queue.dequeue
      }
      dataTry match {
        //
        case Failure(e) => e match {
          case e: java.util.NoSuchElementException => {
            main.sendEvent(EmptyQueue(true))
            after(delay, using = main.system.scheduler) {
              Future {
                process(main.incDelay)
              }
            }
            return
          }
          case _ => println("uncaught exception")
        }
        //we get data now let's try to start new job
        case Success(data) => {
          //do i have to remember reference?
          new Job(data,MaxRetry,main.minDelay)
          PendingJobs+=1
          delayX=main.minDelay
        }
      }
    }
  }
}
