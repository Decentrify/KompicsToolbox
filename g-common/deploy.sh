#!/bin/bash

mvn  deploy:deploy-file -Durl=scpexe://kompics.i.sics.se/home/maven/repository \
                       -DrepositoryId=sics-release-repository \
                       -Dfile=target/g-common-1.0-SNAPSHOT.jar \
                       -DgroupId=se.kth.co \
                       -DartifactId=global-common \
                       -Dversion=1.0-SNAPSHOT \
                       -Dpackaging=jar \
                       -DpomFile=./pom.xml \
                       -DgeneratePom.description="Common Stuff" 

