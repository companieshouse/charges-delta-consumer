#!/bin/bash

# Start script for charges-delta-consumer-service

PORT=8080

exec java -jar -Dserver.port="${PORT}" -XX:MaxRAMPercentage=80 "charges-delta-consumer.jar"