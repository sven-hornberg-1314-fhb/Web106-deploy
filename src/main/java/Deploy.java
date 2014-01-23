
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import org.joda.time.DateTime;

import java.io.File;


public class Deploy {

    private static String awsAccessKey = "";
    private static String awsSecretKey = "";
    private static boolean deploymentIsFine = true;
    private static String fileName = "web106.war";
    private static AWSCredentials awsCredentials;
    private static String groupName = "web106beanstalk";

    private static String databaseName = "ebdb";
    private static String userName = "web106db";
    private static String userPassword = "web106db";
    private static int Gb = 5;
    private static String securityGroupName = "web106beanstalk";
    private static String instanceClass = "db.t1.micro";
    private static String bucketName = "web106eb";
    private static String databaseEndpoint = "";
    private static String applicationName = "web106";
    private static String applicationLabel = "Web106prod";
    private static String applicationTemaplate = "TWeb106";

    public enum settings {LOWCOST, STANDARD}

    private static settings setting;

    /**
     * -mod lowcost , -mod bench
     *
     * @param args
     */
    public static void main(String args[]) {

        Common common = new Common();

        if (1 != args.length) {
            System.out.println("Wrong parameter count");
            deploymentIsFine = false;
            common.displayHelp();
        }

        if (deploymentIsFine) {
            System.out.println("Begin deploy at " + DateTime.now().toLocalTime());


            if (args[0].contains("-deploy")) {

                if (args[0].contains(":low")) {
                    setting = settings.LOWCOST;
                } else {
                    setting = settings.STANDARD;
                }

                try {
                    //Start deployment steps
                    deploymentIsFine = false;
                    if (deploymentIsFine) {
                        deploymentIsFine = common.checkAwsProperties();
                        if (deploymentIsFine) {
                            common.fillVaiablesFromProperties();
                            awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
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

                    common.deleteAwsCredentialsFile();

                    if (deploymentIsFine) {
                        Beanstalk beanstalk = new Beanstalk(awsCredentials);
                        if (!beanstalk.checkDNSAvailability(applicationName)) {
                            System.out.println("Your Applicationname is not available, cname is already taken");
                            deploymentIsFine = false;
                        }
                    }

                    //todo delete
                    awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

                    deploymentIsFine = true;
                    if (deploymentIsFine) {
                        Security security = new Security(awsCredentials);
                        deploymentIsFine = security.checkSecurityGroupForMySQL(groupName);
                        if (deploymentIsFine) {
                            deploymentIsFine = security.checkMySQLPortOnSecurityGroup(groupName);
                        }
                    }

                    if (deploymentIsFine) {
                        Rds rds = new Rds(awsCredentials);
                        if (rds.dbexists(databaseName)) {
                            System.out.println("DB already exists");
                        } else {
                            deploymentIsFine = rds.createRdsDatabase(databaseName, userName, userPassword, Gb, securityGroupName, instanceClass);
                        }
                    }

                    if (deploymentIsFine) {
                        if (common.checkWarFileCalcSize(fileName)) {
                            S3 s3 = new S3(awsCredentials);
                            deploymentIsFine = s3.checkAndCreateBucket(bucketName);
                            if (deploymentIsFine) {
                                File warFile = new File(fileName);

                                if (deploymentIsFine) {
                                    String MD5BucketFile = s3.MD5OfFileInBucket(bucketName, fileName);
                                    String MD5LocalFile = s3.MD5ofFile(warFile);

                                    if (MD5BucketFile.equals(MD5LocalFile)) {
                                        System.out.println("skipping upload, file has the same hash");
                                    } else {
                                        deploymentIsFine = s3.uploadWarfile(bucketName, warFile);
                                    }
                                }
                            }
                        }
                    }

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
                    awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);


                    Beanstalk beanstalk = new Beanstalk(awsCredentials);
                    beanstalk.deleteApplication(applicationName);

                    Rds rds = new Rds(awsCredentials);
                    rds.deleteRds(databaseName);
                } catch (Exception ex) {

                    System.out.println("unexpected error:");
                    System.out.println(ex.getMessage());


                }
            } else {
                System.out.println("unknown parameter");
            }
        }
    }


}
