package mass

import java.util.{Date, Properties}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.{Message, PasswordAuthentication, Session, Transport}

import akka.actor.{Actor, ActorLogging, Props}
import mass.Messenger.MessengerConfig

class Messenger(messengerConf: MessengerConfig) extends Actor with ActorLogging {

  val props = new Properties()
  props.put("mail.smtp.host", messengerConf.server)
  props.put("mail.smtp.port", messengerConf.port)

  val session = Session.getInstance(props, null)


  def receive = {
    case payload: String =>
      val msg = new MimeMessage(session)
      msg.addHeader("Content-type", "text/HTML; charset=UTF-8")
      msg.addHeader("format", "flowed")
      msg.addHeader("Content-Transfer-Encoding", "8bit")
      msg.setFrom(new InternetAddress(messengerConf.user, "Meeting Assistant"))

      msg.setSubject(s"Minutes of meeting: ${new Date()}", "UTF-8")

      msg.setText(payload, "UTF-8")

      msg.setSentDate(new Date())

      msg.addRecipient(Message.RecipientType.TO, new InternetAddress("debajyotiroy@kpmg.com"))
      log.info("Message is ready")
      Transport.send(msg)
      log.info("Message is sent")
  }
}

object Messenger {

  case class MessengerConfig(server: String, port: String, user: String)

  def props(messengerConf: MessengerConfig): Props = Props(new Messenger(messengerConf))
}
