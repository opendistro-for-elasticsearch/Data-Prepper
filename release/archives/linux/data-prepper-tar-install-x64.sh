#!/bin/bash

# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
if [[ $# -ne 2 ]]
  then
    echo
    echo "Error: Paths to pipeline and data-prepper configuration files are required. Example:"
    echo "./data-prepper-tar-install.sh config/example-pipelines.yaml config/example-data-prepper-config.yaml"
    echo
    exit 1
fi

PIPELINES_FILE_LOCATION=$1
CONFIG_FILE_LOCATION=$2
MIN_REQ_JAVA_VERSION=8
MIN_REQ_OPENJDK_VERSION=8
DATA_PREPPER_HOME=$(dirname $(realpath $0))
EXECUTABLE_JAR=$(ls -1 $DATA_PREPPER_HOME/bin/*.jar 2>/dev/null)

if [[ -z "$EXECUTABLE_JAR" ]]
then
  echo "Jar file is missing from directory $DATA_PREPPER_HOME/bin"
  exit 1
fi

#check if java is installed
if type -p java; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    _java="$JAVA_HOME/bin/java"
else
    echo "java is required for executing data prepper, consider downloading data prepper tar with jdk"
    exit 1
fi

if [[ "$_java" ]]
then
    java_type=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $1}')
    java_version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | sed '/^1\./s///' | cut -d'.' -f1)
    echo "Found $java_type of $java_version"
    if [[ $java_type == *"openjdk"* ]]
    then
        if (( $(echo "$java_version < $MIN_REQ_OPENJDK_VERSION" | bc -l) ))
        then
            echo "Minimum required for $java_type is $MIN_REQ_OPENJDK_VERSION"
            exit 1
        fi
    else
        if (( $(echo "$java_version < $MIN_REQ_JAVA_VERSION" | bc -l) ))
        then
            echo "Minimum required for $java_type is $MIN_REQ_JAVA_VERSION"
            exit 1
        fi
    fi
fi

DATA_PREPPER_JAVA_OPTS="-Dlog4j.configurationFile=$DATA_PREPPER_HOME/config/log4j2-rolling.properties"
java $JAVA_OPTS $DATA_PREPPER_JAVA_OPTS -jar $EXECUTABLE_JAR $PIPELINES_FILE_LOCATION $CONFIG_FILE_LOCATION