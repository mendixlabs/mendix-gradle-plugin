#!/bin/sh
###############################################################################
#  Mendix startup script for Linux/Mac.
#
#  Usage:
#    start.sh [CONFIG]
#
#    CONFIG      Optional path to config file. Defaults to etc/app.conf.
#
#  The following environment variables can be set
#    JAVA_HOME   Path to the JRE installation
#    JAVA_OPTS   Specify Java command line option
#
###############################################################################

SCRIPT_DIR=$(dirname $0)
BASE_DIR=$(realpath ${SCRIPT_DIR}/..)
RUTNIIME_DIR=${BASE_DIR}/lib

# Process config param
CONFIG=${BASE_DIR}/etc/Default.conf
if [ "$1" != "" ]; then
    CONFIG=$1
fi

# check java
JAVA=java
if [ ! -z "${JAVA_HOME}" ]; then
  JAVA=${JAVA_HOME}/bin/java
fi
$JAVA -version>/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "java can't be found"
  exit 1
fi

${JAVA} ${JAVA_OPTS} -DMX_INSTALL_PATH=${RUTNIIME_DIR} -jar ${RUTNIIME_DIR}/runtime/launcher/runtimelauncher.jar ${BASE_DIR}/app ${CONFIG}