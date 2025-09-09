#!/bin/bash

## resolve folder of this script, following all symlinks,
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
  SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
source ${SCRIPT_DIR}/get_system_info_script.sh

function chooseAlgorithmConfigFile() {
  if [ "$#" -lt 1 -o "$#" -gt 2 ] ; then
    echo "Two arguments. First one should be the information whether we need fips or non-fips listing, the other would be the version of OS we want to test, if not provided OS of current system will be extracted."
    exit 1
  fi

  if [[ "x${2}" != "x" ]] ; then
    OS_version_major="${2}"
  else
    OS_version_major="$(get_os_major_version)"
  fi

  osPart="fedora"
  if [ "20" -ge "$OS_version_major" ] ; then
    osPart="el$OS_version_major"
    if [ "$OS_version_major" -gt 10 ] ; then
      echo "Unknown/unsupported RHEL version was specified: $OS_version_major"
      exit 1
    fi
  fi

  if [[ "x${1}" != "x" ]] ; then
    fips_enabled="${1}"
  else
    fips_enabled="$(get_fips_status)"
  fi
  
  fipsPart="legacy"
  if [ "$fips_enabled" == "true" -o "$fips_enabled" == "fips" ] ; then
    fipsPart="fips"
  fi

  jdk_version="$(get_jdk_major_version)"
  suffix="ojdkX"
  if [ "$jdk_version" == "8" -o "$jdk_version" == "11" ] ; then
    suffix="ojdk${jdk_version}"
  fi

  echo "${osPart}-${fipsPart}-${suffix}.cfg"

}

chooseAlgorithmConfigFile "${1}" "${2}"
