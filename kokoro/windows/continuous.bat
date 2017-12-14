@echo on
cd github\google-cloud-eclipse

rem Pre-download all dependency JARs that test projects of the integration test
rem requires to avoid https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2284
pushd plugins\com.google.cloud.tools.eclipse.integration.appengine\test-projects
for %%i in (*.zip) do jar xf %%i
for /r . %%i in (pom.xml) do @if exist %%i mvn -B -f "%%i" package
popd

set CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
call gcloud.cmd components update --quiet
@echo on
call gcloud.cmd components install app-engine-java --quiet
@echo on

mvn -B --settings kokoro\windows\m2-settings.xml ^
    -N io.takari:maven:wrapper -Dmaven=3.5.0

mvnw.cmd -B --settings kokoro\windows\m2-settings.xml ^
         --fail-at-end -Ptravis -Declipse.target=oxygen verify

exit /b %ERRORLEVEL%
