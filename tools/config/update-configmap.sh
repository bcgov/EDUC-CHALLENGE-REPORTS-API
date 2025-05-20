envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
COMMON_NAMESPACE=$4
EDX_NAMESPACE=$5
GRAD_NAMESPACE=$6
COREG_NAMESPACE=$7
APP_NAME_UPPER=${APP_NAME^^}
DB_JDBC_CONNECT_STRING=$8
DB_PWD=$9
DB_USER=${10}
SPLUNK_TOKEN=${11}
CHES_CLIENT_ID=${12}
CHES_CLIENT_SECRET="${13}"
CHES_TOKEN_URL="${14}"
CHES_ENDPOINT_URL="${15}"
TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca

SOAM_KC_LOAD_USER_ADMIN=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
NATS_URL="nats://nats.${COMMON_NAMESPACE}-${envValue}.svc.cluster.local:4222"

echo Fetching SOAM token
TKN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=$SOAM_KC_LOAD_USER_ADMIN" \
  -d "password=$SOAM_KC_LOAD_USER_PASS" \
  -d "grant_type=password" \
  "https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" | jq -r '.access_token')

echo
echo Retrieving client ID for challenge-reports-api-service
CHALLENGE_REPORTS_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="challenge-reports-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for challenge-reports-api-service
CHALLENGE_REPORTS_APIServiceClientSecret=$([ -n "$CHALLENGE_REPORTS_APIServiceClientID" ] && curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$CHALLENGE_REPORTS_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')

echo
echo Removing CHALLENGE_REPORTS API client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$CHALLENGE_REPORTS_APIServiceClientID" \
  -H "Authorization: Bearer $TKN"

echo Writing scope WRITE_CHALLENGE_REPORTS
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write challenge reports\",\"id\": \"WRITE_CHALLENGE_REPORTS\",\"name\": \"WRITE_CHALLENGE_REPORTS\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo Writing scope READ_CHALLENGE_REPORTS
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read challenge reports\",\"id\": \"READ_CHALLENGE_REPORTS\",\"name\": \"READ_CHALLENGE_REPORTS\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

if [[ -n "$CHALLENGE_REPORTS_APIServiceClientID" && -n "$CHALLENGE_REPORTS_APIServiceClientSecret" && ("$envValue" = "dev" || "$envValue" = "test") ]]; then
  echo
  echo Creating client challenge-reports-api-service with secret
  curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TKN" \
    -d "{\"clientId\" : \"challenge-reports-api-service\",\"secret\" : \"$CHALLENGE_REPORTS_APIServiceClientSecret\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"READ_INSTITUTE_CODES\", \"READ_SCHOOL\", \"READ_DISTRICT\",\"READ_SDC_SCHOOL_COLLECTION_STUDENT\",\"READ_SDC_COLLECTION\",\"COREG_READ_COURSE\",\"READ_EDX_USERS\",\"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"
else
  echo
    echo Creating client challenge-reports-api-service without secret
    curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TKN" \
      -d "{\"clientId\" : \"challenge-reports-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"READ_INSTITUTE_CODES\", \"READ_SCHOOL\", \"READ_DISTRICT\",\"READ_SDC_SCHOOL_COLLECTION_STUDENT\",\"READ_SDC_COLLECTION\",\"COREG_READ_COURSE\",\"READ_EDX_USERS\", \"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"
fi

echo
echo Retrieving client ID for challenge-reports-api-service
CHALLENGE_REPORTS_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="challenge-reports-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for challenge-reports-api-service
CHALLENGE_REPORTS_APIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$CHALLENGE_REPORTS_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')

echo
echo Writing scope READ_CHALLENGE_REPORTS_CODES
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Challenge Reports Codes\",\"id\": \"READ_CHALLENGE_REPORTS_CODES\",\"name\": \"READ_CHALLENGE_REPORTS_CODES\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"


###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"

THREADS_MIN_SUBSCRIBER=8
THREADS_MAX_SUBSCRIBER=12

MAXIMUM_DB_POOL_SIZE=15
MINIMUM_IDLE_DB_POOL_SIZE=10

EMAIL_TEMPLATE_PRELIMINARY_SAMPLE_STAFF="<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\"><head><meta charset=\"ISO-8859-1\"><title>Funding Reports for Successful Course Challenges</title></head><body>Dear Superintendent,<br><br>RE: Funding Reports for Successful Course Challenges<br><br>The Ministry of Education provides school districts with funding equivalent to a one-credit course for each successfully completed course challenge. The funding is intended to cover the costs of providing challenges to students in the Graduation Program.<br><br>Each successful challenge will be funded at the rate of <span th:text=\"\${$}{fundingRate}\"></span>. Once confirmed, the funding amount will be provided to your district by the Resource Management Division after they complete the funding recalculation in December. It will be included as part of subsequent bi-monthly electronic funds transfers.<br><br>The Preliminary Report for the <span th:text=\"\${$}{schoolYear}\"></span> school year is available for your school district to download through the <a href=\"https://educationdataexchange.gov.bc.ca\">Education Data Exchange</a>.<br><br>The report for your district shows all students who, according to our records, successfully completed a course challenge during the <span th:text=\"\${$}{schoolYear}\"></span> school year. Please review the report to confirm these numbers match with your own. Any discrepancies should be reported to the Student Certification Unit (Student.Certification@gov.bc.ca) by <span th:text=\"\${$}{finalDateForChanges}\"></span>.<br><br>Sincerely,<br><br><span th:text=\"\${$}{executiveDirectorName}\"></span><br>Executive Director<br>Student Information, Data and Education Network Services<br>pc: <span th:text=\"\${$}{resourceManagementDirectorName}\"></span>, Director, Resource Management Division</body></html>"
EMAIL_SUBJECT_PRELIMINARY_SAMPLE_STAFF="PREVIEW - Preliminary Funding Report for Course Challenges"
EMAIL_FROM_PRELIMINARY_SAMPLE_STAFF="challenge-reports-noreply@gov.bc.ca"
EMAIL_TO_PRELIMINARY_SAMPLE_STAFF="student.certification@gov.bc.ca"

EMAIL_TEMPLATE_FINAL_SAMPLE_STAFF="<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\"><head><meta charset=\"ISO-8859-1\"><title>Final Funding Reports for Successful Course Challenges</title></head><body>Dear Superintendent,<br><br>RE: Final Funding Reports for Successful Course Challenges<br><br>The Ministry of Education and Child Care provides school districts with funding equivalent to a one-credit course for each successfully completed course challenge. The funding is intended to cover the costs of providing challenges to students in the Graduation Program.<br><br>With our email on <span th:text=\"\${$}{preliminaryStageCompletionDate}\"></span>, preliminary reports were provided to you for verification that your school district’s record of successful course challenges matched the number captured in your data submitted to the Ministry. The deadline for addressing discrepancies was <span th:text=\"\${$}{finalDateForChanges}\"></span>.<br><br>The Final Report for the <span th:text=\"\${$}{schoolYear}\"></span> school year is available for your school district to download through the <a href=\"https://educationdataexchange.gov.bc.ca\">Education Data Exchange</a>.<br><br>The report for your district shows all students eligible for funding who, according to our records, successfully completed a course challenge during the <span th:text=\"\${$}{schoolYear}\"></span> school year. Each successful challenge will be funded at the rate of <span th:text=\"\${$}{fundingRate}\"></span>. This funding will be provided to your district by the ministry’s Resource Management Division once they complete the funding recalculation in December. It will be included as part of subsequent bi-monthly electronic fund transfers.<br><br>Please contact the Student Certification Unit (student.certification@gov.bc.ca) with any questions.<br><br>Sincerely,<br><br><span th:text=\"\${$}{executiveDirectorName}\"></span><br>Executive Director<br>Student Information, Data and Education Network Services<br>pc: <span th:text=\"\${$}{resourceManagementDirectorName}\"></span>, Director, Resource Management Division</body></html>"
EMAIL_SUBJECT_FINAL_SAMPLE_STAFF="PREVIEW - Final Funding Report for Course Challenges"
EMAIL_FROM_FINAL_SAMPLE_STAFF="challenge-reports-noreply@gov.bc.ca"
EMAIL_TO_FINAL_SAMPLE_STAFF="student.certification@gov.bc.ca"

if [ "$envValue" = "dev" ]
then
  EMAIL_TO_PRELIMINARY_SAMPLE_STAFF="marco.1.villeneuve@gov.bc.ca"
  EMAIL_TO_FINAL_SAMPLE_STAFF="marco.1.villeneuve@gov.bc.ca"
elif [ "$envValue" = "test" ]
then
  EMAIL_TO_PRELIMINARY_SAMPLE_STAFF="marco.1.villeneuve@gov.bc.ca"
  EMAIL_TO_FINAL_SAMPLE_STAFF="marco.1.villeneuve@gov.bc.ca"
fi

echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap --from-literal=EMAIL_TEMPLATE_FINAL_SAMPLE_STAFF="$EMAIL_TEMPLATE_FINAL_SAMPLE_STAFF" --from-literal=EMAIL_SUBJECT_FINAL_SAMPLE_STAFF="$EMAIL_SUBJECT_FINAL_SAMPLE_STAFF" --from-literal=EMAIL_FROM_FINAL_SAMPLE_STAFF="$EMAIL_FROM_FINAL_SAMPLE_STAFF" --from-literal=EMAIL_TO_FINAL_SAMPLE_STAFF="$EMAIL_TO_FINAL_SAMPLE_STAFF" --from-literal=EMAIL_TEMPLATE_PRELIMINARY_SAMPLE_STAFF="$EMAIL_TEMPLATE_PRELIMINARY_SAMPLE_STAFF" --from-literal=EMAIL_SUBJECT_PRELIMINARY_SAMPLE_STAFF="$EMAIL_SUBJECT_PRELIMINARY_SAMPLE_STAFF" --from-literal=EMAIL_FROM_PRELIMINARY_SAMPLE_STAFF="$EMAIL_FROM_PRELIMINARY_SAMPLE_STAFF" --from-literal=EMAIL_TO_PRELIMINARY_SAMPLE_STAFF="$EMAIL_TO_PRELIMINARY_SAMPLE_STAFF" "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL="$DB_JDBC_CONNECT_STRING" --from-literal=DB_USERNAME="$DB_USER" --from-literal=DB_PASSWORD="$DB_PWD" --from-literal=STUDENT_API_URL="http://student-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=INSTITUTE_API_URL="http://institute-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/institute" --from-literal=COREG_API_URL="http://coreg-api-master.$COREG_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/course/information" --from-literal=CHES_CLIENT_ID="$CHES_CLIENT_ID" --from-literal=CHES_CLIENT_SECRET="$CHES_CLIENT_SECRET" --from-literal=CHES_TOKEN_URL="$CHES_TOKEN_URL" --from-literal=CHES_ENDPOINT_URL="$CHES_ENDPOINT_URL" --from-literal=GRAD_STUDENT_API_URL="http://educ-grad-student-api.$GRAD_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=EDX_API_URL="http://edx-api-master.$EDX_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/edx" --from-literal=SDC_API_URL="http://student-data-collection-api-master.$EDX_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student-data-collection" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=SPRING_JPA_SHOW_SQL="false" --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=TOKEN_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=NATS_URL=$NATS_URL --from-literal=CLIENT_ID="challenge-reports-api-service" --from-literal=CLIENT_SECRET="$CHALLENGE_REPORTS_APIServiceClientSecret" --from-literal=THREADS_MIN_SUBSCRIBER="$THREADS_MIN_SUBSCRIBER" --from-literal=THREADS_MAX_SUBSCRIBER="$THREADS_MAX_SUBSCRIBER" --from-literal=MAXIMUM_DB_POOL_SIZE="$MAXIMUM_DB_POOL_SIZE" --from-literal=MINIMUM_IDLE_DB_POOL_SIZE="$MINIMUM_IDLE_DB_POOL_SIZE" --dry-run -o yaml | oc apply -f -

echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" set env --from=configmap/$APP_NAME-config-map deployment/$APP_NAME-$SOAM_KC_REALM_ID

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -
