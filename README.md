[![unstable](http://badges.github.io/stability-badges/dist/unstable.svg)](http://github.com/badges/stability-badges)
[![Build Status](https://travis-ci.org/GoogleCloudPlatform/google-cloud-eclipse.svg?branch=master)](https://travis-ci.org/GoogleCloudPlatform/google-cloud-eclipse)


This project provides an Eclipse plugin for building, debugging, and deploying Google Cloud Platform applications.

[End user documentation and installation instructions can be found on cloud.google.com.](https://cloud.google.com/eclipse/docs/)

_TL;DR_: `mvn package` should
generate a p2-accessible repository in `gcp-repo/target/repository`.

# Development

This project is built using _Maven Tycho_, a set of extensions to
Maven for building Eclipse bundles and features.

## Requirements

1. The [Google Cloud SDK](https://cloud.google.com/sdk/); install
  this somewhere on your file system.

1. The [Eclipse IDE](https://www.eclipse.org/downloads/eclipse-packages/).
  It's easiest to use the _Eclipse IDE for Java EE Developers_ package. You can use
  Eclipse 4.6 (Neon) or 4.7 (Oxygen) as we define a target platform to build and run against.

  1. The [m2eclipse plugin](http://www.eclipse.org/m2e/) (also called m2e) is
     required to import the projects into Eclipse.  m2eclipse is included in
     [several packages](https://www.eclipse.org/downloads/compare.php?release=neon),
     such as the _Eclipse IDE for Java EE Developers_ package.

  2. The [m2e connector for maven-dependency-plugin](https://github.com/ianbrandt/m2e-maven-dependency-plugin)
     should be installed from `http://ianbrandt.github.io/m2e-maven-dependency-plugin/`.

1. Maven 3.5.0 or later.  Although m2eclipse is bundled with its own Maven install,
   Maven is necessary to test command-line builds.

1. JDK 7

1. git (optional: you can use EGit from within Eclipse instead)

1. Clone the project to a local directory using `git clone
   https://github.com/GoogleCloudPlatform/google-cloud-eclipse.git`.


## Configuring Maven/Tycho Builds

The plugin is built using Maven/Tycho and targeted to Java 7.

The tests need to find the Google Cloud SDK.  You can either:

  1. Place the _SDK_`/bin` directory on your `PATH`
  2. Set `GOOGLE_CLOUD_SDK_HOME` to point to your SDK

### Changing the Eclipse Platform compilation and testing target

By default, the build is targeted against Eclipse Mars / 4.5.
You can explicitly set the `eclipse.target` property to
`neon` (4.6) or `oxygen` (4.7).
```
$ mvn -Declipse.target=neon package
```

### Adding a new bundle/fragment

We normally put production code into a bundle and tests as a fragment hosted
by that bundle, put under the `plugins/` directory.
For now we have been committing both the `pom.xml` and Eclipse's
`.project`, `.classpath`, and `.settings/` files.

Our CI process is configured to run our tests with JaCoCo, which requires
some additional configuration to add new bundles and fragments
in `build/jacoco/`.


## Import into Eclipse

We pull in some dependencies directly from Maven-style repositories,
such as Maven Central and the Sonatype staging repository, which isn't
directly supported within Eclipse.  We have a few hoops to jump
through to set up a working development environment.

### Assemble the IDE Target Platform

The Eclipse IDE and Tycho both use a _Target Platform_ to manage
the dependencies for the source bundles and features under development.
Although Tycho can pull dependencies directly from Maven-style
repositories (like [Maven Central](https://search.maven.org)), Eclipse
cannot.  So we use Tycho to cobble together
a target platform suitable for the Eclipse IDE with the following command.
```
$ (cd eclipse; mvn package)        # may want -Declipse.target=XXX
```
This command creates a local copy of the
target platform, including any Maven dependencies, into
[`eclipse/ide-target-platform/target/repository`](eclipse/ide-target-platform/target/repository).
You will use this repository to create a target platform within the IDE,
as described below.

The Eclipse version used for the target platform is affected by the
`eclipse.target` property, described below.

You must regenerate the target platform and reconfigure the IDE's
target platform whenever dependencies are updated.

### Steps to import into the Eclipse IDE


1. Setup JDK 7 in Eclipse

  1. Select `Window/Preferences` (on Mac `Eclipse/Preferences`).

  1. Under `Java/Installed JREs` click `Add`.

  1. Select Standard VM and click `Next`.

  1. Select the folder that contains the JDK 7 installation by clicking
     `Directory`.

  1. Click `Finish`.

  1. Select `Java/Installed JREs/Execution Environments` page.

  1. Click on `JavaSE-1.7` in the list on the left under `Execution
     Environments:`.

  1. The JDK just added should show up in the list on the right along with other
     installed JDKs/JREs. Set the checkbox next the the JDK 7 added in the
     previous steps to mark it as compatible with the `JavaSE-1.7` execution
     environment.

  1. Click `OK`.

2. Set up the Target Platform: you will need to repeat this process whenever
   items are changed in the target platform, such as a new release of the
   `appengine-plugins-core`.

  0. As described above, you must first build the target platform with Maven:

     `$ mvn -Pide-target-platform package`

  1. Open the `Preferences` dialog, go to `Plug-in Development` > `Target Platform`.

  2. Click `Add...` > `Nothing` to create a new Target Platform.

  3. Name it `GCP IDE Target Platform`.

  4. Select `Add` > `Software Site`.

  5. Select the `Add...` button (found beside the `Work with:` field) and then select `Local`
     to find a local repository. Navigate to `.../eclipse/ide-target-platform/target/repository`,
     and click `OK`.

  6. Once the main content populates, check the `Uncategorized` item to pull in all items. Click `Finish`.

  7. Click `Finish` to complete the new target platform definition.

  8. Select your new target platform (instead of Running Platform) in the `Target Platform` preferences.

  9. Click `Apply and Finish` to load this new target platform.

  10. Eclipse will load the target.

3. Import the projects

  1. Select `File/Import...` menu in Eclipse.

  1. Select `Existing Maven Projects` from the list.

  1. Click `Browse...` and select the directory that
     contains the project.

  1. Under `Projects:` the `pom.xml` files representing modules should be
     displayed. Make sure that all of them are selected, and click `Finish`.

  1. Maven may prompt you to install several additional plugin connector plugins from
  [Tycho](https://eclipse.org/tycho/) if they are not already installed. Click
  `Finish` to install them. If Eclipse prompts you to install any other
  plugins, do so.

  1. Restart Eclipse when prompted.

4. Check the imported projects:
  1. There should be no errors in the `Markers` or `Problems` views in Eclipse.
    However you may see several low-priority warnings.

      1. You may see Maven-related errors like _"plugin execution not
         covered by lifecycle configuration"_.
         If so, right-click on the problem and select
         _Quick Fix_ > _Discover new m2e connectors_
	 and follow the process to install the recommended plugin
	 connectors.

5. Create and initialize a launch configuration:

  1. Right-click the `gcloud-eclipse-tools.launch` file under the
  `google-cloud-eclipse` module in the `Package Explorer`.

  1. Select `Run As` > `Run Configurations...`

  1. Set variables required for launch:

    1. Go to the second tab for `Arguments`

    1. Click the `Variables...` button for `VM argument:`

    1. Click the `Edit variables...` button

    1. Click `New...`

    1. Set the name to `oauth_id`, and the value to the value you want to use
    (description optional)

    1. Click `OK`, the variable will appear in the list

    1. Repeat steps 6-8 but use `oauth_secret` as the name and use the
    corresponding value

    1. Click `OK` to close the edit variables dialog

    1. Click `Cancel` to close the variable selection dialog

    1. Click `Apply` to apply the changes to the run config

  1. From the `Run` menu, select `Run History > gcloud-eclipse-tools`

  1. A new instance of Eclipse launches with the plugin installed.


# Updating Target Platforms

### Updating the `.target` files

We use _Target Platform_ files (`.target`) to collect the dependencies used
for the build.  These targets specify exact versions of the bundles and
features being built against. We currently maintain three target platforms,
targeting the latest version of the current, previous, and next releases.
This is currently:

  - Eclipse Mars (4.5 SR2): [`eclipse/mars/gcp-eclipse-mars.target`](eclipse/mars/gcp-eclipse-mars.target)
  - Eclipse Neon (4.6): [`eclipse/neon/gcp-eclipse-neon.target`](eclipse/neon/gcp-eclipse-neon.target)
  - Eclipse Oxygen (4.7): [`eclipse/oxygen/gcp-eclipse-oxygen.target`](eclipse/oxygen/gcp-eclipse-oxygen.target)

These `.target` files are generated and *should not be manually updated*.
Updating `.target` files directly becomes a chore once it has more than a
couple of dependencies.  We instead generate these `.target`s from
_Target Platform Definition_ `.tpd` files.
The `.tpd` files use a simple DSL to specify the bundles and features,
and the location of the repositories containing them.
The `.tpd` files are processed using the [TPD Editor](https://github.com/mbarbero/fr.obeo.releng.targetplatform)
which resolves the specified dependencies and creates a `.target`.
The process is:

  1. Install the TPD Editor, if necessary
     - Use _Help > Install New Software_ and specify `http://mbarbero.github.io/fr.obeo.releng.targetplatform/p2/latest/`
       as the location.
     - Restart Eclipse when prompted
  2. Open the `.tpd` file in Eclipse.
  3. Make any necessary changes and save.
     - Note that the TPDs specify artifacts using their _p2 identifiers_.
       Bundles are specified using their OSGi Bundle Symbolic Name (e.g.,
       `org.eclipse.core.runtime`).
       Features are specified using their Feature ID suffixed with `.feature.group`
       (e.g., `org.eclipse.rcp.feature.group`).
  4. Right-click in the editor and choose _Create Target Definition File_
     to update the corresponding .target file.

Both the `.tpd` and `.target` files should be committed.

### Updating Dependencies

The IDE Target Platform needs to be rebuilt at the command line
and reimported into Eclipse when dependency versions are changed:

1. `mvn -Pide-target-platform package`
2. Preferences > Plug-in Development > Target Platforms
3. Select your target ("GCP IDE Target Platform") and click Edit
4. Select the location and click Reload to cause any cached info to be discarded.
5. Click Edit and then select Uncategorized.
6. Finish / OK until done.

### Updating the Eclipse IDE Target Platforms

The IDE Target Platform, defined in `eclipse/ide-target-platform`,
may need to be updated when dependencies are added or removed.  The
contents are defined in the `category.xml` file, which specifies
the list of features and bundles that should be included.  This
file can be edited using the Category editor in Eclipse.  Ideally
the version should be specified as `"0.0.0"` to indicate that the
current version found should be used.  Unlike the `.tpd` file,
the identifiers are not p2 identifiers, and so features do not
require the `.feature.group` suffix.


### Configuring Maven/Tycho Toolchains for CI Builds

_Note: this section is only relevant for configuring CI builds_

We use Tycho's support for Maven Toolchains to ensure that Java 8
features do not creep into the code.  This support is enabled by
compiling with the [`useJDK=BREE`](https://eclipse.org/tycho/sitedocs/tycho-compiler-plugin/compile-mojo.html)
setting that ensures bundles are compiled with a JDK that matches
the bundle's `Bundle-RequiredExecutionEnvironment`, but configuring
`tycho-surefire` to run the tests using the configured toolchain
(the default for
[`useJDK=SYSTEM`](https://eclipse.org/tycho/sitedocs/tycho-surefire/tycho-surefire-plugin/test-mojo.html#useJDK)).
These settings
require configuring [Maven's toolchains](https://maven.apache.org/guides/mini/guide-using-toolchains.html)
to point to appropriate JRE installations.  Tycho further requires
that a toolchain defines an `id` for the specified _Execution
Environment_ identifier.  For example, a `~/.m2/toolchains.xml` to
configure Maven for a Java 7 toolchain on a Mac might be:

```
<?xml version="1.0"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <id>JavaSE-1.7</id> <!-- the Execution Environment -->
      <version>1.7</version>
      <vendor>oracle</vendor>
    </provides>
    <configuration>
      <jdkHome>/Library/Java/JavaVirtualMachines/jdk1.7.0_79.jdk/Contents/Home/jre</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

Note that _jdkHome_ above specifies the `jre/` directory: Tycho sets
the default boot classpath to _jdkHome_`/lib/*`, _jdkHome_`/lib/ext/*`,
and _jdkHome_`/lib/endorsed/*`.  For many JDKs, including Oracle's JDK
and the OpenJDK, those directories are actually found in the `jre/`
directory.  Compilation errors such as `java.lang.String` not found
and `java.lang.Exception` not found
indicate a misconfigured _jdkHome_.

