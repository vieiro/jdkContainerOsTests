#!/bin/bash

function chooseAlgorithmConfigFile() {
  if [ "$#" -ne 3 ] ; then
    echo "Needs three arguments. First one should be the version of RHEL and second one should be 'true' or 'fips', if FIPS mode is on. Last one should be the java version."
    exit 1
  fi
  
  osPart="fedora"
  if [ "20" -ge "$1" ] ; then
    osPart="el$1"
    if [ "$1" -gt 10 ] ; then
      echo "Unknown/unsupported RHEL version was specified: $1"
      exit 1
    fi
  fi

  fipsPart="Legacy"
  if [ "$2" == "true" -o "$2" == "fips" ] ; then
    fipsPart="Fips"
  fi

  # so far only fips had differences with older jdks
  suffix=""
  if [ "$3" == "8" -o "$3" == "11" ] ; then
    suffix="OldJdks"
  fi

  echo "${osPart}Config${fipsPart}${suffix}.txt"

}

chooseAlgorithmConfigFile "${1}" "${2}" "${3}"
