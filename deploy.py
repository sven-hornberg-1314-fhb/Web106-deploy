import os,boto, boto.rds,time,datetime
from boto.s3.connection import S3Connection
from boto.s3.key import Key

class Deploy:
    awsAccessKey = ""
    awsSecretKey = ""
    bucketName = "web106eb"
    fileName = "web106.war"
    dbUser = "web106db"
    dbUserPassword = "web106db"
    dbEndpoint = None
    bucketKey = None

    def __init__(self):
        date = datetime.datetime.now()
        print('starting deployment at '+str(date.hour)+":"+str(date.minute))
        self.changeProjectDir()
        
    
        #self.createWarFile()
        #self.uploadWarFile()
        self.createDbRds()
        print self.dbEndpoint
        print self.bucketKey
    
    def checkoutProjectFromGit(self):
        print "checkout"
    
    def changeProjectDir(self):
        os.chdir('../Web106')
        print(os.getcwd())
    
    def createWarFile(self):
        print 'Try to create war file...'
        os.system("./grailsw prod war " + str(self.fileName))
        
    def uploadWarFile(self):
        print "start uploading war file"
        connS3 = S3Connection(self.awsAccessKey, self.awsSecretKey) 
        nonExistend = connS3.lookup(self.bucketName)
        bucket = None
        
        if nonExistend is None:
            print "start creating bucket"
            bucket = connS3.create_bucket(self.bucketName)
            print "done creating bucket"
        else:
            bucket = connS3.get_bucket(self.bucketName)
            print "bucket already exists"
            
        
        k = Key(bucket)
        k.key = self.fileName
        print "uploading..."
        k.set_contents_from_filename(self.fileName)
        
        self.bucketKey = k.key
        
        print "done uploading war file"
        
    def createDbRds(self):
        connRds = boto.rds.connect_to_region(
            "eu-west-1",
            aws_access_key_id=self.awsAccessKey, 
            aws_secret_access_key=self.awsSecretKey
        )
        db = None
    
        check = connRds.get_all_dbinstances("dbeb")
        if(len(check) == 0):
            db = connRds.create_dbinstance("dbeb",5,"db.t1.micro",self.dbUser,self.dbUserPassword) 
    
        instances = connRds.get_all_dbinstances("dbeb")
        db = instances[0]

        while db.endpoint == None:
            time.sleep(5)
            date = datetime.datetime.now()
            print "waiting for endpoint "+str(date.hour)+":"+str(date.minute)
         
        self.dbEndpoint = db.endpoint[0]




if __name__ == "__main__":
    d = Deploy()
    













