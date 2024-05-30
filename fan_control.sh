#!/bin/bash

MAC_ADDRESS="78:04:73:19:77:BC"

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <command> [options]"
    exit 1
fi

COMMAND=$1
OPTIONS=${@:2}

java -Djava.library.path=/usr/local/lib/arm-linux-gnueabihf -cp .:/usr/local/lib/lib/java/tinyb.jar SimpleFanController $MAC_ADDRESS $COMMAND $OPTIONS
RESULT=$?

if [ $RESULT -eq 0 ]; then
    echo "Success"
else
    echo "Failure"
fi

exit $RESULT
