package esbook10

import scala.collection.mutable.Queue
import scala.concurrent.Future
import scala.io.Source
import scala.util.{Success, Failure, Try}
import akka.pattern.after
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by chrisrozacki on 16/07/15.
 */
object Producer{

  val MaxQueueSize  = 100

  def process(Queue:Queue[String], FileName:String) {

    val fileTry = Try {
      Source.fromFile(FileName)
    }

    val iteratorTry = fileTry match {
      case Failure(e) => {
        throw e
      }
      case Success(e) => Try {
        fileTry.get.getLines()
      }
    }

    val iterator = iteratorTry match {
      case Failure(e) => throw e
      case Success(e) => e
    }

    enque(1 millisecond)

    def enque(delay:FiniteDuration) {
      implicit var delayX = delay
      //def closure here
      while (iterator.hasNext) {
        if (Queue.length >= MaxQueueSize) {
          after(delay, using = main.system.scheduler) {
            Future {
              enque(main.getDelay)
            }
          }
          return
        } else {
          delayX=main.resetDelay
          Queue.enqueue(iterator.next)
        }
      }
      //
      for(f<-fileTry) f.close
      main.sendEvent(FinishedProducing)
      println("finished producing")
    }
  }
}
