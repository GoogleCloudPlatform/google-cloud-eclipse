@echo on
rem Tycho 2.5.0+ requires Java 11+
curl --silent --show-error --location --output openjdk-17.zip "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2+8/OpenJDK17U-jre_x64_windows_hotspot_17.0.2_8.zip"
unzip -q -d "c:\Program Files\openjdk" openjdk-17.zip
@rem need to update toolchains.xml with JRE path
set "JAVA_HOME=c:\Program Files\openjdk\jdk-17.0.2+8-jre"
set "PATH=%JAVA_HOME%\bin;%PATH%"
@echo JAVA_HOME: %JAVA_HOME%
java -version

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

mvn -V -B -N io.takari:maven:wrapper -Dmaven=3.8.4
call mvnw.cmd --toolchains=kokoro/windows/toolchains.xml -V -B --fail-at-end -Pci-build verify
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
