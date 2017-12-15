@echo on

echo %HOMEPATH%
dir %HOMEPATH%
dir %HOMEPATH%/.m2

pushd %HOMEPATH%
call gsutil -q cp "gs://ct4e-m2-repositories/m2-oxygen.tar" .
echo on
tar xf m2-oxygen.tar
rename m2-oxygen .m2
dir
popd

cd github/google-cloud-eclipse

rem Pre-download all dependency JARs that test projects from the integration
rem test require to avoid the concurrent download issue:
rem https://github.com/GoogleCloudPlatform/google-cloud-eclipse/issues/2284
rem (Currently, this workaround is mostly not effective as we dump the M2 local
rem repo above that already caches the JARs for these projects, but we leave it
rem here in case we decided not to dump the local repo.)
pushd plugins\com.google.cloud.tools.eclipse.integration.appengine\test-projects
mkdir tmp-unzip-area
cd tmp-unzip-area
for %%i in (..\*.zip) do jar xf %%i
for /f %%i in ('dir /b /s pom.xml') do mvn -B -f "%%i" package)
cd ..
rmdir /s /q tmp-unzip-area
popd

set CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
call gcloud.cmd components update --quiet
@echo on
call gcloud.cmd components install app-engine-java --quiet
@echo on

mvn -B --settings kokoro\windows\m2-settings.xml ^
    -N io.takari:maven:wrapper -Dmaven=3.5.0
call mvnw.cmd -B --settings kokoro\windows\m2-settings.xml ^
              --fail-at-end -Ptravis -Declipse.target=oxygen verify
echo on

cd ..
echo %CD%
rmdir /s /q google-cloud-eclipse
dir

cd %HOMEPATH%
tar cf m2-oxygen-home.tar .m2
call gsutil cp m2-oxygen-home.tar "gs://ct4e-m2-repositories/"
echo on
rmdir /s /q .m2

exit /b %ERRORLEVEL%
