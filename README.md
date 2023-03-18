# ODK-X Suitcase

This project is __*actively maintained*__

The developer [wiki](https://github.com/odk-x/tool-suite-X/wiki) (including release notes) and [issues tracker](https://github.com/odk-x/tool-suite-X/issues) are located under the [**ODK-X Tool Suite**](https://github.com/odk-x) project.

## Setting up Your Environment

1. Install [Maven](https://maven.apache.org/download.cgi) and [Ant](https://ant.apache.org/bindownload.cgi) onto your system. 
2. Run `ant` in the dependencies folder. 
3. From the root directory (with the pom.xml) run: `mvn clean package`

A new folder, target, will be created with the resulting jar file. 

By default, the pom.xml skips the tests.  

The tests can be run by passing the test arguments via the command line.  Replace the values in the example below with the appropriate server url, app id, etc. :

mvn clean package -DskipTests=false -Dtest.aggUrl=http://127.0.0.1 -Dtest.appId=default \
-Dtest.absolutePathOfTestFiles=testfiles<file.separator>  -Dtest.batchSize=1000 -Dtest.userName=<user> -Dtest.password=<password>

## How to contribute
If you’re new to ODK-X you can check out the documentation:
- [https://docs.odk-x.org](https://docs.odk-x.org)

Once you’re up and running, you can choose an issue to start working on from here: 
- [https://github.com/odk-x/tool-suite-X/issues](https://github.com/odk-x/tool-suite-X/issues)

Issues tagged as [good first issue](https://github.com/odk-x/tool-suite-X/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) should be a good place to start.

Pull requests are welcome, though please submit them against the development branch. We prefer verbose descriptions of the change you are submitting. If you are fixing a bug please provide steps to reproduce it or a link to a an issue that provides that information. If you are submitting a new feature please provide a description of the need or a link to a forum discussion about it. 

## Links for users
This document is aimed at helping developers and technical contributors. For information on how to get started as a user of ODK-X, see our [online documentation](https://docs.odk-x.org), or to learn more about the Open Data Kit project, visit [https://odk-x.org](https://odk-x.org).
