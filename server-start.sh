APACHE_DIR=../apache-zookeeper-3.6.2-bin/
CONF_DIR=.
CONF_FILE='zoo-base.cfg'
cp $CONF_DIR/$CONF_FILE $APACHE_DIR/$(hostname)/$CONF_FILE
cd $APACHE_DIR/$(hostname)
pwd
../bin/zkServer.sh start ./$CONF_FILE