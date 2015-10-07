# Introduction #

This page describes how to setup a development environment with Eclipse in order to test and debug MMS. It is intended for MMS developers, enthusiasts and contributors who like to look under the hood and tune the engine.

## Pulling the MMS source code ##

The first step is naturally to pull the MMS source code from the [git repository](http://code.google.com/p/mediaserver/source/checkout):

```
http://code.google.com/p/mediaserver/source/checkout 
```

Let's assume the root directory of the local git clone of MMS is
```
/Users/ivelin117/tools/mediaserver 
```

After performing ` git clone ` the directory contents should looks something like this:

```

Ivo-Mac:mediaserver ivelin117$ ls 
bootstrap		git-patch.sh		mvn.log
chassis			io			pom.xml
codecs			ivelin-cr2.diff		release
component		jsr-309			resources
controls		jsr-309-tck		scheduler
endpoints		log			spi
git-outgoing.sh		media-server-docs	test-suite


```


## Installing Eclipse Enterprise Edition ##

The next step is to install a recent version of Eclipse for Java EE Developers. It is available for download from http://www.eclipse.org/downloads/.

After installing Eclipse, the About dialog should look similar to the following image:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseJavaEEAbout.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseJavaEEAbout.png)

## Installing the Eclipse Maven plugin ##

MMS uses Maven for project build and dependency management.

The M2E plugin for Eclipse allows Maven project management within Eclipse. M2E is already pre-installed with Eclipse Indigo for Java Developers. At this time it does not ship with Eclipse for Java EE Developers, but that might change. Check your plug-in repository in Eclipse to see if M2E is already present. If not, you can download and install from: http://www.eclipse.org/m2e/.

## Create maven projects in Eclipse from the local git repo ##

The next step is to import the MMS maven modules from the local git clone to the Eclipse workspace. Go to File > Import > Existing Maven Projects:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMaven2Import.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMaven2Import.png)

The next screen should look similar to:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMaven2ImportPage2.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMaven2ImportPage2.png)

Finish the rest of the wizard with its default options.

## Setting up the Eclipse Project to use JVM 1.6 ##

Make sure that you have JDK 1.6 or later installed on your machine. Otherwise you will see compile errors in the MMS code. It is possible that your Eclipse by default picked up an older version of the JVM, which results in compile errors in Eclipse. To resolve that, go to Eclipse Preferences > Java > Compiler and set the Compiler compliance level to 1.6. The dialog should look similar to the following screenshot:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseJDKCompilerComplianceLevel.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseJDKCompilerComplianceLevel.png)

If you have projects that need to use an older version of the JDK, you can also set the compiler compliance level just for the MMS maven projects.

## Setting up a Maven build target in Eclipse ##

To build the full MMS code base, you would need to setup a Maven build target. Go to Run > Run Configuration... Select Maven Build type and create a new configuration. Now setup the Maven parameters in the Main tab. The configuration screen should look like this:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMavenBuildTarget.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMavenBuildTarget.png)

You can now build and package MMS by running this new ` mediaserver ` target. Be patient. The build cleans up everything, compiles from scratch, packages and runs all unit tests. It takes a few minutes, but it is well worth the wait. Its better to invest a little extra time in thinking the code changes through before and while running the full build, rather than looking for a quick shortcut to build the project.

## Building MMS from the command line ##

Alternatively you can build MMS from the command line like so:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/CommandLineMavenBuildTarget.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/CommandLineMavenBuildTarget.png)

## Setting up run/debug parameters in Eclipse ##

The next step is to prepare Eclipse for running and debugging MMS. Go to Run > Debug Configuration... Select Java Application in the list of debug target types and press the New button to create a new configuration of the Java Application type. You can name it "MMS Main" or something else that makes sense to you. There are two important tabs to setup in the new configuration. In the Main tab, select project ` bootstrap ` from your Eclipse workspace. Then set the main class to ` org.mobicents.media.server.bootstrap.Main `. The screen should look like this:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseDebugConfig1.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseDebugConfig1.png)

Then populate the VM arguments field in the (x)=Arguments tab. A reasonable default value is:

```
-DMMS_HOME=/Users/ivelin117/tools/mediaserver/bootstrap/target/mms-server -Xms256m -Xmx512m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000
```

The screen should look like this:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseDebugConfig2.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseDebugConfig2.png)

## Starting MMS in Debug mode from Eclipse ##

Now we have come along far enough to be able to run and debug MMS. To start MMS in debug mode and test that it works, let's setup a breakpoint in its main class. If all is well, after setting the breakpoint and starting a debug target, you should see a screen similar to:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMMSBreakpoint.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/EclipseMMSBreakpoint.png)

As MMS is inherently multi threaded you may want to select Suspend VM in Breakpoint options instead of the default Suspend Thread setting.

### Starting MMS from the command line ###

You can also start MMS from the command line if you prefer:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/StartMMSCommandLine.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/StartMMSCommandLine.png)

## Running the JSR 309 tests ##

MMS is fully compliant with JSR 309 - Media Server Control API. You can verify yourself by running the full JSR 309 test suite. First start MMS. Then from the command line, go to the JSR 309 TCK directory and run the ant junit target like so:

```

/Users/ivelin117/tools/mediaserver/jsr-309-tck
Ivo-Mac:jsr-309-tck ivelin117$ ant -f build.xml
Buildfile: /Users/ivelin117/tools/mediaserver/jsr-309-tck/build.xml

```

## Running a single JSR 309 test ##

To run a single JSR 309 test from the command line, you can use this example:

```

ant -f build.xml -Drun.testlist=com/hp/opencall/jmsc/test/mandatory/functional/mediagroup/PlayerTest.class

```

It is sometimes helpful to run a single test from Eclipse in order to step through either the test or the MMS code or both. For that, you would need to setup a new Run/Debug Configuration target of type JUnit test. It would look like this:

![http://wiki.mediaserver.googlecode.com/git-history/master/images/JSR309JunitEclipse.png](http://wiki.mediaserver.googlecode.com/git-history/master/images/JSR309JunitEclipse.png)


## Summary ##

Hopefully this document was able to speed up the setup time for folks eager to get their hands on MMS and see what it is made of. Contributions to MMS and proposals for improvements to this document are most welcome!