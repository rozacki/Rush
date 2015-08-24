package esbook10

import scala.concurrent.Future
import scala.util.Try
import org.elasticsearch.action.index.IndexResponse

object TestServer{
  var RequestId = 0
  val IndexName = "scala_cookbook"
  //throws exception or returns Future
  def write(data:String):Try[Future[IndexResponse]]={
    Try{
      ElastSearchClient.Index(IndexName,data)
    }
  }
}
