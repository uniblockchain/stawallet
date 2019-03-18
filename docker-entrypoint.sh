#!/bin/bash

set -e

./bin/stawallet database init
./bin/stawallet database populate
./bin/stawallet database migrate

if [ "$1" = 'stawallet' ]; then
    exec "./bin/$@"
fi

exec "$@"
