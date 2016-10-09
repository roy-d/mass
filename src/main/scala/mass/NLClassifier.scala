package mass

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier
import mass.NLClassifier.NLClassifierConfig

import scala.collection.JavaConversions._

class NLClassifier(nlClassifierConf: NLClassifierConfig) extends Actor with ActorLogging {
  val service = new NaturalLanguageClassifier()
  service.setUsernameAndPassword(nlClassifierConf.userName, nlClassifierConf.password)

  val summarizer = context.actorSelection("../../summarizer")

  def receive = {
    case sentence: Sentence =>
      val classifications = service.classify(nlClassifierConf.classifierId.get, sentence.text).execute()
      val topClassification = classifications.getClasses.toList.sortWith(_.getConfidence > _.getConfidence).head
      summarizer ! NLClassification(sentence.sentenceId, topClassification.getName, topClassification.getConfidence)
  }
}

object NLClassifier {

  case class NLClassifierConfig(userName: String, password: String, classifierId: Option[String])

  def props(nlClassifierConf: NLClassifierConfig): Props = Props(new NLClassifier(nlClassifierConf))
}
