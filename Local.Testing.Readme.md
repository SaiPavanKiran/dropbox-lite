# Dropbox Lite — Local Development and Testing

This guide explains how to run Dropbox Lite locally using Docker Compose, inspect application logs, access the PostgreSQL database, and test the REST APIs.

Docker installation instructions are intentionally excluded. Install Docker and ensure Docker Compose is available before following this guide.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Configure Environment Variables](#configure-environment-variables)
- [Start the Application](#start-the-application)
- [Access the Application](#access-the-application)
- [Docker Commands](#docker-commands)
- [View Application Logs](#view-application-logs)
- [Access PostgreSQL](#access-postgresql)
- [Create Database Tables](#create-database-tables)

---

## Prerequisites

Before starting, make sure you have:

- Docker installed and running.
- Docker Compose available through `docker compose`.
- The Dropbox Lite source code cloned locally and build the war using it.
- The required project files, including the Dockerfile under `tomcat/`.
- Valid gmail credentials for the application's owner account and password must be stored in windows environmental variables -- just req to sent otps.

You can check whether Docker and Docker Compose are available with:

```bash
docker --version
docker compose version
```

Run these commands from your terminal.

## Configure-Environment-Variables

The Tomcat service requires two environment variables:

|Variable|Purpose|
|---|---|
|`OWNER_MAIL`|Email address of the application owner|
|`DROPBOX_LITE_OWNER_PASSWORD`|Password supplied to the application as `OWNER_PASSWORD`|

The Compose file requires both values to be set. If either is missing, Compose will stop before starting the service.

### Option 1: Set environment variables in your terminal

You can also set the variables in your terminal before starting Compose.

For Bash:

```bash
export OWNER_MAIL="your-email@example.com"
export DROPBOX_LITE_OWNER_PASSWORD="your-owner-password"
```

For PowerShell:

```powershell
$env:OWNER_MAIL = "your-email@example.com"
$env:DROPBOX_LITE_OWNER_PASSWORD = "your-owner-password"
```

Use the same terminal session to run the Docker Compose commands.

## Start-the-Application

Open a terminal in the directory containing your `compose.yaml` or `docker-compose.yml` file.

### First-time startup

Run:

```bash
docker compose up -d --build
```

This command:

1. Builds the Tomcat image using `tomcat/Dockerfile`.
2. Starts the Tomcat and PostgreSQL containers.
3. Runs the containers in the background.
4. Waits for PostgreSQL's configured health check before starting Tomcat.

The initial build may take longer because Docker needs to build the application image.

### Subsequent startup

After the image has been built, you can start the services with:

```bash
docker compose up -d
```

Use `--build` again whenever you need to rebuild the Tomcat image after changing the application or its Docker build inputs.

### Verify that the containers are running

```bash
docker ps
```

You should see the running containers, including:

```text
dropbox-lite-tomcat
postgres.dropbox-lite.internal
```


## Access the Application

Once the Tomcat container has started, use:

```text
http://localhost:9091
```

The application is configured with the context path:

```text
/dropbox-lite
```

Therefore, the API base URL is:

```text
http://localhost:9091/dropbox-lite
```

### Test the health endpoint

Open the following URL in your browser or call it using an HTTP client:

```http
GET http://localhost:9091/dropbox-lite/health
```

This endpoint does not require authentication.

If the health endpoint responds successfully, you can proceed to account creation, login, and the other API tests.

For the complete list of API endpoints, request bodies, and authentication requirements, refer to the API usage README.

## Docker-Commands

These are the main commands useful for local development.

### List running containers

```bash
docker ps
```

Shows running containers, their IDs, images, port mappings, and names.

### List all containers

```bash
docker ps -a
```

Includes stopped containers, which is useful when debugging failed startups.

### List downloaded and built images

```bash
docker images
```

Displays Docker images available locally.

### Rebuild and start

```bash
docker compose up -d --build
```

Rebuilds the image and starts the services.

### Start without rebuilding

```bash
docker compose up -d
```

Starts the services using the existing images.

### Stop the services

```bash
docker compose stop
```

Stops the running containers without removing them. Their container definitions remain available for a later restart.

### Open a shell inside Tomcat

```bash
docker exec -it dropbox-lite-tomcat sh
```

This opens a shell inside the running Tomcat container.

Use `exit` to leave the container shell.

### Open a shell inside PostgreSQL

```bash
docker exec -it postgres.dropbox-lite.internal sh
```

This opens a shell inside the PostgreSQL container.

From there, you can use `psql` to connect to the database.

## View Application Logs

The application log file is located at:

```text
webapps/logs/dropbox_lite.log
```

To inspect the log file inside the Tomcat container, use:

```bash
docker exec -it dropbox-lite-tomcat sh
```

Then run:

```bash
cat webapps/logs/dropbox_lite.log
```

If the working directory is different, locate the file using its full path inside the container.

### View Docker container output

You can also inspect the container's standard output and error streams:

```bash
docker logs dropbox-lite-tomcat
```

PostgreSQL logs can be viewed using:

```bash
docker logs postgres.dropbox-lite.internal
```

Application log files and Docker logs are different: the application may write to its own log file rather than standard output.

## Access PostgreSQL

The Compose configuration starts PostgreSQL with these local development settings:

|Setting|Value|
|---|---|
|Container name|`postgres.dropbox-lite.internal`|
|Database|`dropbox-lite`|
|Username|`postgres`|
|Password|`password`|
|Host port|`5432`|
|Container port|`5432`|

These credentials are for local development only. Do not expose this database configuration as-is in a production deployment.

### Step 1: Enter the PostgreSQL container

Run:

```bash
docker exec -it postgres.dropbox-lite.internal sh
```

### Step 2: Connect to the database

Inside the container, execute:

```bash
psql -U postgres -d dropbox-lite
```

If prompted for a password, enter:

```text
password
```

The Compose configuration sets this password when initializing the database.

Once connected, you should see a PostgreSQL prompt similar to:

```text
dropbox-lite=#
```

You can now execute SQL statements.

### Useful PostgreSQL commands

List all databases:

```sql
\l
```

List tables in the current schema:

```sql
\dt
```

Describe a table:

```sql
\d table_name
```

List tables :

```sql
\dt
```

Exit the PostgreSQL shell:

```sql
\q
```


## Create-Database-Tables

The application entities define the database structures used by Dropbox Lite.

To determine the required tables and columns, inspect the entity files in the project source code. Check the comments in those files for the corresponding SQL table definitions or creation statements.

### Recommended procedure

1. Locate the entity classes in the source code.
2. Read their comments and identify the associated table definitions.
3. Review the SQL statements and execute the required `CREATE TABLE` statements in the `dropbox-lite` database.
4. Verify that the tables were created successfully.

For example, after connecting to PostgreSQL:

```sql
\dt
```

To inspect an individual table:

```sql
\d your_table_name
```

**Important:** Use the actual SQL definitions documented in the entity files. Do not create tables from guessed column names or types, because the application may depend on exact column types, constraints, foreign keys, indexes, and relationships.

If the project includes SQL initialization scripts or database migration files, use the project's intended schema initialization process instead of manually creating duplicate tables.
