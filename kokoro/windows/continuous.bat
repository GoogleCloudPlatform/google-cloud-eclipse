@echo on
rem Tycho 1.0.0 does not support Java 9
set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_152"

rem To speed up build, download and unpack an M2 repo cache.
pushd %USERPROFILE%
call gsutil.cmd -q cp "gs://ct4e-m2-repositories-for-kokoro/m2-cache.tar" .
@echo on
tar xf m2-cache.tar && del m2-cache.tar
popd

cd github\google-cloud-eclipse

set CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
call gcloud.cmd components update --quiet
@echo on
call gcloud.cmd components install app-engine-java --quiet
@echo on

mvn -V -B -N io.takari:maven:wrapper -Dmaven=3.5.0
call mvnw.cmd -V -B --fail-at-end -Ptravis verify
set MAVEN_BUILD_EXIT=%ERRORLEVEL%
@echo on

rem Delete files under "T:\src" to make Kokoro exit quickly. "rsync" will be
rem performed on "T:\src" to pull back everything (Bug 74837748).

rem First delete regular files.
for /f %%i in ('dir /b /a:-D') do del %%i
rem Delete directories, but leave "kokoro" where this batch script lives.
for /f %%i in ('dir /b /a:D ^| findstr /v kokoro') do rmdir /s /q %%i
rem Clean "T:\src\gfile" too.
rmdir /s /q %KOKORO_GFILE_DIR%

exit /b %MAVEN_BUILD_EXIT%
