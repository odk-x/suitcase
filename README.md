# suitcase

Building instructions (Linux and Mac):

1. Install maven onto your system.
1. Run the shell script *mvn_local_installs* in the dependencies folder. 
1. From the root directory (with the pom.xml), run: *mvn clean compile assembly:single*

A new folder, target, will be created with the resuling jar file. 
