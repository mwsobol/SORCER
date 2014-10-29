#/bin/sh

cwd=`pwd`
echo $cwd
properties="-Djava.security.policy=policy.all -Dwebster.root=$cwd/repository -Dwebster.port=7001 -Dwebster.tmp.dir=$cwd/repository -Dwebster.debug=
true" 
echo $properties
java $properties -jar webster-4.0.1.jar 