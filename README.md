# Fiware NDN Adapter - Connect Fiware-Orion with NDN

## Mirror
If you are viewing this from a mirror then please visit `https://github.com/NDNAppBachelorThesis/fiware-ndn-adapter` to
access the build artifacts


## How do add a Grafana Dashboard
Grafana is hosted under port ``3003``
The default login credentials are ``admin``, `admin`

### Create data source
Go to Grafana and click ``Add your first data source`` and perform the following steps:
- Select ``PostgreSQL`` as database (CrateDB exposes a postgres API)
- Enter the following parameters
  - Name: ``CrateDB``
  - Host Url: ``crate-db:5432``
  - Database name: ``doc``
  - Username: ``crate``
  - Password: ``<empty>``
  - TLS/SSL Mode: ``disable``
- Click Save

### Import the dashboard
This repository contains a file called ``grafana_dashboard.json``. 
This can be imported to Grafana to get a basic dashboard up and running.
Perform the following steps to import the dashboard
- To to ``<host>:3003/connections/datasources``, click the CrateDB datasource and copy the UUID (last part) from the URL
- In the ``grafana_dashboard.json`` file search for `"type": "postgres"` and replace the uid below with the copied UUID. This must be done two times.
- Go to ``<host>:3003/dashboard/import``, paste the modified json file and click `Load` and then `Import`
The dashboard is now imported

### Using the dashboard
- In the top right change the date range to ``Last 15 minutes`` and the `Auto refresh interval` to `10s`
- In the top left you have a dropdown to select which sensor you want to display
