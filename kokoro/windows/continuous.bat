@echo on
cd github\google-cloud-eclipse

pushd plugins\com.google.cloud.tools.eclipse.integration.appengine\test-projects
for %%i in (*.zip) do jar xvf %%i

for /r . %%i in (pom.xml) do if exist %%i (
    echo %%i
    echo %%~fi
    echo %%~dpnxi
    mvn -f "%%i" package
    mvn -f %%i package)
dir /s
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
