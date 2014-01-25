import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import org.joda.time.DateTime;

import java.io.*;
import java.util.Properties;


public class Deploy {

    private static String awsAccessKey = null;
    private static String awsSecretKey = null;
    private static boolean deploymentIsFine = true;
    private static String fileName = null;
    private static AWSCredentials awsCredentials;
    private static String groupName = null;

    private static String databaseName = null;
    private static String userName = null;
    private static String userPassword = null;
    private static int Gb = 5;
    private static String securityGroupName = null;
    private static String bucketName = null;
    private static String databaseEndpoint = "";
    private static String applicationName = null;
    private static String applicationLabel = null;
    private static String applicationTemaplate = null;

    private static boolean vpc = false;


    public enum settings {LOWCOST, STANDARD}

    private static settings setting;


    public static void main(String args[]) {

        Common common = new Common();

        if (1 != args.length) {
            System.out.println("Wrong parameter count");
            deploymentIsFine = false;
            common.displayHelp();
        }

        if (deploymentIsFine) {
            System.out.println("Start at " + DateTime.now().toLocalTime());


            if (args[0].contains("-deploy")) {

                if (args[0].contains(":low")) {
                    setting = settings.LOWCOST;
                } else {
                    setting = settings.STANDARD;
                }

                try {

                    //Start deployment steps
                    if (deploymentIsFine) {
                        deploymentIsFine = common.checkAwsProperties();
                        if (deploymentIsFine) {
                            deploymentIsFine = fillVaiablesFromProperties();
                            if(deploymentIsFine){
                                awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
                                deploymentIsFine= testAWSCredentials();
                            }
                        }
                    }


                    if (deploymentIsFine) {
                        deploymentIsFine = common.checkGrailsWrapper();
                        if (deploymentIsFine) {
                            common.createWarFile(fileName);
                        }
                    }

                    if (deploymentIsFine) {
                        deploymentIsFine = common.createAwsCredentialsFile(awsAccessKey, awsSecretKey);
                    }


                    if (deploymentIsFine) {
                        Beanstalk beanstalk = new Beanstalk(awsCredentials);
                        if (!beanstalk.checkDNSAvailability(applicationName)) {
                            System.out.println("Your Applicationname is not available, cname is already taken");
                            deploymentIsFine = false;
                        }
                    }


                    if (deploymentIsFine) {
                        Security security = new Security(awsCredentials);
                        deploymentIsFine = security.checkSecurityGroupForMySQL(groupName, vpc);
                        if (deploymentIsFine) {
                            deploymentIsFine = security.checkMySQLPortOnSecurityGroup(groupName);
                        }
                    }

                    if (deploymentIsFine) {
                        Rds rds = new Rds(awsCredentials);
                        if (rds.dbexists(databaseName)) {
                            System.out.println("DB already exists");
                        } else {
                            deploymentIsFine = rds.createRdsDatabase(databaseName, userName, userPassword, Gb, securityGroupName, setting, vpc);
                        }
                    }

                    if (deploymentIsFine) {

                        while(!common.WarFileIsReady(fileName)) {
                            System.out.println("waiting for warfile");
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }

                        if (common.checkWarFileCalcSize(fileName)) {
                            S3 s3 = new S3(awsCredentials);
                            deploymentIsFine = s3.checkAndCreateBucket(bucketName);
                            if (deploymentIsFine) {
                                File warFile = new File(fileName);

                                if (deploymentIsFine) {
                                    String MD5BucketFile = s3.MD5OfFileInBucket(bucketName, fileName);
                                    String MD5LocalFile = s3.MD5ofFile(warFile);

                                    if (null != MD5BucketFile && MD5BucketFile.equals(MD5LocalFile)) {
                                        System.out.println("skipping upload, file has the same hash");
                                    } else {
                                        deploymentIsFine = s3.uploadWarfile(bucketName, warFile);
                                    }
                                }
                            }
                        }
                    }
                 
                    common.deleteAwsCredentialsFile();

                    if (deploymentIsFine) {
                        Beanstalk beanstalk = new Beanstalk(awsCredentials);
                        beanstalk.createApplication(applicationName);
                        Rds rds = new Rds(awsCredentials);
                        while (null == rds.DatabaseEndpoint(databaseName)) {
                            System.out.println("waiting for database endpoint");
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }
                        databaseEndpoint = rds.DatabaseEndpoint(databaseName);
                        System.out.println("Databaseendpoint: " + databaseEndpoint);
                        beanstalk.createApplicationVersion(applicationName, applicationLabel, bucketName, fileName, true);
                        beanstalk.createSolutionTemplate(applicationName, applicationTemaplate);

                        String jdbc = "jdbc:mysql://" + databaseEndpoint + "/" + databaseName + "?user=" + userName + "&password=" + userPassword;
                        beanstalk.createEnvironment(applicationName, applicationLabel, applicationTemaplate, applicationName,
                                setting, awsAccessKey, awsSecretKey, jdbc, userName, userPassword);


                        //waiting for green
                        while (null == beanstalk.ApplicationUrl(applicationLabel)) {
                            System.out.println("waiting for application endpoint");
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }

                        System.out.println("Application ist fully deployed under : ");
                        System.out.println(beanstalk.ApplicationUrl(applicationLabel));

                    }

                } catch (Exception ex) {

                    System.out.println("unexpected error:");
                    System.out.println(ex.getMessage());
                }
            } else if (args[0].contains("-terminate")) {
                try {
                    deploymentIsFine = common.checkAwsProperties();
                    if (deploymentIsFine) {
                        deploymentIsFine =fillVaiablesFromProperties();
                        if (deploymentIsFine) {

                            deploymentIsFine =fillVaiablesFromProperties();

                            if(deploymentIsFine) {
                                awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
                                deploymentIsFine= testAWSCredentials();
                            }
                        }
                    }

                    if(deploymentIsFine) {

                        Beanstalk beanstalk = new Beanstalk(awsCredentials);
                        beanstalk.deleteApplication(applicationName);

                        Rds rds = new Rds(awsCredentials);
                        rds.deleteRds(databaseName);

                        System.out.println("Done");
                    }
                    } catch (Exception ex) {

                        System.out.println("unexpected error:");
                        System.out.println(ex.getMessage());


                    }
            } else {
                System.out.println("unknown parameter");
            }
        }
    }

    public static boolean fillVaiablesFromProperties() {

        boolean returnVal = true;

        try {

            Properties properties = new Properties();
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream("aws.properties"));
            properties.load(stream);
            stream.close();

            awsAccessKey = properties.getProperty("awsAccessKey").trim();
            awsSecretKey = properties.getProperty("awsSecretKey").trim();

            bucketName = properties.getProperty("bucketName").trim();
            fileName = properties.getProperty("fileName").trim();
            applicationName = properties.getProperty("applicationName").trim();
            applicationLabel = properties.getProperty("label").trim();

            userName = properties.getProperty("dbUser").trim();
            userPassword = properties.getProperty("dbUserPassword").trim();
            databaseName = properties.getProperty("dbName").trim();

            applicationTemaplate = properties.getProperty("templateName").trim();
            groupName =properties.getProperty("ec2SecurityGroup").trim();

            vpc = Boolean.parseBoolean(properties.getProperty("vpc").trim());


            if(awsAccessKey == null || awsSecretKey == null || bucketName == null
                    || fileName == null || applicationName == null || applicationLabel == null ||
                    userName == null || userPassword == null || databaseName == null ||
                    applicationTemaplate == null || groupName == null) {
                System.out.println("please check our aws.properties file , some information are missing");
                returnVal = false;
            }

        } catch (FileNotFoundException e) {
            System.out.println("cant find aws.properties file");
            returnVal = false;

        } catch (IOException e) {
            System.out.println("IO Error");
            returnVal = false;
        }
        return returnVal;
    }

    public static boolean testAWSCredentials() {

        try {
            Beanstalk beanstalk = new Beanstalk(awsCredentials);
            beanstalk.checkDNSAvailability("Test");

        }catch (Exception ex) {
            System.out.println("Your aws keys do not work");
            return false;
        }
        return  true;
    }

}
