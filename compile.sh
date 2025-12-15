#!/bin/bash

BASEDIR=.

cd $BASEDIR/zk/task
./compiletask.sh

cd $BASEDIR/zk/clnt
./compileclnt.sh

cd $BASEDIR/zk/dist
./compilesrvr.sh