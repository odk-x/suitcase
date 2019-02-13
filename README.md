# suitcase

Building instructions:

1. Install Maven and Ant onto your system.
1. Run `ant` in the dependencies folder. 
1. From the root directory (with the pom.xml), run: *mvn clean package*

A new folder, target, will be created with the resulting jar file. 

By default, the pom.xml skips the tests.  

The tests can be run by passing the test arguments via the command line.  Replace the values in the example below with the appropriate server url, app id, etc. :

mvn clean package -DskipTests=false -Dtest.aggUrl=http://127.0.0.1 -Dtest.appId=default \
-Dtest.absolutePathOfTestFiles=testfiles<file.separator>  -Dtest.batchSize=1000 -Dtest.userName=<user> -Dtest.password=<password>