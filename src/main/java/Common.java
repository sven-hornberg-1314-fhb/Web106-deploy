import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * Created by devnull on 22.01.14.
 */
public class Common {

    public boolean deleteAwsCredentialsFile() {
        File file = new File("AwsCredentials.properties");
        if (file.exists()) {
            file.delete();
        }
        return true;
    }

    public void displayHelp() {
        System.out.println("example usage: ");
        System.out.println("-deploy:low for low cost applicationmode");
        System.out.println("-deploy:standard for normal more realistic applicationmode");
        System.out.println("-terminate");
    }

    public boolean createWarFile(String fileName) {
        String systemString = System.getProperty("os.name");

        System.out.println("creating war file");

        //URL location = Common.class.getProtectionDomain().getCodeSource().getLocation();
        //System.out.println(location.getFile());

        try {
            Runtime rt = Runtime.getRuntime();
            String command;
            if (systemString.toLowerCase().equals("windows")) {
                command = "grailsw.bat prod war " + fileName;
            } else {
                command = "./grailsw prod war " + fileName;
            }

            Process proc = rt.exec(command);
            proc.waitFor();
            System.out.println(fileName + " created");
        } catch (IOException ex) {
            System.out.println("cant execute grailwrapper");
            return false;
        } catch (InterruptedException e) {
            System.out.println("error during execution of grailwrapper");
            return false;
        }

        return true;
    }

    public boolean checkWarFileCalcSize(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            long fileSize = file.length();
            String fileSizeDisplay = FileUtils.byteCountToDisplaySize(fileSize);
            System.out.println("Filesize is about: " + fileSizeDisplay);

        } else {
            return false;
        }

        return true;
    }

    public boolean checkAwsProperties() {

        try {

            Properties properties = new Properties();
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream("aws.properties"));
            properties.load(stream);
            stream.close();
            System.out.println("checking aws.properties file");

        } catch (FileNotFoundException e) {
            System.out.println("cant find aws.properties file");
            return false;

        } catch (IOException e) {
            System.out.println("IO Error");
            return false;
        }
        return true;
    }

    public boolean checkGrailsWrapper() {

        File file = new File("grailsw");
        if (!file.exists()) {
            System.out.println("cant find grails wrapper");
            return false;

        } else {
            return true;
        }
    }

    public boolean createAwsCredentialsFile(String awsAccessKey, String awsSecretKey) {
        String fileName = "AwsCredentials.properties";
        String separator = System.getProperty("line.separator");

        File file = new File(fileName);
        if (file.exists()) {
            System.out.println("existing file updated");

        } else {
            System.out.println("file with credentials created");
        }
        String content = "accessKey = " + awsAccessKey + separator + "secretKey = " + awsSecretKey + separator;
        try {
            FileUtils.writeStringToFile(file, content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean WarFileIsReady(String fileName){
        File file = new File(fileName);
        return file.exists();
    }


}
