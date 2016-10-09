package mass

import javax.sound.sampled._

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.ibm.watson.developer_cloud.http.HttpMediaType
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.{RecognizeOptions, SpeechResults}
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback
import com.typesafe.config.ConfigFactory
import mass.MeetingAssistant.MeetingAssistantConfig
import mass.NLClassifier.NLClassifierConfig
import mass.Summarizer.SummarizerConfig
import mass.TextAnalytics.TextAnalyticsConfig
import mass.ToneAnalytics.ToneAnalyticsConfig

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import mass.Messenger.MessengerConfig

object Server extends App {
  implicit val timeout = Timeout(5.seconds)
  val config = ConfigFactory.load()
  val service = new SpeechToText()
  service.setUsernameAndPassword(
    config.getString("mass.watson.speech.username"),
    config.getString("mass.watson.speech.password")
  )

  val toneAnalyticsConfig = ToneAnalyticsConfig(
    config.getString("mass.watson.tone.username"),
    config.getString("mass.watson.tone.password"))
  val textAnalyticsConfig = TextAnalyticsConfig(
    config.getString("mass.watson.alchemy.apikey"))
  val nlClassifierConfig = NLClassifierConfig(
    config.getString("mass.watson.nlc.username"),
    config.getString("mass.watson.nlc.password"),
    Some(config.getString("mass.watson.nlc.classifierid")))

  val meetingAssistantConfig = MeetingAssistantConfig(toneAnalyticsConfig, textAnalyticsConfig, nlClassifierConfig)

  val system = ActorSystem("MeetingAssistantSystem")
  val meetingAssistant = system.actorOf(MeetingAssistant.props(meetingAssistantConfig), "meetingAssistant")
  val summarizer = system.actorOf(Summarizer.props(SummarizerConfig(System.currentTimeMillis(), System.currentTimeMillis())), "summarizer")
  val messenger = system.actorOf(Messenger.props(MessengerConfig(config.getString("mass.mail.server"), config.getString("mass.mail.port"), config.getString("mass.mail.user"))))

  val sampleRate = 16000
  val format = new AudioFormat(sampleRate, 16, 1, true, false)
  val info = new DataLine.Info(classOf[TargetDataLine], format)

  if (!AudioSystem.isLineSupported(info)) {
    println(s"Line not supported: $info")
    System.exit(0)
  }

  val line = AudioSystem.getLine(info).asInstanceOf[TargetDataLine]
  line.open(format)
  line.start()

  val audio = new AudioInputStream(line)

  val options =
    new RecognizeOptions.Builder().continuous(true).interimResults(true).timestamps(true).wordConfidence(true)
      .contentType(HttpMediaType.AUDIO_RAW + "; rate=" + sampleRate).build()

  def summarize(): Unit = {
    val summaryFuture = (summarizer ? "summarize").mapTo[MeetingMinutes]
    val summary = Await.result(summaryFuture, Duration.Inf)
    val body = s"MINUTES OF MEETING:\n" +
      s"-----------------------------------------------------\n" +
      s"Text:\n${summary.text}\n" +
      s"Entities:\n${summary.entities.asJson.spaces4}\n" +
      s"Accomplishments:\n${summary.accomplishments.asJson.spaces4}\n" +
      s"Issues:\n${summary.issues.asJson.spaces4}\n" +
      s"Action Items:\n${summary.actionItems.asJson.spaces4}\n" +
      s"Analytics:\n${summary.analytics.asJson.spaces4}\n" +
      s"-----------------------------------------------------\n"
    println(body)
    messenger ! body
  }

  service.recognizeUsingWebSocket(audio, options, new BaseRecognizeCallback() {
    override def onTranscription(speechResults: SpeechResults): Unit = {
      val text = speechResults.getResults.toList.filter(_.isFinal)
        .map(t => t.getAlternatives.toList.map(_.getTranscript).mkString(" "))
        .mkString(" ")

      if (text.toLowerCase contains "summarize") summarize()

      if (text.nonEmpty) meetingAssistant ! text
    }
  })

  val duration = config.getInt("mass.audio.duration")
  println(s"Listening for next $duration seconds")
  Thread.sleep(duration * 1000)
  line.stop()
  line.close()

  summarize()
  Thread.sleep(1000)
  println("FINISHED SESSION !!")
  System.exit(0)
}
