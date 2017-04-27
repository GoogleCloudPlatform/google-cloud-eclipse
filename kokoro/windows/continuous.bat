cd github/google-cloud-eclipse

set CLOUDSDK_CORE_DISABLE_USAGE_REPORTING=true
wget https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe
start /WAIT GoogleCloudSDKInstaller.exe /S /noreporting /nostartmenu /nodesktop /logtofile /D=T:\google
call t:\google\google-cloud-sdk\bin\gcloud.cmd components copy-bundled-python>>python_path.txt && SET /p CLOUDSDK_PYTHON=<python_path.txt && DEL python_path.txt
call t:\google\google-cloud-sdk\bin\gcloud.cmd components update --quiet
call t:\google\google-cloud-sdk\bin\gcloud.cmd components install app-engine-java --quiet
set GOOGLE_CLOUD_SDK_HOME=t:\google\google-cloud-sdk

wget http://www-us.apache.org/dist/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.zip
unzip apache-maven-3.5.0-bin.zip

apache-maven-3.5.0/bin/mvn --fail-at-end -Ptravis -Declipse.target=oxygen verify

exit /b %ERRORLEVEL%
