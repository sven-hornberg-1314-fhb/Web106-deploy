Web106-deploy
=============

Deploy [Web106](https://github.com/sven-hornberg-1314-fhb/Web106) with one single jar file to your elasticbeanstalk environment

## Instructions


First checkout and compile using following command:

    mvn clean compile assembly:single

## Usage

* Copy the <strong>created jar file and aws.properties</strong> into the Web106 directory
* Fill in your Amazonkeys and other parameters
* run 
     <strong>java -jar deploy.jar</strong> it will show you the supported commands

--- 
deploy a low cost environment

    java -jar deployedjar.jar -deploy:low


deploy a low standard environment

    java -jar deployedjar.jar -deploy:standard

terminate all

    java -jar deployedjar.jar -terminate

## Requirement to use deploying jar

* Java 1.7 with working classpath
* Maven with working classpath
* The grails wrapper should be able to perform <strong>grailsw prod war</strong>
