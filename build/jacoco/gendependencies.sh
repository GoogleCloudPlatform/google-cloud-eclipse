#!/bin/sh
# Use xmlstarlet to generate the list of compile and test dependencies
# This script will need updating if we go pomless.

xml sel -I -N 'mvn=http://maven.apache.org/POM/4.0.0' -t \
  --if 'not(/mvn:project/mvn:artifactId = "com.google.cloud.tools.eclipse.test.dependencies")' \
    --elem dependency \
      --elem groupId \
          --if '/mvn:project/mvn:groupId' \
              -v '/mvn:project/mvn:groupId' \
          --else \
              -v '/mvn:project/mvn:parent/mvn:groupId' \
          --break \
      --break \
      --elem artifactId \
          -v '/mvn:project/mvn:artifactId' \
          --break \
      --elem version \
          -v '/mvn:project/mvn:version' \
          --break \
      --elem scope \
          --if '/mvn:project/mvn:packaging = "eclipse-plugin"' \
            -o 'compile' \
          --elif '/mvn:project/mvn:packaging = "eclipse-test-plugin"' \
            -o 'test' \
          --else \
            -o 'unknown' \
          --break \
          --break \
  --break \
../../plugins/*/pom.xml \
| sed 's;dependency xmlns="http://maven.apache.org/POM/4.0.0";dependency;'
