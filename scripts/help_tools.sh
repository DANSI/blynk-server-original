#!/usr/bin/env bash

sudo apt-get update
sudo apt-get install fail2ban
sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

sudo apt-get install sendmail

apt-get install ipset
ipset create blacklist hash:ip hashsize 4096
iptables -I INPUT -m set --match-set blacklist src -j DROP
iptables -I FORWARD -m set --match-set blacklist src -j DROP