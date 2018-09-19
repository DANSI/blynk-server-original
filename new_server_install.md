        sudo apt-get update
        sudo apt-get install fail2ban
        sudo cp /etc/fail2ban/jail.conf /etc/fail2ban/jail.local

        sudo timedatectl set-ntp no
        sudo apt-get install ntp

        sudo service ntp stop
        sudo ntpd -gq
        sudo service ntp start

        sudo apt-get upgrade
        sudo apt-get dist-upgrade
        sudo apt-get autoremove --purge

        sudo add-apt-repository ppa:linuxuprising/java
        sudo apt-get update
        sudo apt-get install oracle-java10-installer
        
        wget "https://github.com/blynkkk/blynk-server/releases/download/v0.39.6/server-0.39.6.jar"
        

server.properties

data.folder=./data
logs.folder=./logs
log.level=info
enable.db=true
force.port.80.for.csv=true
contact.email=xxx@blynk.cc
user.devices.limit=100000
user.message.quota.limit=10000
web.request.max.size=5242880
region=test
admin.rootPath=/test
product.name=test
server.host=test.blynk.cc
restore.host=test.blynk.cc
admin.email=test@blynk.cc
admin.pass=
vendor.email=

        
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
        
        sudo apt-get install iptables-persistent
        
        sudo iptables -t nat -A PREROUTING -p tcp --dport 8442 -j REDIRECT --to-port 8080
        iptables-save > /etc/iptables/rules.v4