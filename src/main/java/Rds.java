import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by devnull on 22.01.14.
 */
public class Rds {

    AWSCredentials awsCredentials;
    private static String instanceClassMicro = "db.t1.micro";//db.m1.medium;
    private static String instanceClassMedium ="db.m1.medium";

    public Rds(AWSCredentials aws) {
        awsCredentials = aws;
    }

    public boolean dbexists(String databaseName) {

        boolean returnVal = false;
        Region region = Region.getRegion(Regions.EU_WEST_1);

        AmazonRDS amazonRDSClient = new AmazonRDSClient(awsCredentials);
        amazonRDSClient.setRegion(region);

        DescribeDBInstancesResult describeDBInstancesResult = amazonRDSClient.describeDBInstances();
        List<DBInstance> dbInstances = describeDBInstancesResult.getDBInstances();
        if (dbInstances.size() == 0) {
            returnVal = false;
        } else {

            for (DBInstance dbInstance : dbInstances) {
                if (dbInstance.getDBName().equals(databaseName)) {
                    returnVal = true;
                    break;
                }
            }
        }
        return returnVal;
    }

    public boolean createRdsDatabase(String databaseName, String userName, String userPassword, int Gb, String securityGroupName, Deploy.settings setting, boolean vpc) {

        try {

            Region region = Region.getRegion(Regions.EU_WEST_1);

            AmazonRDS amazonRDSClient = new AmazonRDSClient(awsCredentials);
            amazonRDSClient.setRegion(region);

            AmazonEC2 amazonEC2 = new AmazonEC2Client(awsCredentials);
            amazonEC2.setRegion(region);

            if(vpc) {

                DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2.describeSecurityGroups();
                List<SecurityGroup> securityGroups = describeSecurityGroupsResult.getSecurityGroups();

                SecurityGroup web106ec2 = null;
                for (SecurityGroup securityGroup : securityGroups) {
                    if (securityGroup.getGroupName().equals(securityGroupName)) {
                        web106ec2 = securityGroup;
                    }
                }


                if (web106ec2 != null) {

                    List<String> ids = new ArrayList<String>();

                    ids.add(web106ec2.getGroupId());

                    CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest();
                    createDBInstanceRequest
                            .withDBName(databaseName)
                            .withAllocatedStorage(5)
                            .withEngine("mysql")
                            .withMasterUsername(userName)
                            .withMasterUserPassword(userPassword)
                            .withDBInstanceIdentifier(databaseName)
                            .withMultiAZ(false);

                        createDBInstanceRequest.withVpcSecurityGroupIds(ids);

                    if(setting.equals(Deploy.settings.LOWCOST)) {
                        createDBInstanceRequest.withDBInstanceClass(instanceClassMicro);
                    } else {
                        createDBInstanceRequest.withDBInstanceClass(instanceClassMedium);
                    }

                    amazonRDSClient.createDBInstance(createDBInstanceRequest);
                }
            } else {

                DBSecurityGroup group = null;
                List<DBSecurityGroup> dbSecurityGroups= amazonRDSClient.describeDBSecurityGroups().getDBSecurityGroups();
                for (DBSecurityGroup securityGroup : dbSecurityGroups) {
                    if (securityGroup.getDBSecurityGroupName().equals(securityGroupName)) {
                        group = securityGroup;
                    }
                }

                if(group != null) {

                    List<String> ids = new ArrayList<String>();

                    ids.add(group.getDBSecurityGroupName());

                    CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest();
                    createDBInstanceRequest
                            .withDBName(databaseName)
                            .withAllocatedStorage(5)
                            .withEngine("mysql")
                            .withMasterUsername(userName)
                            .withMasterUserPassword(userPassword)
                            .withDBInstanceIdentifier(databaseName)
                            .withMultiAZ(false);



                    createDBInstanceRequest.setDBSecurityGroups(ids);

                    if(setting.equals(Deploy.settings.LOWCOST)) {
                        createDBInstanceRequest.withDBInstanceClass(instanceClassMicro);
                    } else {
                        createDBInstanceRequest.withDBInstanceClass(instanceClassMedium);
                    }

                    amazonRDSClient.createDBInstance(createDBInstanceRequest);

                }

            }
            System.out.println("creating DB");

        } catch (Exception ex) {

            if(ex.getMessage().toLowerCase().contains("dbinstancealreadyexists")) {
                System.out.println("DBInstance already exists");
            } else if(ex.getMessage().toLowerCase().contains("groupnotfound")) {
                System.out.println("Group not found");
                return false;
            } else {
                System.out.println(ex.getMessage());
            }

        }
        return true;
    }

    public String DatabaseEndpoint(String databaseName) {
        String returnVal = null;
        Region region = Region.getRegion(Regions.EU_WEST_1);

        AmazonRDS amazonRDSClient = new AmazonRDSClient(awsCredentials);
        amazonRDSClient.setRegion(region);

        DescribeDBInstancesResult describeDBInstancesResult = amazonRDSClient.describeDBInstances();
        List<DBInstance> dbInstances = describeDBInstancesResult.getDBInstances();
        for (DBInstance dbInstance : dbInstances) {
            if (dbInstance.getDBName().equals(databaseName) && dbInstance.getEndpoint() != null) {
                returnVal = dbInstance.getEndpoint().getAddress() + ":" + dbInstance.getEndpoint().getPort();
            }
        }
        return returnVal;
    }

    public boolean deleteRds(String databaseName) {

        if(dbexists(databaseName)) {

            Region region = Region.getRegion(Regions.EU_WEST_1);

            AmazonRDS amazonRDSClient = new AmazonRDSClient(awsCredentials);
            amazonRDSClient.setRegion(region);


            DeleteDBInstanceRequest deleteDBInstanceRequest = new DeleteDBInstanceRequest();
            deleteDBInstanceRequest.setDBInstanceIdentifier(databaseName);
            deleteDBInstanceRequest.setSkipFinalSnapshot(true);

            amazonRDSClient.deleteDBInstance(deleteDBInstanceRequest);
            System.out.println("deleting database");
        }
        return true;

    }
}
