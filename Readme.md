# Meeting Assistant using IBM Watson
## (using Scala and Akka) 

##Watson APIs used
* Alchemy Language
* Tone Analyzer
* NLC
* Speech to text

CI: 
```sbt clean test```

Run twitter bot: 
```sbt "run-main mass.Server"```

Run speech bot: 
```sbt "run-main lab.NLCTrainer"```
