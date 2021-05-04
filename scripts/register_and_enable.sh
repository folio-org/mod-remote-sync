BASEDIR=$(dirname "$0")
# echo Please make sure you have run ./gradlew clean generateDescriptors before starting this script
pushd "$BASEDIR/../service"

# Check for decriptor target directory.

DESCRIPTORDIR="build/resources/main/okapi"

if [ ! -d "$DESCRIPTORDIR" ]; then
    echo "No descriptors found. Let's try building them."
    ./gradlew generateDescriptors
fi

DEP_DESC=`cat ${DESCRIPTORDIR}/DeploymentDescriptor.json`
SVC_ID=`echo $DEP_DESC | jq -rc '.srvcId'`
INS_ID=`echo $DEP_DESC | jq -rc '.instId'`

echo Delete previous install

curl -XDELETE "http://localhost:9130/_/proxy/tenants/diku/modules/${SVC_ID}"
curl -XDELETE "http://localhost:9130/_/discovery/modules/${SVC_ID}/${INS_ID}"
curl -XDELETE "http://localhost:9130/_/proxy/modules/${SVC_ID}"

echo Install new module
# ./gradlew clean generateDescriptors
curl -XPOST 'http://localhost:9130/_/proxy/modules' -d @"${DESCRIPTORDIR}/ModuleDescriptor.json"
curl -XPOST 'http://localhost:9130/_/discovery/modules' -d "$DEP_DESC"
curl -XPOST 'http://localhost:9130/_/proxy/tenants/diku/install?tenantParameters=loadSample%3Dtest,loadReference%3Dother' -d `echo $DEP_DESC | jq -c '[{id: .srvcId, action: "enable"}]'`
popd

