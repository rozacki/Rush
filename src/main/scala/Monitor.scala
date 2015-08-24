package esbook10

import akka.actor.{ActorSystem, Props, Actor}
import concurrent.{Promise,Future}

//events
//todo use traits on events
case class NewJob(id:String)
case class JobFinished(id:String)
case class Get
case class ConnectionError
case class RequestError(id:String)
case class StartSession
case class GetJobsCount
//sent by producer when there is no more data to send
case class FinishedProducing
case class IsFinished
case class EmptyQueue(yes:Boolean)

//todo:
//format as JSON store in file
//use class serializer to serialise the state
class Monitor extends Actor{
  var JobsCounter=0
  var ConnectionErrors=0
  var RequestErrors=0
  var Producing=false
  var IsFinishedPromise:Promise[Boolean] = null
  var IsFinishedFuture:Future[Boolean]  = null
  var FinishedJobs=0
  var IsEmptyQueue = false

  def receive={
    case StartSession=> {
      JobsCounter = 0;
      ConnectionErrors = 0;
      RequestErrors = 0;
      Producing = true;
      IsFinishedPromise = Promise[Boolean];
      IsFinishedFuture = IsFinishedPromise.future;
      FinishedJobs=0
    }
    //todo: queue may be not empty
    case Get=> if(Producing==false&&JobsCounter==0&&IsEmptyQueue==true)
                  //it should not be finished but about to finish as there are messages from jobs in the mailbox!
                  sender!IsFinished
               else
                  sender! "jobs:%d, finished_jobs:%d, connection_errors:%d, request_errors:%d, is_empty_queue:%s, producing:%s".
                    format(JobsCounter,FinishedJobs,ConnectionErrors,RequestErrors, IsEmptyQueue.toString, Producing.toString)
    case NewJob(j)=>JobsCounter+=1
    case JobFinished(j)=>{
      JobsCounter-=1;FinishedJobs+=1;checkFinished
    }
    case ConnectionError=>ConnectionErrors+=1
    case RequestError=>RequestErrors+=1
    case GetJobsCount=>sender!JobsCounter
    case FinishedProducing=>Producing=false;checkFinished
    //if client wants to know when is finished then we have to deliver future and keep promise for this future
    case IsFinished=>sender!IsFinishedFuture
    case EmptyQueue(yes)=>IsEmptyQueue=yes
    case _=>println("he?")
  }

  def checkFinished()={
    if(Producing==false&&JobsCounter==0&&IsFinishedPromise.isCompleted!=true){
      //set future that is awaited by the main thread
      IsFinishedPromise.success(true)
    }
  }
  def calculatePace():Int={
    0
  }
}
