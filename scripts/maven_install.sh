#!/usr/bin/env bash
wget http://apache.volia.net/maven/maven-3/3.3.1/binaries/apache-maven-3.3.1-bin.tar.gz

sudo tar -xzvf apache-maven-3.3.1-bin.tar.gz

export M2_HOME=/home/azureuser/apache-maven-3.3.1
export M2=$M2_HOME/bin
export PATH=$M2:$PATH