#!/bin/bash

set -e

./stawallet database init
./stawallet database populate
./stawallet database migrate

if [ "$1" = 'stawallet' ]; then
    ./stawallet "$@"
fi

exec "$@"
