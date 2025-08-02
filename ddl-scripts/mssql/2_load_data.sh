#!/usr/bin/env bash
set -eux
cd $(dirname $0)

source db.properties

export TPCE_PASSWORD=$TPCE_PASSWORD

BCP="/opt/mssql-tools/bin/bcp"
BCP_ARGS="-c -t| -S ${DB_SERVER_HOST} -U tpce -P ${TPCE_PASSWORD}"


$BCP tpce.dbo.financial2 in /home/lneves/tst/tpc-e-tool/flat_out_5000/Financial.txt $BCP_ARGS

#/opt/mssql-tools/bin/bcp tpce.dbo.financial2 in /home/lneves/tst/tpc-e-tool/flat_out_5000/Financial.txt -c -t$'\|' -S localhost -U sa -P $'!QAZ2wsx'