        sudo apt-get update
        sudo apt-get install fail2ban
        sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

        sudo apt-add-repository ppa:webupd8team/java
        sudo apt-get update
        sudo apt-get install oracle-java9-installer
        
        wget "https://github.com/blynkkk/blynk-server/releases/download/v0.29.0/server-0.29.0.jar"
        

server.properties

data.folder=./data
logs.folder=./logs
log.level=info
enable.db=true
admin.rootPath=/admin
allowed.administrator.ips=
server.host=xxx.blynk.cc
contact.email=xxx@blynk.cc
region=xxx
        
db.properties

jdbc.url=jdbc:postgresql://xxx:5432/blynk?tcpKeepAlive=true&socketTimeout=150
user=test
password=test
connection.timeout.millis=30000
clean.reporting=false

gcm.properties

mail.properties

IP Tables

        sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
        sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-port 9443