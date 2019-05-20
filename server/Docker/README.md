## HOW TO 

### Configure

Just edit the Dockerfile and change ENV vars


### Build

```bash
docker build -t blynk .
```

### RUN

```bash
docker run --name blynk-server -v ~/blynk-server/server/Docker:/data -p 8440:8440 -p 8080:8080 -p 9443:9443 -d blynk 
```

Don't forget to change the port attribution if you change on the ENV vars in Dockerfile


## UPDATE Blynk version

## How to

Stop and remove your actual container

```bash
docker stop blynk-server && docker rm blynk-server
```

- Edit your Blynk server version on the ENV var in Dockerfile
- Build & Launch


Have Fun ! :v: :whale:

