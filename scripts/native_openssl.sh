#!/usr/bin/env bash

sudo apt-get install git
sudo apt-get install autoconf automake libtool make tar libapr1-dev libssl-dev
git clone https://github.com/netty/netty-tcnative.git
cd netty-tcnative
git checkout netty-tcnative-1.1.32.Fork1
mvn clean install