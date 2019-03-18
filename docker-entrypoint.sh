#!/bin/bash

set -e

pwd
ls -la

./bin/stawallet database init
./bin/stawallet database populate
./bin/stawallet database migrate

if [ "$1" = 'stawallet' ]; then
    exec ./bin/stawallet "$@"
fi

exec "$@"
