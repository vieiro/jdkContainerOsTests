#!/bin/bash
set -ex
set -o pipefail

## resolve folder of this script, following all symlinks,
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  LIBCQA_SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
  SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$LIBCQA_SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly LIBCDQA_SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"

source $LIBCDQA_SCRIPT_DIR/../containersQa/testlib.bash




function dotnetVer() {
  runOnBaseDir  dotnet --version
}

function dotnetSdks() {
  runOnBaseDir  dotnet --list-sdks
}

function dotnetRuntimes() {
  runOnBaseDir  dotnet --list-runtimes
}

function getOldDotnetRegularTestsLog() {
  getOldLog 1100_dotnetRegularTests.sh
}

function getOldDotnetSecurityTestsLog() {
  getOldLog 1200_dotnetSecurityTests.sh
}

function dotnetRegularTestsResultsGathering() {
  getOldDotnetRegularTestsLog # to check if it is accessible
  local log=`getOldDotnetRegularTestsLog`
  dotnetTestsResultsGathering ${log}
}

function dotnetSecurityTestsResultsGathering() {
  getOldDotnetSecurityTestsLog # to check if it is accessible
  local log=`getOldDotnetSecurityTestsLog`
  dotnetTestsResultsGathering ${log}
}

function dotnetTestsResultsGathering() {
  local log=${1}
  getTestResultsName ${log} # to check if it is valid
  local origPath=`getTestResultsName ${log}`
  local name=$(basename $(dirname "$origPath"))
  local futurePath1="$REPORT_DIR/$name.jtr.xml"
  getTestResults ${log} > ${futurePath1}
  pushd $REPORT_DIR
    tar -czf "$name.tar.gz"  `basename "$futurePath1"`
  popd
}

ENDING_STRING="^############### ending "
STARTING_STRING="^############### starting "
ENDING_REGEX="$ENDING_STRING"
STARTING_REGEX="$STARTING_STRING"

function validateResultLines() {
  local log=${1}
  local ends=`cat $log | grep "$ENDING_REGEX"`
  local starts=`cat $log | grep "$STARTING_REGEX"`
  local ends_count=`echo "$ends" | wc -l`
  local starts_count=`echo "$starts" | wc -l`
  if [ $ends_count == 1 ] ; then 
    echo "ok, one end found: $ends" >&2
  else
   echo "bad, to much results ends found: $ends" >&2
   return 50
  fi
  if [ $starts_count == 1 ] ; then 
    echo "ok, one start found: $starts" >&2
  else
   echo "bad, to much results starts found: $starts" >&2
   return 52
  fi
}

function getTestResults() {
  local log=${1}
  validateResultLines ${log}
  cat ${log} | grep "$STARTING_REGEX"  -A 1000000 | grep "$ENDING_REGEX" -B 1000000 |  tail -n +2 | head -n -1
}

function getTestResultsName() {
  local log=${1}
  validateResultLines ${log}
  local n1=`cat $log | grep "$ENDING_REGEX" | sed "s/$ENDING_REGEX//g"`
  local n2=`cat $log | grep "$STARTING_REGEX" | sed "s/$STARTING_REGEX//g"` 
  if [ "x$n1" == "x$n2" ] ; then 
    echo $n1
  else
   echo "names difere: $n1 $n2" >&2
   return 51
  fi
}

function s2iDotNetHelloWorld() {
  OVERWRITE_USER="default"
  MAIN="Nothing=nowhere"
  ARGS="DOTNET_STARTUP_PROJECT=app/app.csproj"
  SETTING_ARGS=""
  REPO="https://github.com/redhat-developer/s2i-dotnetcore-ex.git" 
  NAME="dotnet_app"
  ADDS=""
  JENKINS_BUILD=""
  s2iLocal
}

