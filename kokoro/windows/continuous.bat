@echo on

cd github/google-cloud-eclipse

set CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
REM get -nv https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe
REM tart /WAIT GoogleCloudSDKInstaller.exe /S /noreporting /nostartmenu /nodesktop /logtofile /D=T:\google
REM call t:\google\google-cloud-sdk\bin\gcloud.cmd components copy-bundled-python>>python_path.txt && SET /p CLOUDSDK_PYTHON=<python_path.txt && DEL python_path.txt
REM all t:\google\google-cloud-sdk\bin\gcloud.cmd components update --quiet
REM all t:\google\google-cloud-sdk\bin\gcloud.cmd components install app-engine-java --quiet
REM set GOOGLE_CLOUD_SDK_HOME=C:\Program Files (x86)\Google\Cloud SDK\google-cloud-sdk
call gcloud.cmd components update --quiet
call gcloud.cmd components install app-engine-java --quiet

echo "========================================================================="
echo "PATH:"
echo %PATH%
echo "========================================================================="

REM wget -nv http://www-us.apache.org/dist/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.zip
REM unzip -q apache-maven-3.5.0-bin.zip

mvn --version
mvn -B --fail-at-end -Ptravis -Declipse.target=oxygen verify
REM apache-maven-3.5.0/bin/mvn -B --fail-at-end -Ptravis -Declipse.target=oxygen verify

exit /b %ERRORLEVEL%
