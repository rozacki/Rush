package esbook10

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
import akka.pattern.after
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by chrisrozacki on 16/07/15.
 */
class Job(data:String, maxAttempts:Int, delay:FiniteDuration){
  val Data:String = data
  val MaxAttempts = maxAttempts
  val Delay       = delay
  var Attempts    =  0
  val GUID        = ""
  //if we use central, persistent data repository ( could be source itself) and repo where we track jobs then we can survive disaster
  //or is it better to start off again?
  val DataHash=""
  main.eventNewJob(GUID)

  //start writing
  write

  def write(): Unit ={
    if(Attempts<MaxAttempts)
      TestServer.write(data) match{
        case Success(s)=>main.eventJobDone(GUID)
        case Failure(e)=>{
          after(Delay,using=main.system.scheduler){
            Attempts+=1
            Future{
              write()
            }
          }
        }
      }
  }
}