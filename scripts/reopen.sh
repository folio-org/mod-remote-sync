#!/bin/bash

AUTH_TOKEN=$(curl -sSL -D - -X POST -H 'accept: application/json' -H 'Content-type: application/json' \
    -H "X-Okapi-Tenant: diku" --connect-timeout 5 --max-time 30 -d "{ \"username\":\"diku_admin\", \"password\": \"admin\" }" \
    "https://okapi.testing.dev.folio.finc.info/authn/login" | grep -Fi x-okapi-token | sed -r 's/^.*\:\s*(([A-Za-z0-9+\/]+\.){2}[A-Za-z0-9+\/]+)/\1/' | xargs)

echo auth token: $AUTH_TOKEN

echo Attempt to get blocked record
curl -sSL -XGET -H "X-Okapi-Token: ${AUTH_TOKEN}" -H 'accept: application/json' -H 'Content-type: application/json' -H "X-Okapi-Tenant: diku" --connect-timeout 5 --max-time 30 "https://okapi.testing.dev.folio.finc.info/remote-sync/records/8ed55611-6c77-4fe9-92f7-0c45920d42ae"

echo Attempt to unblock
curl -sSL -XPUT -H "X-Okapi-Token: ${AUTH_TOKEN}" -H 'accept: application/json' \
    -H 'Content-type: application/json' \
    -H "X-Okapi-Tenant: diku" \
    --connect-timeout 50 \
    --max-time 90 \
    -d '{ "id":"8ed55611-6c77-4fe9-92f7-0c45920d42ae", "processControlStatus":"OPEN" }' \
    "https://okapi.testing.dev.folio.finc.info/remote-sync/records/8ed55611-6c77-4fe9-92f7-0c45920d42ae"


# https://okapi.testing.dev.folio.finc.info/remote-sync/records/8ed55611-6c77-4fe9-92f7-0c45920d42ae
