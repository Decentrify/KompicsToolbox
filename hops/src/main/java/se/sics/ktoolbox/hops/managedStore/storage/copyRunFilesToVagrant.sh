#!/bin/bash
# Deploy the frontend to the glassfish home directory and run bower
export PORT=20003
export WEBPORT=14003
export SERVER=bbc1.sics.se
export key=private_key

scp root@bbc1.sics.se:/home/hopsworks/johan/hopsworks-chef/.vagrant/machines/default/virtualbox/private_key .

ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -p $PORT vagrant@${SERVER} "cd /srv/hadoop/etc/hadoop/"

scp -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -i $key -P ${PORT} -r vagrant@${SERVER}:/srv/hadoop/etc/hadoop/ .
