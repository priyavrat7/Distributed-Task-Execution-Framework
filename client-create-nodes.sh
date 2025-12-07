if [[ -z "$ZOOBINDIR" ]]
then
	echo "Error!! ZOOBINDIR is not set" 1>&2
	exit 1
fi

if [[ -z "$ZKSERVER" ]]
then
	echo "Error!! ZKSERVER is not set" 1>&2
	exit 1
fi

printf "deleteall /dist27\ncreate /dist27 ''\ncreate /dist27/tasks ''\ncreate /dist27/workers ''\ncreate /dist27/idle ''\nquit" | $ZOOBINDIR/zkCli.sh \
-server $ZKSERVER