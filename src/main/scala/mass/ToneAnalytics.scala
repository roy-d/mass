package mass

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneScore
import mass.ToneAnalytics.ToneAnalyticsConfig

import scala.collection.JavaConversions._
import scala.util.Try

class ToneAnalytics(toneAnalyticsConf: ToneAnalyticsConfig) extends Actor with ActorLogging {
  val service = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19)
  service.setUsernameAndPassword(toneAnalyticsConf.userName, toneAnalyticsConf.password)

  val summarizer = context.actorSelection("../../summarizer")
  def receive = {
    case sentence: Sentence =>
      val tone = service.getTone(sentence.text, null).execute()

      log.debug(tone.toString)

      val maxTones: Map[String, ToneScore] = tone.getDocumentTone.getTones.toList
        .map(category => (category.getName.replace(" Tone", ""), category.getTones.toList.sortWith(_.getScore > _.getScore).head))
        .filter { case (_, v) => v.getScore > 0 }
        .toMap

      summarizer ! SentenceTones(sentence.sentenceId,
        Try(maxTones("Emotion").getName).getOrElse(""),
        Try[Double](maxTones("Emotion").getScore).getOrElse(0.0),
        Try(maxTones("Social").getName).getOrElse(""),
        Try[Double](maxTones("Social").getScore).getOrElse(0.0)
      )
  }
}

object ToneAnalytics {

  case class ToneAnalyticsConfig(userName: String, password: String)

  def props(tonerConf: ToneAnalyticsConfig): Props = Props(new ToneAnalytics(tonerConf))
}
