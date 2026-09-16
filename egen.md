

# EGenLoader Parameter Guide

This guide provides a detailed explanation of the command-line options for the`EGenLoader` tool. The parameters are similar as the official TPC-E benchmark tool. Understanding these parameters is essential for correctly generating a compliant and scalable TPC-E database.

## Database Scale and Sizing

These parameters define the size and scale of the database you intend to generate.

-----

  * **`-t <number>` (Total Customers)**

      * **Default**: `5000`
      * This is the most important sizing parameter. It defines the **total logical size** of the entire database. The number of records in most other tables (like `COMPANY`, `SECURITY`, `ACCOUNT_PERMISSION`, etc.) is calculated based on this value.

  * **`-c <number>` (Instance Customers)**

      * **Default**: `5000`
      * This specifies the number of customers that **this specific instance** of the `EGenLoader` will generate. For a single-node load, this value should be the same as `-t`. For a parallel load across multiple nodes, this will be a fraction of `-t`.

  * **`-b <number>` (Beginning Customer)**

      * **Default**: `1`
      * This sets the starting customer ID for this loader instance. It's crucial for parallel data loading, as it tells each instance which "slice" of the total customer base to generate.

  * **`-f <number>` (Scale Factor)**

      * **Default**: `500`
      * This critical parameter enforces the TPC-E rule that links database size to performance. It sets the **number of customers required per 1 tpsE** (transactions per second) of measured throughput. This ensures that a higher-performing system is benchmarked against a proportionally larger database.

  * **`-w <number>` (Workdays)**

      * **Default**: `300`
      * This defines the number of 8-hour workdays to simulate for the initial trade population. Increasing this value will generate a larger volume of historical data in the "growing" tables like `TRADE` and `TRADE_HISTORY`.

## Input/Output and Load Type

These parameters control how the `EGenLoader` reads input files and writes its output.

-----

  * **`-o <dir>` (Output Directory)**

      * **Default**: `./flat_out`
      * Specifies the directory where the generated data files will be written when using the `FLAT` load type.

## Table Generation Control

These flags allow you to generate specific subsets of the tables, which is useful for regenerating parts of the database without starting from scratch.

-----

  * **`-x`**: The default behavior if no other flag is specified. Generates **all** tables.
  * **`-xf`**: Generates only the **fixed-size tables** (e.g., `TAXRATE`, `EXCHANGE`).
  * **`-xs`**: Generates only the **scaling tables**, whose size is proportional to the customer count (e.g., `CUSTOMER`, `COMPANY`).
  * **`-xg`**: Generates only the growing tables (TRADE, TRADE_HISTORY, SETTLEMENT, CASH_TRANSACTION, HOLDING, HOLDING_HISTORY, HOLDING_SUMMARY, BROKER).

## Practical Examples 💡

-----

#### Example 1: A Small, Single-Instance Load

This command generates a standard 5,000-customer database into flat files in the `./output_files/` directory.

```bash
oltp1 egen -t 5000 -c 5000 -o ./output_files/
```

#### Example 2: A Large-Scale Parallel Load

Imagine you need to generate a **20,000-customer** database using **4 parallel loader instances**. Each instance will be responsible for a 5,000-customer slice.

  * **Loader 1:**
    ```bash
    oltp1 egen -t 20000 -c 5000 -b 1 -o ./loader1_out/
    ```
  * **Loader 2:**
    ```bash
    oltp1 egen -t 20000 -c 5000 -b 5001 -o ./loader2_out/
    ```
  * **Loader 3:**
    ```bash
    oltp1 egen -t 20000 -c 5000 -b 10001 -o ./loader3_out/
    ```
  * **Loader 4:**
    ```bash
    oltp1 egen -t 20000 -c 5000 -b 15001 -o ./loader4_out/
    ```

In this setup, each instance knows the total scale is 20,000 customers (`-t`) but only generates its assigned 5,000-customer chunk (`-c`), starting at the correct position (`-b`).



