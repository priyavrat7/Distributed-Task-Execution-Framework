CURR_DIR=$(pwd)
APACHE_DIR=../apache-zookeeper-3.6.2-bin
ZOO_BASE_SRC=zoo-base-source.cfg
ZOO_BASE_DST=zoo-base.cfg
labs=("open-gpu-12" "open-gpu-13" "open-gpu-14")


CLIENT_PORT=21899
export ZKSERVER=$(printf "%s,%s,%s" "${labs[0]}.cs.mcgill.ca:$CLIENT_PORT" "${labs[1]}.cs.mcgill.ca:$CLIENT_PORT" "${labs[2]}.cs.mcgill.ca:$CLIENT_PORT")

cp $ZOO_BASE_SRC $ZOO_BASE_DST
for index in "${!labs[@]}";
do
    echo server.$(($index+1))=${labs[$index]}.cs.mcgill.ca:22298:22398 >> $ZOO_BASE_DST
done

cd $APACHE_DIR
for index in "${!labs[@]}";
do
    mkdir -p ${labs[$index]}/data
    echo $(($index+1)) > ${labs[$index]}/data/myid
done

cd $CURR_DIR