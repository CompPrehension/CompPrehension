#!/bin/bash
cd "$(dirname "$0")"
mvn install:install-file -Dfile=questionGen-1.0.jar -DgroupId=com.github.max_person.its -DartifactId=questionGen -Dversion=1.0 -Dpackaging=jar
