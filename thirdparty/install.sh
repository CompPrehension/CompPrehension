#!/bin/bash
cd "$(dirname "$0")"
curl -L -O https://github.com/CompPrehension/top-learning-generator/releases/download/v0.4.0/question-gen-2.0-SNAPSHOT.jar
mvn install:install-file -Dfile=question-gen-2.0-SNAPSHOT.jar -DgroupId=ru.compprehension.its -DartifactId=question-gen -Dversion=2.0-SNAPSHOT -Dpackaging=jar
