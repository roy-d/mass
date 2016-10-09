package mass

import akka.actor.{Actor, ActorLogging, Props}
import mass.Summarizer.SummarizerConfig

import scala.util.Try

class Summarizer(summarizerConf: SummarizerConfig) extends Actor with ActorLogging {

  val sentenceMap = scala.collection.mutable.HashMap.empty[Long, Sentence]
  val nlcMap = scala.collection.mutable.HashMap.empty[Long, NLClassification]
  val toneMap = scala.collection.mutable.HashMap.empty[Long, SentenceTones]
  val textMap = scala.collection.mutable.HashMap.empty[Long, SentenceERs]

  def receive = {
    case _: String =>
      val sentenceVals = sentenceMap.values.toList.sortWith(_.sentenceId < _.sentenceId)
      val text = sentenceVals.map(_.text).mkString(". ")
      val nlcVals = nlcMap.values.toList.sortWith(_.sentenceId < _.sentenceId).groupBy(_.name)

      val actionItems = Try(nlcVals("meeting_actionitem").map(nlc => sentenceMap(nlc.sentenceId).text)).getOrElse(List.empty)
      val issues = Try(nlcVals("meeting_issue").map(nlc => sentenceMap(nlc.sentenceId).text)).getOrElse(List.empty)
      val accomplishments = Try(nlcVals("meeting_accomplishment").map(nlc => sentenceMap(nlc.sentenceId).text)).getOrElse(List.empty)

      val analytics = sentenceVals.map {
        sentence =>
          val sentenceId = sentence.sentenceId
          SentenceAnalytics(sentence, nlcMap.get(sentenceId), toneMap.get(sentenceId), textMap.get(sentenceId))
      }

      sender() ! MeetingMinutes(text, actionItems, issues, accomplishments, analytics)

    case sentence: Sentence =>
      sentenceMap += (sentence.sentenceId -> sentence)

    case nlc: NLClassification =>
      nlcMap += (nlc.sentenceId -> nlc)

    case st: SentenceTones =>
      toneMap += (st.sentenceId -> st)

    case ser: SentenceERs =>
      textMap += (ser.sentenceId -> ser)
  }
}

object Summarizer {

  case class SummarizerConfig(meetingId: Long, startTime: Long)

  def props(summarizerConf: SummarizerConfig): Props = Props(new Summarizer(summarizerConf))
}