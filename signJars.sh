#! /bin/bash

sign() {
for jar in ${jars[@]} ; do
	echo -n "signing $jar " | sed "s/\\.\\/.*\\///g" \
		| sed "s/\\.\\///g"
	echo -e -n "\033[33mbusy\033[0m"
	password=`secret-tool "lookup" "app" "jarsigner"`
	echo $password | jarsigner $jar "stone" "-tsa" "http://timestamp.comodoca.com/rfc3161" >>$LOG 2>/dev/null
	success=$?
	printf "\b\b\b\b"
	if (( $success != 0 )) ; then
		echo -e "\033[31mfailed"
		echo -e "\033[0m$success"
		exit $success
	fi
	echo -e "\033[32mdone\033[0m"
done
}

jars=(`find . -name \*.jar`)
LOG="$0.log"

trap "exit 1" SIGINT
echo "" >$LOG
sign
