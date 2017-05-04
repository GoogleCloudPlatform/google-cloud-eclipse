cd github/google-cloud-eclipse

set CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
call gcloud.cmd components update --quiet
call gcloud.cmd components install app-engine-java --quiet

mvn -B -N io.takari:maven:wrapper -Dmaven=3.5.0
mvnw.cmd -B --fail-at-end -Ptravis -Declipse.target=oxygen verify

exit /b %ERRORLEVEL%
