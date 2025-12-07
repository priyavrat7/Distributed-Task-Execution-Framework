```
for h in lab2-10 lab2-11 lab2-21
do
    mkdir -p $h/data
done

APACHE_DIR=../apache-zookeeper-3.6.2-bin

cd $APACHE_DIR
labs=("lab2-10" "lab2-11" "lab2-21")
for index in "${!labs[@]}";
do
    mkdir -p ${labs[$index]}/data
    echo $(($index+1)) > ${labs[$index]}/data/myid
done
```

Servers that are in use:
```
server.1-lab2-25.cs.mcgill.ca:22299:22399
server.2-lab2-26.cs.mcgill.ca:22299:22399
server.3-lab2-29.cs.mcgill.ca:22299:22399
```