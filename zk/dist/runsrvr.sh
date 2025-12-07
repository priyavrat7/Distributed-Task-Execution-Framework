#!/bin/bash

if [[ -z "$ZOOBINDIR" ]]
then
	echo "Error!! ZOOBINDIR is not set" 1>&2
	exit 1
fi

. $ZOOBINDIR/zkEnv.sh

#TODO Include your ZooKeeper connection string here. Make sure there are no spaces.
# 	Replace with your server names and client ports.

if [[ -z "$ZKSERVER" ]]
then
	export ZKSERVER=lab2-10.cs.mcgill.ca:21827,lab2-11.cs.mcgill.ca:21827,lab2-12.cs.mcgill.ca:21827
fi
java -cp $CLASSPATH:../task:.: DistProcess 
