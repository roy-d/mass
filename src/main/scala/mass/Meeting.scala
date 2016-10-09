package mass

case class MeetingMinutes(text: String,
                          actionItems: List[String],
                          issues: List[String],
                          accomplishments: List[String],
                          analytics: List[SentenceAnalytics])

case class SentenceAnalytics(sentence: Sentence,
                             nlc: Option[NLClassification],
                             tone: Option[SentenceTones],
                             er: Option[SentenceERs])

case class Sentence(sentenceId: Long,
                    text: String)

case class NLClassification(sentenceId: Long,
                            name: String,
                            confidence: Double)

case class SentenceTones(sentenceId: Long,
                         emotionTone: String,
                         emotionScore: Double,
                         socialTone: String,
                         socialScore: Double)

case class SentenceERs(sentenceId: Long,
                       entities: List[SentenceEntity],
                       relations: List[SentenceRelation],
                       sentiment: SentenceSentiment)

case class SentenceEntity(name: String,
                          entityType: String,
                          entitySubTypes: List[String])

case class SentenceRelation(name: String,
                            score: Double)

case class SentenceSentiment(name: String,
                             score: Double)
