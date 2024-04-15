## Prebuild
install graphviz maven openjdk-11-jdk 
## Build
Build a self-contained jar file using the following command:
`mvn package shade:shade`

## Usage
`java -jar SVCSLearner-0.0.1-SNAPSHOT.jar <configuration file>`
