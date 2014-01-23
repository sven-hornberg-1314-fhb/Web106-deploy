import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Created by devnull on 22.01.14.
 */
public class S3 {
    AWSCredentials awsCredentials;

    S3(AWSCredentials aws) {
        awsCredentials = aws;
    }

    public boolean checkAndCreateBucket(String bucketName) {

        AmazonS3 s3client = new AmazonS3Client(awsCredentials);
        if (s3client.doesBucketExist(bucketName)) {

            List<Bucket> bucketList = s3client.listBuckets();
            boolean ownerOfBucket = false;
            for (Bucket bucket : bucketList) {
                if (bucket.getName().equals(bucketName)) {
                    ownerOfBucket = true;
                }
            }

            if (!ownerOfBucket) {
                System.out.println("you are not the owner of the given bucket");
                return false;
            }

        } else {
            s3client.createBucket(bucketName);
            System.out.println("create bucket");
        }

        return true;
    }

    public boolean uploadWarfile(String bucketName, File file) {

        try {

            System.out.println("begin upload");
            AmazonS3 s3client = new AmazonS3Client(awsCredentials);
            TransferManager tx = new TransferManager(s3client);

            String keyName = file.getName();

            PutObjectRequest por = new PutObjectRequest(bucketName, keyName, file);
            por.setCannedAcl(CannedAccessControlList.BucketOwnerFullControl);
            tx.getAmazonS3Client().putObject(por);
            System.out.println("warfile uploaded");

        } catch (AmazonServiceException aEx) {
            System.out.println("could not upload warfile");
            return false;

        } catch (AmazonClientException aEx) {
            System.out.println("could not upload warfile");
            return false;
        }
        return true;
    }

    String MD5OfFileInBucket(String bucketName, String fileName) {

        AmazonS3 s3client = new AmazonS3Client(awsCredentials);
        TransferManager tx = new TransferManager(s3client);
        String MD5 = null;

        ObjectListing objectListing = tx.getAmazonS3Client().listObjects(
                new ListObjectsRequest().withBucketName(bucketName));

        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {

            String keyName = objectSummary.getKey();
            if (keyName.equals(fileName)) {

                MD5 = objectSummary.getETag();
            }
        }
        return MD5;
    }

    String MD5ofFile(File file) {
        String md5 = null;
        try {

            FileInputStream fis = new FileInputStream(file);
            md5 = DigestUtils.md5Hex(IOUtils.toByteArray(fis));

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            String test = e.getMessage();
        }
        return md5;
    }
}
