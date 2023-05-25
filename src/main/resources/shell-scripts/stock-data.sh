#!/bin/bash

LOG_DATE=$(date +%Y%m%d)
SCRIPT_DIR=$(dirname $0)
ENV_FILE="${HOME}/conf/env.sh"
. ${ENV_FILE}

LOG_FILE="${SCRIPT_DIR}/logs/stock-data-data-${LOG_DATE}.log"

rm -f ${LOG_FILE}
rm -f nohup.out

HISTORICAL_STOCK_PRICES_OUTPUT_FILE=${SCRIPT_DIR}/historical-stock-prices-output.txt

export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/${ENV_DB_NAME}?rewriteBatchedStatements=true"
export SPRING_DATASOURCE_USERNAME=${ENV_DB_USERNAME}
export SPRING_DATASOURCE_PASSWORD=${ENV_DB_PASSWORD}
export DOWNLOAD_DIR_LOC="${SCRIPT_DIR}/downloads"
export HISTORICAL_STOCK_PRICES_OUTPUT_LOC=${HISTORICAL_STOCK_PRICES_OUTPUT_FILE}
export NO_DAYS_BACK=7
RUN_TYPE=$1

nohup java -jar -DXmx512M -Dlogging.file.name=${LOG_FILE} -Dlogging.level.anshuman=INFO -DcleanDir=true ${SCRIPT_DIR}/niivostockdata-1.0.jar ${RUN_TYPE} &
find ${SCRIPT_DIR}/logs -name '*.log' -mtime +10 -type f -exec rm -f {} +

if [ -f "${HISTORICAL_STOCK_PRICES_OUTPUT_FILE}" ]; then
    mysql --local-infile=1  -u${ENV_DB_USERNAME} -p${ENV_DB_PASSWORD} ${ENV_DB_NAME} -e "LOAD DATA LOCAL INFILE '${HISTORICAL_STOCK_PRICES_OUTPUT_FILE}' INTO TABLE historical_stock_prices FIELDS TERMINATED BY '|' LINES TERMINATED BY '\n' IGNORE 0 LINES"
    rm -f ${HISTORICAL_STOCK_PRICES_OUTPUT_FILE}
fi