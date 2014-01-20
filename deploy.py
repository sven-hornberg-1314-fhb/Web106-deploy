import os,sys,boto
from boto.s3.connection import S3Connection
from boto.s3.key import Key

awsAccessKey = ""
awsSecretKey = ""
bucketName = "web106eb"
fileName = "web106.war"

def main():
    print('Los')
    changeProjectDir()

    #createWarFile()
    #uploadWarFile()
    createDbRds()

def checkoutProjectFromGit():
    print "checkout"

def changeProjectDir():
    os.chdir('../Web106')
    print(os.getcwd())

def createWarFile():
    print 'Try to create war file...'
    os.system("./grailsw prod war " + str(fileName))
    
def uploadWarFile():
    print "start uploading war file"
    connS3 = S3Connection(awsAccessKey, awsSecretKey) 
    nonExistend = connS3.lookup(bucketName)
    bucket = None
    
    if nonExistend is None:
        print "start creating bucket"
        bucket = connS3.create_bucket(bucketName)
        print "done creating bucket"
    else:
        bucket = connS3.get_bucket(bucketName)
        print "bucket already exists"
        
    
    k = Key(bucket)
    k.key = fileName
    print "uploading..."
    k.set_contents_from_filename(fileName)
    print "done uploading war file"
    
def createDbRds():
     = boto.rds.connect_to_region(
        "eu-west-1",
        awsAccessKey,
        awsSecretKey
    )
    connRds = boto.rds.connect_to_region()

    
    db = connRds.create_dbinstance("dbeb",5,"t1.micro","web106db","web106db")
    
    
        

if __name__ == "__main__":
    main()













