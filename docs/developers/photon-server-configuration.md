---
title: Configuring photon server
layout: page
parent: Information for developers
has_toc: false
---
The search bar in the app is driven by our own [photon server](https://github.com/komoot/photon).
We run it on EC2, and here are some notes on how it is configured to run.

# Photon Docker Setup Guide

## Start the EC2 Instance

We run an EC2 `r6g.large` instance running ubuntu. It needs at least 200GB of storage to run 
with a single copy of the planet wide index.


## Install Docker & Tools
```bash
sudo apt update
sudo apt install -y docker.io docker-compose pbzip2
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
newgrp docker
```

## Clone the photon-docker repo and built it 

```bash
git clone https://github.com/rtuszik/photon-docker.git
cd photon-docker
```

Then build the image:
```bash
docker build -t photon-docker .
```

## Download + Extract the Photon Data
To minimize the storage cost, we can only have a single copy of the data. This means that we want
to extract it as it downloads and we disable automatically updating the index. Without this, we have
to pay for double the storage on the server 24/7 even though half of it would only be required once
a week at most.

I'm going to file a PR against `photon-docker` but for now we have to extract the data prior to
running the docker.

```bash
wget -O - https://r2.koalasec.org/public/experimental/photon-db-latest.tar.bz2 | pbzip2 -cd | tar x
```

## Configure `docker-compose.yml`

Update the volume path:

```yaml
volumes:
  - /home/ubuntu/photon-docker/photon_data:/photon/photon_data
```

You may also want to disable automatic updates by adding:

```yaml
environment:
  - UPDATE_STRATEGY=DISABLED
```

---

## Run Photon

```bash
docker-compose up -d
```
Photon will now be running on port 2322.

## Confirm It’s Running

Check logs:
```bash
docker logs photon-docker
```

Test the endpoint (replace IP):
```bash
curl http://<YOUR_PUBLIC_IP>:2322/api?q=edinburgh
```

You should see a JSON response with geocoded results.

Check live logs:
```bash
docker-compose logs -f
```

# Restart container with config changes:

```bash
docker-compose down && docker-compose up -d
```
