# Fiware NDN Adapter - Connect Fiware-Orion with NDN
This Repository contains all information about Docker and the infrastructure

## Mirror
If you are viewing this from a mirror then please visit `https://github.com/NDNAppBachelorThesis/fiware-ndn-adapter` to
access the build artifacts


# Docker-compose File
The complete infrastructure can be deployed using the single ``docker-compose.yml`` file. The images are a mix of 
official and self-made images. The self-made images are mostly hosted on the github container registry 
[ghcr](https://github.com/orgs/NDNAppBachelorThesis/packages).
Only the NFD image is hosted on the docker container registry as it's too large for githubs container registry.
[docker container registry](https://hub.docker.com/repository/docker/derteufelqwe/ndn-nfd)

## Using the docker-compose file
Using the ``docker-compose.yml`` file is pretty straight forward. Just follow these steps
1. Download the ``docker-compose.yml`` file to your machine
2. In the ``management`` service set the `HOST_IP` env to the hosts IP address. This must be the IP of the network 
   which the ESP32 boards use as well.
3. Edit ``/etc/sysctl.conf`` and add `vm.max_map_count = 262144`. Run `sudo sysctl -p` to apply this change. It is required
   for CrateDB to work properly [CrateDB docs](https://cratedb.com/docs/crate/howtos/en/latest/admin/bootstrap-checks.html#linux)
4. Run ``docker compose pull`` to download all images (this can take a while)
5. Run ``docker compose up -d`` to start the containers.
6. After some time you can go the ``http://<yourIp>:3000``, login using user `admin` and password `root` (if you didn't 
   change the credentials) and check if the infrastructure is up and running

## Available Ports
This table contains the relevant ports and their purpose

| Port | Container         | Protocol               | Description            |
|------|-------------------|------------------------|------------------------|
| 3000 | Management Server | HTTP                   | The management website |
| 3003 | Grafana           | HTTP                   | The Grafana website    |
| 4200 | CrateDB           | HTTP                   | CrateDB web interface  |
| 5432 | CrateDB           | Postgres Wire Protocol | Access to the database |
| 6363 | NFD               | NDN                    | The NFDs NDN network   |
| 1026 | Fiware-Orion      | HTTP (API)             | The API endpoint       |
| 8668 | Quantumleap       | HTTP (API)             | The API endpoint       |



# How do add a Grafana Dashboard
Grafana is hosted under port ``3003``
The default login credentials are ``admin``, `admin`

## Create data source
Go to Grafana and click ``Add your first data source`` and perform the following steps:
- Select ``PostgreSQL`` as database (CrateDB exposes a postgres API)
- Enter the following parameters
  - Name: ``CrateDB``
  - Host Url: ``crate-db:5432``
  - Database name: ``doc``
  - Username: ``crate``
  - Password: ``<empty>``
  - TLS/SSL Mode: ``disable``
- Click ``Save & test``

## Import the dashboard
This repository contains a file called ``grafana_dashboard.json``. 
It can be imported to Grafana to get a basic dashboard up and running.
Perform the following steps to import the dashboard
- Go to ``<host>:3003/connections/datasources``, click the CrateDB datasource and copy the UUID (last part) from the URL 
  (looks like this `aaec1afe-29d4-4004-b76f-2af5edc2c6bf`)
- In the ``grafana_dashboard.json`` file search for `"type": "postgres"` and replace the uid below with the copied UUID. 
  This must be done two times.
  After that search for `"type": "grafana-postgresql-datasource"` and replace the uid below with the copied UUID.
  This must be done one time.
- Go to ``<host>:3003/dashboard/import``, paste the modified json file and click `Load` and then `Import`
The dashboard is now imported

## Using the dashboard
- In the top right change the date range to ``Last 15 minutes`` and the `Auto refresh interval` to `10s`
- In the top left you have a dropdown to select which sensor you want to display


# Getting started deveoping
To get started developing locally do the following steps:
1. Install the required dependencies using maven
2. Run the `Main.kt` file with the `FIWARE_HOST` and `NDN_HOST` environment variables set to the 
   IP of the fiware and ndn hosts. For this to work both NDN and Fiware-Orion must be running