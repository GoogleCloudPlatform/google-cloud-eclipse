@echo on
cd github\google-cloud-eclipse

pushd plugins\com.google.cloud.tools.eclipse.integration.appengine\test-projects
for %%i in (*.zip) do jar xf %%i
rem for /r . %%i in (pom.xml) do if exist %%i mvn -B -q -f "%%i" package
for /f %%i in ('dir /b /s pom.xml') do echo "%%i"
echo #####################
for /f "delims=" %%i in ('dir /b /s pom.xml') do echo "%%i"
echo #####################
for /f "delims=" %%i in ('dir /b /s pom.xml') do mvn -B -q -f "%%i" package
popd

exit /b %ERRORLEVEL%
