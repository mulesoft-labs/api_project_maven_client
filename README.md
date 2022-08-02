# API Project Maven Polyglot

This project contains the maven polyglot extension and the maven packager to build an API project using maven.

As an example go to the api_example and just use 
`mvn clean install`

This will call our [polyglot](https://www.baeldung.com/maven-polyglot) extension `ExchangeModelProcessor.java` that translate the exchange.json into 
a maven model. This maven model, basically a pom.xml file, will contain the api packager plugin that generates the api artifacts, (raml, raml-artifact, oas) with the -full counterpart.

To run on debug mode, execute the maven project with `mvnDebug {goal}` where goal could be `compile`/`install` and run this project with a Remote JVM Debug configuration using port 8000 

To disable connector generation, add `-Dexchange.maven.disable.restConnect=true`