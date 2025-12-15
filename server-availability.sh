COMPUTERS=('lab2-' 'lab9-' 'open-gpu-' 'open-')
RANGES=(51 12 32 12)
# lab2 1..51
# lab9 1..12
# open-gpu- 1..32
# open- 1..12
PREFIX=${COMPUTERS[$1]}
RANGE=${RANGES[$1]}
for i in $(seq $RANGE)
do
    echo $PREFIX$i
    sshpass -f temp_password.txt ssh -o PubkeyAuthentication=no -o ConnectTimeout=2 rau2@$PREFIX$i.cs.mcgill.ca date
done