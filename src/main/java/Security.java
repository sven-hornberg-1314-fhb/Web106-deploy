import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by devnull on 22.01.14.
 */
public class Security {

    AWSCredentials awsCredentials;

    Security(AWSCredentials aws) {
        awsCredentials = aws;
    }

    public boolean checkSecurityGroupForMySQL(String groupName, boolean vpc) {

        Region region = Region.getRegion(Regions.EU_WEST_1);

        AmazonEC2 amazonEC2 = new AmazonEC2Client(awsCredentials);
        amazonEC2.setRegion(region);


        DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2.describeSecurityGroups();
        List<SecurityGroup> securityGroups = describeSecurityGroupsResult.getSecurityGroups();

        SecurityGroup web106ec2 = null;
        for (SecurityGroup securityGroup : securityGroups) {
            if (securityGroup.getGroupName().equals(groupName)) {
                web106ec2 = securityGroup;
                System.out.println("Securitygroup ok");
            }
        }

        if (web106ec2 == null) {

            DescribeVpcsResult describeVpcsResult = amazonEC2.describeVpcs();
            List<Vpc> vpcs = describeVpcsResult.getVpcs();
            if (vpcs.size() == 0 && vpc) {
                System.out.println("Please set at least one VPC before deploying");
                return false;
            } else {
                System.out.println("Taking VPC: " + vpcs.get(0).getVpcId());
                CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest();
                createSecurityGroupRequest.setGroupName(groupName);
                createSecurityGroupRequest.setDescription("deployed group");

                if(vpc) {
                    createSecurityGroupRequest.setVpcId(vpcs.get(0).getVpcId());
                }

                amazonEC2.createSecurityGroup(createSecurityGroupRequest);
                System.out.println("Securitygroup created");
            }

        }

        return true;
    }

    public boolean checkMySQLPortOnSecurityGroup(String groupName) {

        try {
            Region region = Region.getRegion(Regions.EU_WEST_1);

            AmazonEC2 amazonEC2 = new AmazonEC2Client(awsCredentials);
            amazonEC2.setRegion(region);

            DescribeSecurityGroupsResult describeSecurityGroupsResult = amazonEC2.describeSecurityGroups();
            List<SecurityGroup> securityGroups = describeSecurityGroupsResult.getSecurityGroups();

            SecurityGroup web106ec2 = null;
            for (SecurityGroup securityGroup : securityGroups) {
                if (securityGroup.getGroupName().equals(groupName)) {
                    web106ec2 = securityGroup;
                    System.out.println("Securitygroup ok");
                }
            }

            String ipAddr = "0.0.0.0/0";
            ArrayList<String> ipRanges = new ArrayList<String>();
            ipRanges.add(ipAddr);

            ArrayList<IpPermission> ipPermissions = new ArrayList<IpPermission>();
            IpPermission ipPermission = new IpPermission();
            ipPermission.setIpProtocol("tcp");
            ipPermission.setFromPort(new Integer(3306));
            ipPermission.setToPort(new Integer(3306));
            ipPermission.setIpRanges(ipRanges);
            ipPermissions.add(ipPermission);

            // Authorize the ports to the used.
            AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest()
                    .withGroupId(web106ec2.getGroupId()).withIpPermissions(ipPermissions);

            amazonEC2.authorizeSecurityGroupIngress(ingressRequest);
        } catch (AmazonServiceException aEx) {

            if (aEx.getErrorCode().contains("InvalidPermission.Duplicate")) {

                System.out.println("MySQL-Port: ok");
            } else {
                System.out.println(aEx.getMessage());
                return false;
            }
        } catch (AmazonClientException aEx) {
            System.out.println(aEx.getMessage());
            return false;
        }
        return true;
    }
}
