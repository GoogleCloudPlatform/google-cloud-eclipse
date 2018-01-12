@echo on
rem Tycho 1.0.0 does not support Java 9
set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_152"

rem To speed up build, download and unpack an M2 repo cache.
pushd %USERPROFILE%
call gsutil -q cp "gs://ct4e-m2-repositories/m2-oxygen.tar" .
echo on
tar xf m2-oxygen.tar && del m2-oxygen.tar
popd

cd github\google-cloud-eclipse

rem Pre-download all dependency JARs that test projects from the integration
rem test require to avoid the concurrent download issue:
rem https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2284
pushd plugins\com.google.cloud.tools.eclipse.integration.appengine\test-projects
mkdir tmp-unzip-area
cd tmp-unzip-area
for %%i in (..\*.zip) do jar xf %%i
for /f %%i in ('dir /b /s pom.xml') do mvn -B -q -f "%%i" package
cd ..
rmdir /s /q tmp-unzip-area
popd

set CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
call gcloud.cmd components update --quiet
@echo on
call gcloud.cmd components install app-engine-java --quiet
@echo on

mvn -V -B --settings kokoro\windows\m2-settings.xml ^
    -N io.takari:maven:wrapper -Dmaven=3.5.0
mvnw.cmd -V -B --settings kokoro\windows\m2-settings.xml ^
         --fail-at-end -Ptravis -Declipse.target=oxygen verify

exit /b %ERRORLEVEL%
