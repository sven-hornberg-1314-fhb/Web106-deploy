import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by devnull on 22.01.14.
 */
public class Beanstalk {

    AWSCredentials awsCredentials;

    Beanstalk(AWSCredentials aws) {
        awsCredentials = aws;
    }


    public boolean createApplication(String applicationName) {
        AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(awsCredentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        awsElasticBeanstalk.setRegion(region);

        DescribeApplicationsResult describeApplicationsResult = awsElasticBeanstalk.describeApplications();
        List<ApplicationDescription> applicationDescriptionList = describeApplicationsResult.getApplications();

        boolean alreadyExists = false;
        for (ApplicationDescription ad : applicationDescriptionList) {
            if (ad.getApplicationName().equals(applicationName)) {
                alreadyExists = true;
            }
        }
        if (!alreadyExists) {

            System.out.println("creating Application");
            CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
            createApplicationRequest.setApplicationName(applicationName);
            createApplicationRequest.setDescription("deployed application");
            awsElasticBeanstalk.createApplication(createApplicationRequest);
        } else {
            System.out.println("application already exists");
        }

        return true;
    }

    public boolean createApplicationVersion(String applicationName, String versionLabel, String bucketName, String bucketKey, boolean autoCreateApplication) {

        AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(awsCredentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        awsElasticBeanstalk.setRegion(region);

        DescribeApplicationVersionsResult describeApplicationVersionsResult = awsElasticBeanstalk.describeApplicationVersions();
        List<ApplicationVersionDescription> applicationVersionDescriptions = describeApplicationVersionsResult.getApplicationVersions();

        boolean alreadyExists = false;
        if (applicationVersionDescriptions.size() > 0) {

            for (ApplicationVersionDescription avd : applicationVersionDescriptions) {
                if (avd.getVersionLabel().equals(versionLabel)) {
                    alreadyExists = true;
                }
            }
        }

        if (alreadyExists) {
            System.out.println("applicationversion already exists");
        } else {

            S3Location s3Location = new S3Location();
            s3Location.setS3Bucket(bucketName);
            s3Location.setS3Key(bucketKey);

            CreateApplicationVersionRequest createApplicationVersionRequest = new CreateApplicationVersionRequest();
            createApplicationVersionRequest.withApplicationName(applicationName)
                    .withAutoCreateApplication(autoCreateApplication).withVersionLabel(versionLabel).withSourceBundle(s3Location);

            awsElasticBeanstalk.createApplicationVersion(createApplicationVersionRequest);

            System.out.println("created Applicationversion");
        }
        return true;
    }

    public boolean createSolutionTemplate(String applicationName, String templateName) {
        AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(awsCredentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        awsElasticBeanstalk.setRegion(region);

        try {
            CreateConfigurationTemplateRequest createConfigurationTemplateRequest = new CreateConfigurationTemplateRequest();
            createConfigurationTemplateRequest.setTemplateName(templateName);
            createConfigurationTemplateRequest.setApplicationName(applicationName);
            createConfigurationTemplateRequest.setDescription("deployed template");
            createConfigurationTemplateRequest.setSolutionStackName("64bit Amazon Linux running Tomcat 7");


            awsElasticBeanstalk.createConfigurationTemplate(createConfigurationTemplateRequest);

            System.out.println("Teamplate: " + templateName + " created");

        } catch (Exception ex) {
            if (ex.getMessage().contains("already exists")) {
                System.out.println("Teamplate: " + templateName + " already exists");
            }
        }
        return true;
    }

    public boolean checkDNSAvailability(String cname) {
        AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(awsCredentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        awsElasticBeanstalk.setRegion(region);

        CheckDNSAvailabilityRequest checkDNSAvailabilityRequest = new CheckDNSAvailabilityRequest();
        checkDNSAvailabilityRequest.setCNAMEPrefix(cname);
        CheckDNSAvailabilityResult checkDNSAvailabilityResult = awsElasticBeanstalk.checkDNSAvailability(checkDNSAvailabilityRequest);
        return checkDNSAvailabilityResult.isAvailable();
    }

    public boolean createEnvironment(String appliationName, String versionLabel, String templateName, String cnamePrefix, Deploy.settings setting,
                                     String awsAccessKey, String awsSecretKey, String JDBC_CONNECTION_STRING, String dbUser, String dbUserPassword) {
        AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(awsCredentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        awsElasticBeanstalk.setRegion(region);

        List<EnvironmentDescription> environmentDescriptions = awsElasticBeanstalk.describeEnvironments().getEnvironments();
        if (environmentDescriptions.size() > 0) {

        } else {
            CreateEnvironmentRequest createEnvironmentRequest = new CreateEnvironmentRequest();
            createEnvironmentRequest.setVersionLabel(versionLabel);
            createEnvironmentRequest.setTemplateName(templateName);
            createEnvironmentRequest.setApplicationName(appliationName);
            createEnvironmentRequest.setCNAMEPrefix(cnamePrefix);
            createEnvironmentRequest.setEnvironmentName(appliationName + "env");

            List<ConfigurationOptionSetting> configurationOptionSettings;
            if (setting.equals(Deploy.settings.LOWCOST)) {
                configurationOptionSettings = createLowcostSettings(awsAccessKey, awsSecretKey, JDBC_CONNECTION_STRING, dbUser, dbUserPassword);
            } else {
                configurationOptionSettings = createStandardSettings(awsAccessKey, awsSecretKey, JDBC_CONNECTION_STRING, dbUser, dbUserPassword);
            }
            createEnvironmentRequest.setOptionSettings(configurationOptionSettings);

            awsElasticBeanstalk.createEnvironment(createEnvironmentRequest);
        }

        return true;
    }

    public List<ConfigurationOptionSetting> createLowcostSettings(String awsAccessKey, String awsSecretKey, String JDBC_CONNECTION_STRING, String dbUser, String dbUserPassword) {
        List<ConfigurationOptionSetting> configurationOptionSettings = new ArrayList<ConfigurationOptionSetting>();
        ConfigurationOptionSetting configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("AWS_SECRET_KEY");
        configurationOptionSetting.setValue(awsSecretKey);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("AWS_ACCESS_KEY_ID");
        configurationOptionSetting.setValue(awsAccessKey);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("JDBC_CONNECTION_STRING");
        configurationOptionSetting.setValue(JDBC_CONNECTION_STRING);
        configurationOptionSettings.add(configurationOptionSetting);


        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("PARAM1");
        configurationOptionSetting.setValue(dbUser);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("PARAM2");
        configurationOptionSetting.setValue(dbUserPassword);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions");
        configurationOptionSetting.setOptionName("Xms");
        configurationOptionSetting.setValue("256m");
        configurationOptionSettings.add(configurationOptionSetting);


        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions");
        configurationOptionSetting.setOptionName("Xmx");
        configurationOptionSetting.setValue("256m");
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions");
        configurationOptionSetting.setOptionName("XX:MAXPermSize");
        configurationOptionSetting.setValue("512m");
        configurationOptionSettings.add(configurationOptionSetting);
        return configurationOptionSettings;
    }

    public List<ConfigurationOptionSetting> createStandardSettings(String awsAccessKey, String awsSecretKey, String JDBC_CONNECTION_STRING, String dbUser, String dbUserPassword) {
        List<ConfigurationOptionSetting> configurationOptionSettings = new ArrayList<ConfigurationOptionSetting>();
        ConfigurationOptionSetting configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("AWS_SECRET_KEY");
        configurationOptionSetting.setValue(awsSecretKey);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("AWS_ACCESS_KEY_ID");
        configurationOptionSetting.setValue(awsAccessKey);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("JDBC_CONNECTION_STRING");
        configurationOptionSetting.setValue(JDBC_CONNECTION_STRING);
        configurationOptionSettings.add(configurationOptionSetting);


        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("PARAM1");
        configurationOptionSetting.setValue(dbUser);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:application:environment");
        configurationOptionSetting.setOptionName("PARAM2");
        configurationOptionSetting.setValue(dbUserPassword);
        configurationOptionSettings.add(configurationOptionSetting);

        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions");
        configurationOptionSetting.setOptionName("Xms");
        configurationOptionSetting.setValue("512m");
        configurationOptionSettings.add(configurationOptionSetting);


        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions");
        configurationOptionSetting.setOptionName("Xmx");
        configurationOptionSetting.setValue("512m");
        configurationOptionSettings.add(configurationOptionSetting);


        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:elasticbeanstalk:container:tomcat:jvmoptions");
        configurationOptionSetting.setOptionName("XX:MAXPermSize");
        configurationOptionSetting.setValue("2048m");
        configurationOptionSettings.add(configurationOptionSetting);


        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:autoscaling:asg");
        configurationOptionSetting.setOptionName("MaxSize");
        configurationOptionSetting.setValue("10");
        configurationOptionSettings.add(configurationOptionSetting);


        configurationOptionSetting = new ConfigurationOptionSetting();
        configurationOptionSetting.setNamespace("aws:autoscaling:asg");
        configurationOptionSetting.setOptionName("InstanceType");
        configurationOptionSetting.setValue("m1.medium");
        configurationOptionSettings.add(configurationOptionSetting);

        return configurationOptionSettings;


    }

    public String ApplicationUrl(String versionLabel) {
        AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(awsCredentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        awsElasticBeanstalk.setRegion(region);

        String url = null;
        List<EnvironmentDescription> environmentDescriptions = awsElasticBeanstalk.describeEnvironments().getEnvironments();

        for (EnvironmentDescription environmentDescription : environmentDescriptions) {
            if (environmentDescription.getVersionLabel().equals(versionLabel)) {
                if (environmentDescription.getHealth().toLowerCase().equals("green")) {
                    url = environmentDescription.getEndpointURL();
                }
            }
        }
        return url;
    }


    public boolean deleteApplication(String applicationName) {
        AWSElasticBeanstalk awsElasticBeanstalk = new AWSElasticBeanstalkClient(awsCredentials);
        Region region = Region.getRegion(Regions.EU_WEST_1);
        awsElasticBeanstalk.setRegion(region);

            String tempName = applicationName+"env";
        boolean exists = false;
        List<EnvironmentDescription> environmentDescriptions = awsElasticBeanstalk.describeEnvironments().getEnvironments();

        for (EnvironmentDescription environmentDescription : environmentDescriptions) {
            if (tempName.equals(environmentDescription.getEnvironmentName())) {
                exists = true;
            }
        }
        if(exists) {

            TerminateEnvironmentRequest terminateEnvironmentRequest = new TerminateEnvironmentRequest();
            terminateEnvironmentRequest.setEnvironmentName(applicationName + "env");
            awsElasticBeanstalk.terminateEnvironment(terminateEnvironmentRequest);
            System.out.println("terminate " + applicationName);
        }
        return true;
    }
    /*
    bsContainer = "aws:elasticbeanstalk:application:environment"
    javaContainer = "aws:elasticbeanstalk:container:tomcat:jvmoptions"
    generalContainer = 'aws:autoscaling:asg'
    */

}