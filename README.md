[![unstable](http://badges.github.io/stability-badges/dist/unstable.svg)](http://github.com/badges/stability-badges)


This project provides an Eclipse plugin for building, debugging, and deploying Google Cloud Platform applications.

# Development

## Import into Eclipse

### Requirements

1. Eclipse 4.5 (Mars) or later

1. The [m2eclipse plugin](http://www.eclipse.org/m2e/) (also called m2e) installed
to import the projects into Eclipse.

1. JDK 7

1. git

1. Clone the project to a local directory using `git clone
   https://github.com/GoogleCloudPlatform/gcloud-eclipse-tools.git`.

### Steps to import

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

1. Import the projects

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

  1. Once Eclipse is running, open the `Preferences` again and go to `Plug-in
     Development/Target Platform`.

  1. Set the checkbox next to one of the `Google Cloud Platform for Eclipse Mars`
     target platforms (they are pointing to the same file), and click `Apply`.

  1. After some time Eclipse will finish resolving and setting the target
     platform, you can click `OK`.

  1. There should be no errors in the `Markers` or `Problems` views in Eclipse. However
      you may see several low-priority warnings.

1. Check the imported project:

  1. Right-click the `gcloud-eclipse-tools.launch` file under the `trunk` module in the
  `Package Explorer`.

  1. Select `Run As/1 gcloud-eclipse-tools` from the context menu.

  1. A new instance of Eclipse should be launched with the plugin installed.

##Configuring Maven/Tycho Builds

The plugin is built using Maven/Tycho and targeted Java 7.

We use Tycho's [`useJDK=BREE`](https://eclipse.org/tycho/sitedocs/tycho-compiler-plugin/compile-mojo.html)
setting to ensure that Java 8 features do not creep into the code.
This setting causes bundles to be compiled with a JDK that matches
the bundle's `Bundle-RequiredExecutionEnvironment`.  This setting
requires configuring [Maven's toolchains](https://maven.apache.org/guides/mini/guide-using-toolchains.html)
to point to appropriate JRE installations.  Tycho also requires
that a toolchain provide an `id` equivalent to the specified Execution
Environment identifier.  For example, a `~/.m2/toolchains.xml` to
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
      <jdkHome>/Library/Java/JavaVirtualMachines/jdk1.7.0_75.jdk/Contents/Home/jre</jdkHome>
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
