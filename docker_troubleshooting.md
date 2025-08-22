# Troubleshooting: Docker data directory permissions

This guide helps you resolve filesystem permission errors when launching the OLTP‑1 Docker environments. The focus is **bind‑mounted data directories** (the host folders that Docker maps into a container to store database files).

---

## Quick symptoms

You likely have a permissions problem if you see messages like:

* `permission denied`, `EACCES`, or `Operation not permitted` when the container starts
* PostgreSQL: `FATAL: data directory ... has invalid permissions` or `could not create directory ...: Permission denied`
* MariaDB: `Can't create/write to file` or `InnoDB: Operating system error number 13`
* SQL Server: `The specified data directory is not writeable` or `ERROR: BootstrapSystemDataDirectories failed`

---

## Why this happens (in one minute)

* **Bind mounts keep host permissions.** When you mount `./pgdata` into a container, the **host** folder’s owner and mode are used. The database process inside the container runs as a **non‑root user** (e.g., `postgres`, `mysql`, `mssql`) and may not be allowed to write there.
* **Different UIDs/GIDs.** Inside the image, the service user has a **numeric UID/GID** that may not match any user on your host. Ownership must match the numeric IDs, not the name.
* **SELinux/AppArmor.** On Fedora/RHEL/CentOS (SELinux enforcing), containers can’t access a bind‑mounted directory unless the path is labeled for containers.
* **Windows/WSL2/macOS quirks.** Host file‑sharing settings or mounting from slow/unsupported paths can block or degrade access.

---

## Fast fix checklist

1. **Pre‑create the data directory on the host** and ensure it’s empty:

```bash
mkdir -p ./pgdata ./mssql_data ./mariadb_data
```

2. **Find the UID\:GID used by the image** for its data directory owner:

```bash
# Example for PostgreSQL
docker run --rm --entrypoint bash postgres:17 -lc 'id -u; id -g'

# Example for MariaDB
docker run --rm --entrypoint bash mariadb:11 -lc 'id -u; id -g'

# Example for SQL Server (Linux)
docker run --rm --entrypoint bash mcr.microsoft.com/mssql/server:2022-latest -lc 'id -u; id -g'
```

> Tip: You can also inspect a running container: `docker exec -it <name> sh -lc "id -u; id -g"`.

3. **Chown the host directory to match** that UID\:GID and set safe modes:

```bash
# Replace 999:999 with the numbers you found
sudo chown -R 999:999 ./pgdata
chmod 700 ./pgdata  # PostgreSQL prefers 700 on PGDATA

# MariaDB (commonly 999:999, but verify)
sudo chown -R 999:999 ./mariadb_data
chmod -R u+rwX,g-rwx,o-rwx ./mariadb_data

# SQL Server (often UID 10001 and group 0, but verify)
sudo chown -R 10001:0 ./mssql_data
chmod -R 770 ./mssql_data
```

4. **SELinux only (Fedora/RHEL/CentOS):** add the `:Z` flag to bind mounts or adjust the label:

```yaml
# docker‑compose.yml (example)
services:
  postgres:
    volumes:
      - ./pgdata:/var/lib/postgresql/data:Z
```

Or run once:

```bash
sudo chcon -Rt svirt_sandbox_file_t ./pgdata
```

5. **Restart cleanly:**

```bash
docker compose down
# keep volumes, just stop containers
# (if you need a full reset, also remove volumes:
#   docker compose down -v)

docker compose up -d
```

If it still fails, check the container logs:

```bash
docker compose logs -f --tail=200 <service-name>
```

---

## Per‑database notes

### PostgreSQL / OrioleDB

* Data directory in container: `/var/lib/postgresql/data` (aka `PGDATA`).
* Preferred mode on `PGDATA` is **700**.
* Common fixes:

  * Ensure host dir ownership matches the container postgres UID\:GID.
  * Mount with `:Z` on SELinux hosts.
  * Avoid `777`; it will cause PostgreSQL to refuse the directory.

**Compose example:**

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
    volumes:
      - ./pgdata:/var/lib/postgresql/data:Z  # add :Z on SELinux
```

### MariaDB

* Data directory in container: `/var/lib/mysql`.
* Ensure host dir owner is the numeric UID of `mysql` inside the image.
* If you see `InnoDB` permission errors, verify both **ownership** and **execute** bit on directories (`chmod u+rwx`).

**Compose example:**

```yaml
services:
  mariadb:
    image: mariadb:11
    environment:
      - MARIADB_ROOT_PASSWORD=${MARIADB_ROOT_PASSWORD}
    volumes:
      - ./mariadb_data:/var/lib/mysql:Z
```

### Microsoft SQL Server (Linux)

* Data/config directory in container: `/var/opt/mssql`.
* The service runs as a non‑root user; ensure host dir ownership matches (commonly UID **10001** and group **0**, but verify).
* Required permissions are typically **770** on the mount path.
* On Windows/macOS, ensure the host folder is **shared/allowed** in Docker Desktop settings and resides on a local drive.

**Compose example:**

```yaml
services:
  mssql:
    image: mcr.microsoft.com/mssql/server:2022-latest
    environment:
      - ACCEPT_EULA=Y
      - SA_PASSWORD=${SA_PASSWORD}
    volumes:
      - ./mssql_data:/var/opt/mssql  # add :Z on SELinux hosts
```

---

## Named volumes (easy mode)

If you don’t need to see the raw files on the host, **use a named volume** instead of a bind mount. Docker will manage permissions for you, avoiding most host‑side issues.

```yaml
services:
  postgres:
    image: postgres:17
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

> You can still back up/restore with `docker run --rm -v pgdata:/data ...` and `pg_dump`/`pg_restore`.

---

## Windows & WSL2 notes

* Prefer storing bind‑mounted project folders under your **WSL2 distro’s filesystem** (e.g., `/home/<you>/project`) instead of `/mnt/c/...` for fewer permission/performance hiccups.
* In Docker Desktop, ensure the drive/directory is **File Sharing** enabled.
* If using `\wsl$` paths, confirm Docker Desktop’s integration with that distro is enabled.

---

## How to discover the correct UID/GID reliably

Instead of guessing, query the image/container:

```bash
# Show UID:GID used by the service user
docker run --rm --entrypoint sh <image> -lc 'id -u; id -g'

# Or read ownership of the in‑container data dir (may require root inside the container)
docker run --rm --user root --entrypoint sh <image> -lc 'stat -c %u:%g /var/lib/postgresql/data || true'
```

Then mirror that on the host with `chown` before starting the stack.

---

## Repair recipes (copy/paste)

### PostgreSQL/OrioleDB (Linux/macOS, SELinux hosts add `:Z`)

```bash
# 1) create
mkdir -p ./pgdata

# 2) find IDs
docker run --rm --entrypoint bash postgres:17 -lc 'id -u; id -g'
# suppose it prints 999 and 999

# 3) own & secure
sudo chown -R 999:999 ./pgdata
chmod 700 ./pgdata

# 4) restart
docker compose down && docker compose up -d
```

### MariaDB

```bash
mkdir -p ./mariadb_data
ids=$(docker run --rm --entrypoint sh mariadb:11 -lc 'id -u; id -g' | paste -sd : -)
sudo chown -R "$ids" ./mariadb_data
chmod -R u+rwX,g-rwx,o-rwx ./mariadb_data

docker compose down && docker compose up -d
```

### SQL Server (Linux)

```bash
mkdir -p ./mssql_data
# Typically 10001:0, but verify
ids=$(docker run --rm --entrypoint sh mcr.microsoft.com/mssql/server:2022-latest -lc 'id -u; id -g' | paste -sd : -)
sudo chown -R "$ids" ./mssql_data
chmod -R 770 ./mssql_data

docker compose down && docker compose up -d
```
