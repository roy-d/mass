package mass

import java.io.File

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier
import com.typesafe.config.ConfigFactory

object NLCTrainer extends App {
  val config = ConfigFactory.load()
  val service = new NaturalLanguageClassifier()
  service.setUsernameAndPassword(config.getString("mass.watson.nlc.username"), config.getString("mass.watson.nlc.password"))
  val id = service.createClassifier("meeting", "en", new File("/Users/droy/Dev/mass/src/main/resources/training/mass_nlc_train.csv")).execute().getId
  print(s"Classifier id: $id")
}
