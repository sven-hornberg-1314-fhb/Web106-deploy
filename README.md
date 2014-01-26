Web106-deploy
=============

Deploy [Web106](https://github.com/sven-hornberg-1314-fhb/Web106) with one single jar file to your elasticbeanstalk environment

## Instructions

You have the choice between building that project bei your own, or download a generated jar from here [Download](https://github.com/sven-hornberg-1314-fhb/Web106-deploy/raw/master/download/deploy-1.0-jar-with-dependencies.jar)

If you choose to build it yourself first checkout this project and compile using following command:

    mvn clean compile assembly:single

## Usage

* Copy the <strong>deploy-1.0-jar-with-dependencies.jar created jar file and aws.properties</strong> into the existing Web106 directory
* Fill in your Amazonkeys and other parameters into the aws.properties file
* run 
     <strong>java -jar deploy-1.0-jar-with-dependencies.jar</strong> it will show you the supported commands

--- 
Here are some supported commands:


deploy a low cost environment

    java -jar deploy-1.0-jar-with-dependencies.jar -deploy:low


deploy a low standard environment

    java -jar deploy-1.0-jar-with-dependencies.jar -deploy:standard

terminate all

    java -jar deploy-1.0-jar-with-dependencies.jar -terminate

## Requirement to use deploying jar

* Java 1.7 with working classpath
* Maven with working classpath
* The grails wrapper should be able to perform <strong>grailsw prod war</strong> in your Web106 directory
