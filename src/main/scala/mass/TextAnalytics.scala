package mass

import java.util

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage
import com.ibm.watson.developer_cloud.alchemy.v1.model.LanguageSelection
import mass.TextAnalytics.TextAnalyticsConfig

import scala.collection.JavaConversions._
import scala.util.Try

class TextAnalytics(textAnalyticsConf: TextAnalyticsConfig) extends Actor with ActorLogging {
  val service = new AlchemyLanguage()
  service.setLanguage(LanguageSelection.ENGLISH)
  service.setApiKey(textAnalyticsConf.apikey)

  val summarizer = context.actorSelection("../../summarizer")

  def receive = {
    case sentence: Sentence =>
      val params = new util.HashMap[String, Object]()
      params.put(AlchemyLanguage.TEXT, sentence.text)

      val sentiment = service.getSentiment(params).execute()
      val sentimentSummary = SentenceSentiment(
        sentiment.getSentiment.getType.name(),
        Try(sentiment.getSentiment.getScore.doubleValue).getOrElse(0.0d)
      )

      val entities = service.getEntities(params).execute()
      val entitySummary: List[SentenceEntity] = entities.getEntities.toList.map(
        entity =>
          SentenceEntity(
            entity.getText,
            entity.getType,
            Try(entity.getDisambiguated.getSubType.toList).getOrElse(List.empty)
          )
      )

      val relations = service.getTypedRelations(params).execute()
      val relationSummary = relations.getTypedRelations.toList.map(
        relation => SentenceRelation(relation.getType, relation.getScore)
      )

      summarizer ! SentenceERs(sentence.sentenceId, entitySummary, relationSummary, sentimentSummary)
  }
}

object TextAnalytics {

  case class TextAnalyticsConfig(apikey: String)

  def props(textAnalyticsConf: TextAnalyticsConfig): Props = Props(new TextAnalytics(textAnalyticsConf))
}
