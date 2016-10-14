#! /bin/bash

sign() {
local passwordKeyStore=`secret-tool "lookup" "app" "java-keystore"`
local password=`secret-tool "lookup" "app" "jarsigner"`
for jar in ${jars[@]} ; do
	echo -n "signing $jar " | sed "s/\\.\\/.*\\///g" \
		| sed "s/\\.\\///g"
    jar_unsigned=`sed "s/\\.jar/\\.unsigned.jar/g" <<<$jar`
    cp $jar $jar_unsigned
    echo -ne "\033[33mbusy\033[0m"
	jarsigner $jar "stone" "-tsa" "http://timestamp.comodoca.com/rfc3161" \
    "-storepass" $passwordKeyStore "-keypass" $password >>$LOG 
	success=$?
	printf "\b\b\b\b"
	if (( $success != 0 )) ; then
		echo -e "\033[31mfailed"
		echo -e "\033[0m$success"
        mv $jar_unsigned $jar
        echo "\trolled back changes"
		exit $success
	fi
    echo -ne "\033[34mverifying"
    rm $jar_unsigned
    jarsigner -verify $jar -verbose >>$LOG
	printf "\b\b\b\b\b\b\b\b\b"
	printf "         "
	printf "\b\b\b\b\b\b\b\b\b"
	echo -e "\033[32mdone\033[0m"
done
unset password
}

jars=(`find . -name \*.jar`)
LOG="$0.log"

trap "exit 1" SIGINT
echo "" >$LOG
sign
