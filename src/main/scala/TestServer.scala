package esbook10

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by chrisrozacki on 16/07/15.
 */
object TestServer{
  var RequestId = 0
  //throws exception or returns Future
  def write(data:String):Try[Future[String]]={
    Try{
      //here you can throw connection exception
      Future{
        "error code returned from server "+RequestId.toString
      }
    }
  }
}
