# Meeting Assistant using IBM Watson
## (using Scala, Akka, Circe) 

##Watson APIs used
* Alchemy Language
* Tone Analyzer
* NLC
* Speech to text

CI: 
```sbt clean test```

Run Server: 
```sbt "run-main mass.Server"```

Run NLC Trainer: 
```sbt "run-main mass.NLCTrainer"```
