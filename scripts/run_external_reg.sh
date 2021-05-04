#!/bin/bash

# This script is for executing a double check that the endpoint calls that work directly
# also work via okapi.

# see https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html for info on overriding 
# spring boot app config on the command line

echo Start mod-ciim in external-register mode

# curl --header "X-Okapi-Tenant: diku" http://localhost:9130/content -X GET

the_jar_file=`ls build/libs/mod-ciim*.jar | tail -n 1`

echo Attempting to start jar $the_jar_file

# THis DOES work as expected however - 
java -jar $the_jar_file -Xmx1G --grails.server.host=10.0.2.2 --dataSource.username=folio_admin --dataSource.password=folio_admin --dataSource.url=jdbc:postgresql://localhost:54321/okapi_modules

