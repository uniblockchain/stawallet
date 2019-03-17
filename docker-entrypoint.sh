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

alias stawallet=./stawallet/bin/stawallet

stawallet database init
stawallet database populate
stawallet database migrate

case $SERVICE in 
	api)
		stawallet serve
		;;
	btc_watcher)
		stawallet watch $2
		;;
	cli)
		tail -f /dev/null
		;;
  *)
		exec $@
		;;
esac
