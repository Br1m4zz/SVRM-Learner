This is the repo of svrm-composer for docker build detail see: https://github.com/Br1m4zz/SVRM-CorpusGen
## Prebuild
install graphviz maven openjdk-11-jdk 
## Build
Build a self-contained jar file using the following command:
`mvn package shade:shade`

## Usage
`java -jar SVCSLearner-0.0.1-SNAPSHOT.jar <configuration file>`
