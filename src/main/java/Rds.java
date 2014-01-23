import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by devnull on 22.01.14.
 */
public class Rds {

    AWSCredentials awsCredentials;

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

    public boolean createRdsDatabase(String databaseName, String userName, String userPassword, int Gb, String securityGroupName, String instanceClass) {

        try {

            Region region = Region.getRegion(Regions.EU_WEST_1);

            AmazonRDS amazonRDSClient = new AmazonRDSClient(awsCredentials);
            amazonRDSClient.setRegion(region);

            AmazonEC2 amazonEC2 = new AmazonEC2Client(awsCredentials);
            amazonEC2.setRegion(region);


            DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2.describeSecurityGroups();
            List<SecurityGroup> securityGroups = describeSecurityGroupsResult.getSecurityGroups();

            SecurityGroup web106ec2 = null;
            for (SecurityGroup securityGroup : securityGroups) {
                if (securityGroup.getGroupName().equals("web106ec2")) {
                    web106ec2 = securityGroup;
                }
            }

            if (web106ec2 != null) {

                List<String> ids = new ArrayList<String>();

                ids.add(web106ec2.getGroupId());

                CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest();
                createDBInstanceRequest
                        .withDBName("ebdb")
                        .withAllocatedStorage(5)
                        .withDBInstanceClass("db.t1.micro")
                        .withEngine("mysql")
                        .withMasterUsername("web106db")
                        .withMasterUserPassword("web106db")
                        .withDBInstanceIdentifier("ebdb")
                        .withMultiAZ(false)
                        .withVpcSecurityGroupIds(ids);

                amazonRDSClient.createDBInstance(createDBInstanceRequest);
                System.out.println("creating DB");
            } else {
                System.out.println("cant find securitygroup");
            }
        } catch (Exception ex) {

            System.out.println(ex.getMessage());

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
