package mass

import akka.actor.{Actor, ActorLogging, Props}
import mass.MeetingAssistant.MeetingAssistantConfig
import mass.NLClassifier.NLClassifierConfig
import mass.TextAnalytics.TextAnalyticsConfig
import mass.ToneAnalytics.ToneAnalyticsConfig

class MeetingAssistant(meetingAssistantConfig: MeetingAssistantConfig) extends Actor with ActorLogging {
  val toneAnalyzer = context.actorOf(ToneAnalytics.props(meetingAssistantConfig.toneAnalyticsConf), "toneAnalyzer")
  val textAnalyzer = context.actorOf(TextAnalytics.props(meetingAssistantConfig.textAnalyticsConf), "textAnalyzer")
  val nlClassifier = context.actorOf(NLClassifier.props(meetingAssistantConfig.nlClassifierConf), "nlClassifier")
  val summarizer = context.actorSelection("../summarizer")

  def generateID = System.currentTimeMillis()

  def receive = {
    case text: String =>
      val sentence = Sentence(generateID, text)
      summarizer ! sentence
      toneAnalyzer ! sentence
      textAnalyzer ! sentence
      nlClassifier ! sentence
  }
}

object MeetingAssistant {

  case class MeetingAssistantConfig(toneAnalyticsConf: ToneAnalyticsConfig,
                                    textAnalyticsConf: TextAnalyticsConfig,
                                    nlClassifierConf: NLClassifierConfig)

  def props(meetingAssistantConfig: MeetingAssistantConfig): Props = Props(new MeetingAssistant(meetingAssistantConfig))
}