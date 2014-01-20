Web106-deploy
=============



mvn beanstalk:check-availability

mvn beanstalk:upload-source-bundle

mvn beanstalk:create-application-version beanstalk:create-environment

mvn beanstalk:wait-for-environment
