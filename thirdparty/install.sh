#!/bin/bash
cd "$(dirname "$0")"
mvn install:install-file -Dfile=question-gen-2.0-SNAPSHOT.jar -DgroupId=ru.compprehension.its -DartifactId=question-gen -Dversion=2.0-SNAPSHOT -Dpackaging=jar
