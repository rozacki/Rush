package esbook10

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import scala.concurrent.Future
import org.elasticsearch.action.index.IndexResponse

object ElastSearchClient {
  val EndPoint                = "dockerhost"
  val EndPointPort            = 9300
  val Client                  = ElasticClient.remote(EndPoint,EndPointPort)

  def Index(_index:String, field:String): Future[IndexResponse] ={
    //it will throw exception or return future
    Client execute {index into _index fields {"raw"->field}}
  }
}
