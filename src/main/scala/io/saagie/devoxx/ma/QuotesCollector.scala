package io.saagie.devoxx.ma


import java.util.Properties

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import scala.language.postfixOps

/**
  * Created by aurelien on 18/10/16.
  */
class QuotesCollector extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val URLBase = "http://download.finance.yahoo.com/d/quotes.csv?e=.csv&f="
  val URLEnd = "&s="
  val stockDayFormat = "lsnpd1oml1vq"

  val props = new Properties()

  val broker = "192.168.52.50:31200"
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
  val producer = new KafkaProducer[String, String](props)

  def mkUrl(symbols: List[String]) = URLBase + stockDayFormat + URLEnd + symbols.map(java.net.URLEncoder.encode(_,"UTF-8")).mkString("+")

  override def preStart() = {
  }

  def receive = {
    case symbols: List[_]                                 =>
      log.info("symbols received")
      http.singleRequest(HttpRequest(uri = mkUrl(symbols.map(_.toString)) )).pipeTo(self)
    case symbol: String                                   =>
      log.info("single symbol received")
      self ! List(symbol)
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      log.info("response ok from yahoo servers")
      pipe(entity.dataBytes.runFold(ByteString(""))(_ ++ _)) to self
    case HttpResponse(code, _, _, _)                      => log.info("Request failed, response code: " + code)
    case bs: ByteString                                   =>
      log.info("date received to be sent to kafka")
      val msgs: List[String] = bs.decodeString("utf-8").lines.toList
      msgs.foreach(log.info(_))
      msgs.foreach( l => producer.send(new ProducerRecord[String, String]("quotes", null,s"${System.currentTimeMillis},$l")) )
  }
}


object QuotesCollector extends App {

  import scala.concurrent.duration._
  import scala.language.postfixOps

  implicit val system = akka.actor.ActorSystem()
  import system.dispatcher

  val consideredSymbols = List(
    "DIA","AAPL","AXP","BA","CAT","CSCO","CVX","DD","DIS","GE","GS","HD","IBM","INTC","JNJ",
    "JPM","KO","MCD","MMM","MRK","MSFT","NKE","PFE","PG","TRV","UNH","UTX","VZ","WMT","XOM"
  )

  val streamActor = system.actorOf(Props(new QuotesCollector()))
  system.scheduler.schedule(0 milliseconds, 10 seconds, streamActor, consideredSymbols)
}
