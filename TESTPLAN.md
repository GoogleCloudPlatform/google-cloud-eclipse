Manual testing of the GCloud Plugin for Eclipse.

# Install the plugin

1. Launch Eclipse 4.5.

2. If necessary, remove the old version of the plugin.
  1. Help > Installation details
  2. Click "Google Cloud Platform for Eclipse 4.5 and 4.6". (If this is not present,
    move on to step 3 below.)
  3. Click the Uninstall... button.
  4. Click the Finish button.
  5. Click the "Yes" button in the dialog that prompts you to restart Eclipse.
2. ????
3. Restart Eclipse
4. Check installation details. 

# Create a project

1. File > New > Project...
1. In the Wizards: text field begin typing "Goo".  
   "Google App Engine Standard Project" should 
   appear in a "Google Cloud Platform" folder.
   Also check that the App Engine logo (a cartoon plane) also appears.
1. Click the "Next >" button.
1. The New App Engine Standard Project dialog appears.
1. Verify that the "Finish" button is disabled.
1. Verify that "Use default location" button is checked.
1. Into the project name field, type "foo". If you see 
   "A project with that name already exists in the workspace."
   try some other name. 
1. Verify that the "Finish" button is now enabled.
1. In the "Java package:" field, type "com.google.testplan".
1. In the "App Engine Project ID" field type "testplan".
1. Click the "Finish" button.
1. Verify that a project named "foo" (or whatever name you picked in step 6)
   is now created in the package explorer. 
1. Verify that src/main/java and src/test/java both appear to be source roots.
1. Verify that src/main/java contains com.google.testplan.HelloAppEngineServlet
1. Check that the pom.xml contains the project ID testplan.
1. Check that there are no compile errors.
1. Check that the compiler level is 1.7.
1. Check that the servlet version is 2.5.

# Edit the project sample code

# Run the project

# Debug the project

# Deploy the project

