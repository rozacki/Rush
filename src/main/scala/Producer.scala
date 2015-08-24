package esbook10

import scala.collection.mutable.Queue
import scala.concurrent.Future
import scala.io.Source
import scala.util.{Success, Failure, Try}
import akka.pattern.after
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.io._
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.util.PDFTextStripper

/**
 * Created by chrisrozacki on 16/07/15.
 */
trait StreamProducer{
  def process(Queue:Queue[String], FileName:String)
}

trait FileProducer{
  def process(Queue:Queue[String], FileName:String)
}

object ProducerCSV extends FileProducer{

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
              enque(main.incDelay)
            }
          }
          return
        } else {
          delayX=main.minDelay
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

object ProducerPDF extends FileProducer{

  val MaxQueueSize  = 100

  /* error handling*/
  def process(Queue:Queue[String], FileName:String): Unit ={
    val pdf = PDDocument.load(new File(FileName))
    val nPages = pdf.getNumberOfPages()
    val stripper = new PDFTextStripper
    var currentPage = 1

    enque(1 millisecond)

    def getPage(start:Int):String={
      stripper.setStartPage(start)
      stripper.setEndPage(start+1)
      stripper.getText(pdf)
    }

    def enque(delay:FiniteDuration) {
      implicit var delayX = delay
      //def closure here
      while (currentPage<nPages) {
        if (Queue.length >= MaxQueueSize) {
          after(delay, using = main.system.scheduler) {
            Future {
              enque(main.incDelay)
            }
          }
          return
        } else {
          delayX=main.minDelay
          Queue.enqueue(getPage(currentPage))
          currentPage+=1
        }
      }
      //for(p<-pdf) p.close
      //for(f<-fileTry) f.close

      main.sendEvent(FinishedProducing)
      println("finished producing")
    }

  }

  /* todo: read individual pages, split on end of sentence.
  * Senatnce starts with Capital letter, is preceded by '.'
  * regexp: /^(.*?)[.?!]\s/
  * */
  def getTextFromPdf(startPage: Int, endPage: Int, filename: String): Option[String] = {

  }
  /*If first sentence is open in previous page then it is ignored. It last sentence end on page2 then it it taken.
  returns first sentence to last sentence.
  Page1/s1.s2..Page2/...s3.s4.s5..Page3..s6.s7/
  */
  def getTextPageToSentence(page1: String, page2: String): String={
    return ""
  }
}
