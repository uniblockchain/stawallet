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

stemerald database init
stemerald database populate
stemerald database migrate

case $SERVICE in 
	api)
		stemerald serve
		;;
	btc_watcher)
		stemerald watch $2
		;;
	cli)
		tail -f /dev/null
		;;
  *)
		exec $@
		;;
esac
