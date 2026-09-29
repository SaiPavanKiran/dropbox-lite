# PostgreSQL 18 on EC2 with a Separate EBS Volume


> **Important:** The commands below are for a **new PostgreSQL
> installation / new database cluster**. Do not run `initdb` on a
> directory containing an existing PostgreSQL cluster or existing
> database data.

------------------------------------------------------------------------

# 1. Create the PostgreSQL EC2 and EBS volume with CloudFormation

The CloudFormation template creates:

-   A PostgreSQL EC2 instance.
-   A separate 5 GB `gp3` EBS volume.
-   An attachment between the EBS volume and the PostgreSQL EC2
    instance.


### Important about `/dev/xvdf`

CloudFormation requests:

``` text
/dev/xvdf
```

but on Nitro-based EC2 instances the operating system commonly exposes
the attached EBS volume as an NVMe device, for example:

``` text
/dev/nvme1n1
```

Therefore, **do not assume `/dev/xvdf` is the device objectName inside
Linux**.

Always check with:

``` bash
lsblk
```

------------------------------------------------------------------------

# 2. Connect to the PostgreSQL EC2

The PostgreSQL instance is in the private subnet, so use the configured
SSM access.

Once connected, check the disks:

``` bash
lsblk
```

Example:

``` text
NAME        SIZE TYPE MOUNTPOINT
nvme0n1       8G disk
└─nvme0n1p1   8G part /
nvme1n1       5G disk
```

Here:

``` text
nvme0n1 -> root EBS
nvme1n1 -> additional PostgreSQL EBS
```

The exact device objectName can differ, so identify the new volume using its
size and the EC2/EBS configuration.

------------------------------------------------------------------------

# 3. Format the new EBS volume

Only do this for a **brand-new / empty EBS volume**.

> **WARNING:** `mkfs` destroys existing filesystem data on the selected
> device. Verify the device with `lsblk` before running it.

For the new 5 GB volume:

``` bash
sudo mkfs -t xfs /dev/nvme1n1
```

After formatting, create the mount directory:

``` bash
sudo mkdir -p /data
```

Mount the volume:

``` bash
sudo mount /dev/nvme1n1 /data
```

Verify:

``` bash
df -h
```

You should see the new filesystem mounted at:

``` text
/data
```

For example:

``` text
/dev/nvme1n1    5G    ...    /data
```


------------------------------------------------------------------------

# 4. Make the EBS mount survive reboot

A manual `mount` does not automatically recreate the mount after reboot.

First get the filesystem UUID:

``` bash
sudo blkid /dev/nvme1n1
```

Example:

``` text
/dev/nvme1n1: UUID="a1b2c3d4-e5f6-7890-abcd-1234567890ab" TYPE="xfs"
```

Copy the actual UUID from your system.

Open `/etc/fstab`: Use nano or vi as you like

``` bash
sudo vi /etc/fstab
```

Add:

``` text
UUID=a1b2c3d4-e5f6-7890-abcd-1234567890ab /data xfs defaults,nofail 0 2
```

Replace the example UUID with your actual UUID.

### Why UUID instead of `/dev/nvme1n1`?

The NVMe device objectName should not be treated as a permanent identifier.
The EBS volume can appear under a different Linux device objectName after a
reboot.

The filesystem UUID identifies the filesystem itself.

### Test `/etc/fstab` without rebooting

Run:

``` bash
sudo mount -a
```

If there is no error, check:

``` bash
df -h
```

and:

``` bash
mount | grep /data
```

You should see the EBS volume mounted at `/data`.

------------------------------------------------------------------------

# 5. Install PostgreSQL 18

On the Amazon Linux 2023 system used by this project:

``` bash
sudo dnf install -y postgresql18-server postgresql18
```

Verify the packages:

``` bash
rpm -qa | grep -i postgresql
```

You should see packages similar to:

``` text
postgresql18-18.x-...
postgresql18-server-18.x-...
postgresql18-private-libs-18.x-...
```

## Important: PostgreSQL package layout

The original instructions for other PostgreSQL RPM distributions may
refer to paths such as:

``` text
/usr/pgsql-18/bin/initdb
```

and services such as:

``` text
postgresql-18.service
```

That is **not the layout used by the Amazon Linux 2023 PostgreSQL 18
packages used here**.

On this installation:

``` text
initdb:
/usr/bin/initdb

postgres executable:
/usr/bin/postgres

systemd service:
postgresql.service
```

Verify:

``` bash
ls -l /usr/bin/initdb
```

and:

``` bash
sudo systemctl cat postgresql
```

------------------------------------------------------------------------

# 6. Do NOT initialize PostgreSQL in the default directory

A newly installed PostgreSQL service normally expects:

``` text
/var/lib/pgsql/data
```

The service may report:

``` text
Directory "/var/lib/pgsql/data" is missing or empty.

Use "/usr/bin/postgresql-setup --initdb"
to initialize the database cluster.
```

That command would initialize the default location.

For this project, PostgreSQL should instead use:

``` text
/data/postgresql
```

because `/data` is the separate EBS volume.

Therefore, **do not run**:

``` bash
sudo /usr/bin/postgresql-setup --initdb
```

if the intention is to keep the PostgreSQL cluster on `/data`.

------------------------------------------------------------------------

# 7. Create the PostgreSQL data directory

Create the directory:

``` bash
sudo mkdir -p /data/postgresql
```

Give ownership to PostgreSQL:

``` bash
sudo chown postgres:postgres /data/postgresql
```

Set the recommended permissions:

``` bash
sudo chmod 700 /data/postgresql
```

Check:

``` bash
sudo ls -ld /data/postgresql
```

Expected ownership:

``` text
postgres postgres
```

------------------------------------------------------------------------

# 8. Configure PostgreSQL to use `/data/postgresql`

The Amazon Linux service uses:

``` text
PGDATA=/var/lib/pgsql/data
```

You can confirm this with:

``` bash
sudo systemctl cat postgresql
```

The service contains:

``` ini
Environment=PGDATA=/var/lib/pgsql/data
```

Do **not** edit:

``` text
/usr/lib/systemd/system/postgresql.service
```

directly because package upgrades can overwrite it.

Instead, create a systemd drop-in.

Create the directory:

``` bash
sudo mkdir -p /etc/systemd/system/postgresql.service.d
```

Create the override:

``` bash
sudo vi /etc/systemd/system/postgresql.service.d/override.conf
```

Put:

``` ini
[Unit]
RequiresMountsFor=/data/postgresql

[Service]
Environment=PGDATA=/data/postgresql
```

Save and exit.

Reload systemd:

``` bash
sudo systemctl daemon-reload
```

Verify:

``` bash
sudo systemctl cat postgresql
```

At the bottom you should see:

``` text
# /etc/systemd/system/postgresql.service.d/override.conf

[Unit]
RequiresMountsFor=/data/postgresql

[Service]
Environment=PGDATA=/data/postgresql
```

The original service will still show:

``` ini
Environment=PGDATA=/var/lib/pgsql/data
```

That is normal. The drop-in overrides it.

`RequiresMountsFor=/data/postgresql` also tells systemd that the
PostgreSQL service depends on the filesystem containing
`/data/postgresql` being mounted.

------------------------------------------------------------------------

# 9. Initialize the PostgreSQL cluster on the EBS

Make sure `/data` is actually mounted first:

``` bash
df -h /data
```

Then initialize the cluster:

``` bash
sudo -u postgres /usr/bin/initdb -D /data/postgresql
```

The `-D` option tells `initdb` where the PostgreSQL cluster should be
created.

After initialization, the directory should contain files such as:

``` text
PG_VERSION
base/
global/
pg_wal/
pg_hba.conf
postgresql.conf
```

Check:

``` bash
sudo ls -la /data/postgresql
```

### Important

If `initdb` says:

``` text
directory "/data/postgresql" exists but is not empty
```

**do not immediately delete the directory.**

If it already contains:

``` text
PG_VERSION
base/
global/
pg_wal/
postgresql.conf
pg_hba.conf
```

then it is already an initialized PostgreSQL cluster.

------------------------------------------------------------------------

# 10. Start PostgreSQL

Start the service:

``` bash
sudo systemctl start postgresql
```

Check:

``` bash
sudo systemctl status postgresql --no-pager
```

You want:

``` text
Active: active (running)
```

If it fails, inspect:

``` bash
sudo journalctl -xeu postgresql.service --no-pager
```

------------------------------------------------------------------------

# 11. Verify PostgreSQL is using the EBS

Run:

``` bash
sudo -u postgres psql -c "SHOW data_directory;"
```

Expected:

``` text
   data_directory
--------------------
 /data/postgresql
```

This confirms that PostgreSQL is using the separate EBS-mounted
directory.

You can also verify the mount:

``` bash
df -h /data
```

The final storage relationship should be:

``` text
PostgreSQL
    │
    └── PGDATA=/data/postgresql
                 │
                 ↓
              /data
                 │
                 ↓
           Separate EBS
```

------------------------------------------------------------------------

# 12. Enable PostgreSQL on boot

Once PostgreSQL is confirmed to work:

``` bash
sudo systemctl enable postgresql
```

Check:

``` bash
sudo systemctl is-enabled postgresql
```

Expected:

``` text
enabled
```

Because `/data` is also configured in `/etc/fstab`, the EBS filesystem
will be mounted automatically during boot.

------------------------------------------------------------------------

# 13. Create the application database and user

After PostgreSQL is running:

``` bash
sudo -u postgres psql
```

Create the application user:

``` sql
CREATE USER postgres WITH PASSWORD 'password';
```

Create the database:

``` sql
CREATE DATABASE "dropbox-lite" OWNER postgres;
```

Exit:

``` sql
\q
```


## Quick command checklist

For a fresh deployment, the important sequence is:

``` bash
# 1. Identify the new EBS
lsblk

# 2. Format ONLY the new/empty EBS
sudo mkfs -t xfs /dev/nvme1n1

# 3. Mount it
sudo mkdir -p /data
sudo mount /dev/nvme1n1 /data

# 4. Configure persistent mount
sudo blkid /dev/nvme1n1
sudo vi /etc/fstab
sudo mount -a

# 5. Install PostgreSQL
sudo dnf install -y postgresql18-server postgresql18

# 6. Create PostgreSQL directory
sudo mkdir -p /data/postgresql
sudo chown postgres:postgres /data/postgresql
sudo chmod 700 /data/postgresql

# 7. Configure systemd
sudo mkdir -p /etc/systemd/system/postgresql.service.d
sudo vi /etc/systemd/system/postgresql.service.d/override.conf

# Put in override.conf:
#
# [Unit]
# RequiresMountsFor=/data/postgresql
#
# [Service]
# Environment=PGDATA=/data/postgresql

sudo systemctl daemon-reload

# 8. Initialize PostgreSQL
sudo -u postgres /usr/bin/initdb -D /data/postgresql

# 9. Start and enable
sudo systemctl start postgresql
sudo systemctl enable postgresql

# 10. Verify
sudo -u postgres psql -c "SHOW data_directory;"
```

Expected final result:

``` text
/data/postgresql
```

### Add a rule

On the PostgreSQL EC2, first find the file:

```
sudo -u postgres psql -c "SHOW hba_file;"
```

Then open that file. For example, if it returns `/data/postgresql/pg_hba.conf`:

```
sudo vi /data/postgresql/pg_hba.conf
```

Add:

```
host    dropbox-lite    postgres    192.168.3.0/24    scram-sha-256
```

Open that file:

```
sudo vi /data/postgresql/postgresql.conf
```

Find:

```
#listen_addresses = 'localhost'
```

Change it to:

```
listen_addresses = '*'
```

If it already says:

```
listen_addresses = 'localhost'
```

change it to:

```
listen_addresses = '*'
```

Save the file.

Then reload PostgreSQL:

```
sudo systemctl reload postgresql
```
