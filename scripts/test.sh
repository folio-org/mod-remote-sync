FOLIO_AUTH_TOKEN=`./okapi-login`

# Prepolpulate with data.
curl --header "X-Okapi-Tenant: diku" \
     --header "X-Okapi-Token: $FOLIO_AUTH_TOKEN" \
     --header "Content-Type: application/json" \
     --header "Accept: application/json" \
     http://localhost:9130/ciim/wibble

