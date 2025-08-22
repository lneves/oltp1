# OLTP-1: A TPC-E Inspired Database Benchmark

---

## Table of Contents

* [Why OLTP-1](#why-oltp-1)
* [Features](#features)
* [Primary Deviations from the Specification](#primary-deviations-from-the-specification)
* [Getting Started](#getting-started)
* [How to Run](#how-to-run)
* [Benchmark Details](#benchmark-details)
* [Contributing](#contributing)
* [License](#license)
* [Related resources](#related-resources)
* [Troubleshooting](docker_troubleshooting.md)

---

## Why OLTP-1

OLTP-1 is a high-performance, TPC-E inspired benchmark tool designed to measure the OnLine Transaction Processing (OLTP) performance of different database systems. Written in Java, it aims to provide a reasonably compliant implementation of the TPC-E workload, focusing on raw transactional performance and cross-database comparability.

While the older TPC-C benchmark is still more commonly used, TPC-E is the TPC's official and most realistic standard for evaluating modern OLTP systems. OLTP-1 reimagines the TPC-E benchmark with a modern perspective.

**Primary motivations:**

* **Enable Cross-Database Comparison**: A core goal is to provide a consistent and fair workload to "shootout" different database technologies. The same transactional logic is executed against PostgreSQL, SQL Server, and others, providing a level playing field for performance comparisons. *(And, in all honesty, ever since Microsoft SQL Server was released for Linux, I've been itching to compare it head-to-head with PostgreSQL!)*
* **Modernize the Toolchain**: The official TPC-E tools are built in C++ with rigid assumptions about system architectures (e.g., running on Windows, tightly coupled to SQL Server), difficult to extend or modernize without forking and significant engineering work. This project is a chance to leverage something a bit more modern and easier to maintain, extend, and integrate into current development environments.
* **Promote Extensibility**: Core transaction logic is decoupled from the SQL queries, which makes it straightforward to add support for new databases. By simply implementing a new set of dialect-specific queries and registering them in the existing factory system, a new database can be integrated without altering the main benchmark code.

## Features

* **Comprehensive Workload**: Implements all of the TPC-E transactions, including:
    * **Read-only**: `Broker-Volume` , `Customer-Position` , `Market-Watch` , `Security-Detail` , `Trade-Lookup` , `Trade-Status`.
    * **Read-write**: `Trade-Order` , `Trade-Result` , `Trade-Update` , `Data-Maintenance` , `Trade-Cleanup`.
* **Configurable**: Workload parameters such as the number of clients and test duration are fully configurable via command-line arguments.
* **Multi-Database Support**: Designed for portability with specific optimizations for:
    * PostgreSQL 
    * Microsoft SQL Server 
    * OrioleDB (via its PostgreSQL compatibility)
    * MariaDB

* **High-Fidelity Data Generation**: Implements the TPC-E specification's non-uniform random data generation to ensure a realistic and skewed data access pattern.
* **Asynchronous Processing**: Simulates the TPC-E Market Exchange Emulator (MEE) by using an internal thread pool to process some of the transactions asynchronously.
* **Detailed Reporting**: Provides a clear and concise summary report to the console upon completion, detailing TPS, latency percentiles, and error rates for each transaction type.


### Primary Deviations from the Specification

The deviations from the official TPC-E specification are generally simplifications or modernizations that make the project more practical.

#### 1. Market Exchange Emulator (MEE) Implementation
* **Deviation**: The TPC-E specification describes the MEE as a separate component that processes trades asynchronously. This implementation simulates this **in-process** using a Java `ExecutorService`. When a `Trade-Order` transaction completes, it submits the `Trade-Result` and `Market-Feed` tasks to this thread pool for deferred execution.
* **Reasoning**: This is a pragmatic simplification. Building a true external MEE application is complex. Using an in-process thread pool achieves the same essential goal—asynchronous processing of trade outcomes—without the overhead of inter-process communication, making the benchmark easier to deploy and run.
One potential issue, however, is the lack of guaranteed ordering. The specification implies a sequence where a trade result might trigger a market feed. By submitting both tasks to a thread pool, their execution order is not guaranteed, which could lead to race conditions or logical inconsistencies if downstream processing depends on this order. For the purpose of this benchmark it's not critical—as the goal is to measure transactional throughput rather than guarantee the logical consistency of a simulated market feed—but it is a deviation from a real-world sequential flow.


#### 2. Transaction Input Generation
* **Deviation**: The official TPC-E tool is a sophisticated state machine that navigates data interdependencies and state transitions to generate transaction inputs. This implementation simplifies this by loading reference data from the database into memory during startup.
* **Reasoning**: This approach still provides the necessary data for generating compliant inputs while keeping the driver simple. At startup, the driver populates internal data structures by querying the database for essential reference data (e.g., customer and company identifiers). A useful side effect is the **decoupling** of the *data load* from the *benchmark load*: you don’t need to know the parameters used for the initial data load when executing the benchmark.

#### 3. Database Interaction and SQL Dialects
* **Deviation**: For complex inputs, this implementation passes data as **JSON** and uses database-specific functions to parse them (e.g., `jsonb_to_recordset` in PostgreSQL and `OPENJSON` in SQL Server). The TPC-E spec does not define inputs in this manner.
* **Reasoning**: Passing data as JSON simplifies the client-side code by avoiding the need to loop and bind dozens of individual parameters while also avoiding costly client-server roundtrips.

> ⚠️ **Disclaimer**: This tool is intended for performance analysis and comparison — not for generating officially audited TPC-E results. The workload and results generated by this implementation **do not constitute** an official TPC-E result and are **not comparable** to any published TPC-E benchmarks.
---

## Getting Started

### Prerequisites

* Java 21 or higher (build only)
* Apache Maven 3.5.x or higher (build only)
* Docker
* Git

### Setup

There are three steps necessary to run the benchmark:

1. Generate the data to load **into** the database.
2. Create the database schema **and** load the data.
3. Run the benchmark driver.

+*Step zero:* download the pre‑built binary from **Releases** and place it on your `PATH`.

## Quickstart

End‑to‑end example (PostgreSQL via local Docker):



```bash
# clone repo and launch database
git clone https://github.com/lneves/oltp1
cd oltp1/docker/postgresql
cp env.example .env
# Edit .env as appropriate, then:
docker compose up -d

# Generate minimal dataset
oltp1 egen -c 5000 -t 5000 -w 1 -o ./flat_out

# Initialize schema & load data (use env var for safety)
export OLTP1_PASSWORD='<password>'
oltp1 initdb -e pgsql -h localhost -U admin -d ./flat_out

# run 10 minutes with 50 clients 
oltp1 driver  --engine PGSQL --host localhost --clients 50 --duration 600
```

> **Having container start failures due to permissions?** See **[Troubleshooting — Docker data directory permissions](docker_troubleshooting.md)**.

## Data Generation

OLTP‑1 includes a port of the official data‑generation tool. To generate a dataset (example: 5,000 customers):
 
```bash
oltp1 egen -c 5000 -t 5000 -o flat_out
```
Expected runtime: ~5–10 minutes for 5,000 customers and 300 trade days.

More options:

| Option        | Default  | Description                                              |
| ------------- | -------- | -------------------------------------------------------- |
| `-c <number>` | 5000     | Customers generated for this instance                    |
| `-t <number>` | 5000     | Total customers in the full database                     |
| `-f <number>` | 500      | Scale factor (customers per 1 tpsE)                      |
| `-w <number>` | 300      | Initial workdays (8‑hour days) of trade history to load  |
| `-o <path>`   | flat_out | Output path for the generated files                      |

For a complete list and detailed explanations, see[egen.md](egen.md).

> Per TPC‑E, the minimum dataset has **5,000 customers** (Clause 2.6.1.2).

## Database Setup

You can either use the provided `docker-compose.yml` files to spin up a local database instance, or connect to your own pre‑configured server.

Pre‑configured Docker environments are provided for:
- PostgreSQL 17
- Microsoft SQL Server 2022
- OrioleDB
- MariaDB

> To use an existing server, skip Docker and ensure your database is accessible with appropriate permissions.

To use Docker:

```bash
git clone https://github.com/lneves/oltp1
cd ./oltp1/docker/<db_engine>
cp env.example .env
# Edit .env as appropriate, then:
docker compose up -d
```

Load the schema and data:

```bash
oltp1 initdb -e <engine> -h localhost -U <db_user> -P <db_user_password> -d ./flat_out
```

The `initdb` operation will:
1. Create the database and schema
2. Load the generated data files
3. Create indexes and constraints
4. Tweak database configurations

*Depending on dataset size and hardware, this can take from a few minutes to several hours.*

Run `oltp1 initdb -h` to see the available options

## Running the Benchmark Driver

Use the `driver` action to execute the benchmark against your database.

Run `oltp1 driver -h` to see the available options. Example (10‑minute run, 50 clients, PostgreSQL on localhost):

```bash
oltp1 driver \
    --host localhost \
    --port 5432 \
    --user "<db_user>" \
    --password "<db_user_password>" \
    --engine PGSQL \
    --clients 50 \
    --duration 600
```

## Benchmark Details

### Transaction Mix

The workload is designed to follow the TPC-E specification's transaction mix closely:

| Transaction         | Mix Percentage |
| ------------------- | :------------: |
| Trade-Status        |     19.0%      |
| Market-Watch        |     18.0%      |
| Security-Detail     |     14.0%      |
| Customer-Position   |     13.0%      |
| Trade-Order         |     10.1%      |
| Trade-Result        |     10.0%      |
| Trade-Lookup        |      8.0%      |
| Broker-Volume       |      4.9%      |
| Trade-Update        |      2.0%      |
| Market-Feed         |      1.0%      |
| Data-Maintenance    |  ≈1 per minute |
| Trade-Cleanup       |  once at start |

> *Note*: `Trade-Result` and `Market-Feed` are triggered by `Trade-Order` and their percentages are included for completeness.

#### Sample Output

```text
#SUT

PostgreSQL 17.5 (Debian 17.5-1.pgdg120+1) on x86_64-pc-linux-gnu, compiled by gcc (Debian 12.2.0-14) 12.2.0, 64-bit

Date: 2025-08-02T22:27:41.901353

                                                            ----------- Response Time(ms) ---------- 
Transaction          Target(%) Actual(%)  Rate(tx/sec)     Mean   StdDev      Min      Max    Pct90   
Broker-Volume             4.90      4.81         41.34    27.21    15.18    15.09   246.50    33.95   
Customer-Position        13.00     12.88        110.65    33.99    15.18    19.39   316.39    40.74   
Market-Watch             18.00     18.45        158.53    23.04    12.46    13.22   282.99    26.72   
Security-Detail          14.00     13.84        118.92    57.19    19.59    41.90   361.53    71.85   
Trade-Lookup              8.00      8.13         69.90    51.66    18.87    16.11   370.30    61.90   
Trade-Status             19.00     18.55        159.38    28.04    12.78    19.35   268.15    30.37   
Trade-Order              10.10     10.10         86.76   157.64    59.02    54.49   521.93   218.05   
Trade-Result             10.00     10.25         88.05   100.26    26.13    59.71   446.88   115.44   
Market-Feed               1.00      1.03          8.83    50.45    14.96    23.13   116.38    69.13   
Trade-Update              2.00      1.98         16.98    78.41    15.70    44.64   265.67    91.44   
Data-Maintenance          0.00      0.00          0.03    32.12     0.00    32.12    32.12    32.12   

Run time: 34 sec.
Clients: 40
Total Tx: 29310
Tx Rate: 859.36 tx/sec
```

##### Understanding results

- **Tx Rate**: total transactions per second across all types
- **Mean/StdDev**: average response time and variance
- **Pct90**: 90% of transactions completed within this time
- **Actual%**: should closely match Target% for valid results

### Baseline query benchmark

The driver can run a *baseline query* benchmark that repeatedly executes a minimal query (e.g., `SELECT 0;`) to estimate the theoretical maximum throughput under ideal, low‑latency conditions. This establishes an upper bound without the overhead of complex queries, indexing, or data access.

Enable `--baseline` to run the minimal query. Useful for:

- Network + DB stack benchmarking
- Detecting raw throughput limits
- Comparing environments under near‑zero workload

## Contributing

Contributions to OLTP-1 are welcome! Please open an issue or submit a pull request.


## License

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt) 


## Related Resources

* **[TPC-E Standard Specification v1.14.0](https://www.tpc.org/tpc_documents_current_versions/pdf/tpc-e_v1.14.0.pdf)**  
   Official specification for compliance reference and implementation details

* **[From A to E: Analyzing TPC's OLTP Benchmarks](https://www.researchgate.net/publication/262275971_From_A_to_E_Analyzing_TPC's_OLTP_Benchmarks_--_The_obsolete_the_ubiquitous_the_unexplored)**  
   Academic paper explaining TPC-E adoption challenges and comparing it with TPC-C

* **[Percona TPC-E MySQL](https://github.com/Percona-Lab/tpce-mysql)**  
   MySQL/HANA implementation

* **[DBT-5: An Open-Source TPC-E Implementation](https://cai.type.sk/content/2010/5/dbt-5-an-open-source-tpc-e-implementation-for-global-performance-measurement-of-computer-systems/1870.pdf)**  
   Paper describing design decisions in another [open-source implementation](https://github.com/osdldbt/dbt5)

* **[TPC-E vs. TPC-C: I/O Comparison Study](http://muratbuffalo.blogspot.com/2023/01/tpc-e-vs-tpc-c-characterizing-new-tpc-e.html)**  
   Technical analysis of why TPC-C remains more popular despite TPC-E's advantages
