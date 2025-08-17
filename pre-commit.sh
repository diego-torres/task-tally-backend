#!/bin/sh
# Pre-commit git hook to run spotless:check

MVN_CMD="./mvnw"
if [ ! -x "$MVN_CMD" ]; then
  MVN_CMD="mvn"
fi

$MVN_CMD spotless:check
RESULT=$?
if [ $RESULT -ne 0 ]; then
  echo "\nspotless:check failed. Please fix formatting before committing."
  exit 1
fi

exit 0
