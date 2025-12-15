APACHE_DIR=../apache-zookeeper-3.6.2-bin/
CONF_DIR=.
CONF_FILE=zoo-base.cfg
cd $APACHE_DIR/$(hostname)
# cp $CONF_DIR/$CONF_FILE ./$CONF_FILE # No cp so we use the old conf file, probably...
../bin/zkServer.sh stop ./$CONF_FILE