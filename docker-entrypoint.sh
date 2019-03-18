#!/bin/bash
help () {
	echo "Available daemons are: "
	echo "  api"
	echo "  watcher [wallet-name]"
	echo "  cli"
}

echo "@=$@"

if [ $# -lt 1 ];then
	help
	exit 0
fi

SERVICE=$1

./bin/stawallet database init
./bin/stawallet database populate
./bin/stawallet database migrate

case $SERVICE in 
	api)
		./stawallet/bin/stawallet serve
		;;
	btc_watcher)
		./stawallet/bin/stawallet watch $2
		;;
	cli)
        alias stawallet=./stawallet/bin/stawallet
		tail -f /dev/null
		;;
  *)
		exec $@
		;;
esac
