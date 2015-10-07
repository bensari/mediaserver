# Binding to Network Interface instead of localhost #

If you need to bind to a network interface (ie 192.168.0.12 by example), you need to edit the `$MMS_HOME/deploy/server-bans.xml` as the server currently doesn't support -b option as in JBoss to bind to a specific network interface. 2 Beans need to be modified :

  * _MediaServer_ and change its _bindAddress_ property to the network interface IP Address to bind to.
  * _localhost_ and change its _bindAddress_ property to the network interface IP Address to bind to. Please be aware of changing _localNetwork_ and _localSubnet_ to match the bind address as well

## Troubleshooting ##

Please be aware that some editing tools (like gedit in ubuntu) create hidden backup files (ie server-beans.xml~) that can mess up the startup as MMS will try to merge both server-beans.xml and its backup file server-beans.xml~ into one file located in `$MMS_HOME/temp/deployment-beans.xml` and so an exception will be thrown similar to the one below

```
Exception in thread "main" java.lang.IllegalStateException: SystemClock is already installed.
	at org.jboss.dependency.plugins.AbstractController.install(AbstractController.java:716)
	at org.jboss.dependency.plugins.AbstractController.install(AbstractController.java:540)
	at org.jboss.kernel.plugins.deployment.AbstractKernelDeployer.deployBean(AbstractKernelDeployer.java:319)
	at org.jboss.kernel.plugins.deployment.AbstractKernelDeployer.deployBeans(AbstractKernelDeployer.java:297)
	at org.jboss.kernel.plugins.deployment.AbstractKernelDeployer.deploy(AbstractKernelDeployer.java:130)
	at org.jboss.kernel.plugins.deployment.BasicKernelDeployer.deploy(BasicKernelDeployer.java:76)
	at org.jboss.kernel.plugins.deployment.xml.BasicXMLDeployer.deploy(BasicXMLDeployer.java:88)
	at org.jboss.kernel.plugins.deployment.xml.BasicXMLDeployer.deploy(BasicXMLDeployer.java:158)
	at org.mobicents.media.server.bootstrap.MainDeployer.start(MainDeployer.java:126)
	at org.mobicents.media.server.bootstrap.Main.start(Main.java:242)
	at org.mobicents.media.server.bootstrap.Main.boot(Main.java:235)
	at org.mobicents.media.server.bootstrap.Main.main(Main.java:86)

```

To fix it, you need to remove the backup file ie `$MMS_HOME/deploy/server-bans.xml~` created by the editing tool

# Media Server Resources Configuration and Performance Tuning #

> Under `bootstrap/target/mms-server.dir/deploy` you have `server-beans.xml`.
> You can set the following items :
    * MGCP Transactions Pool size : under MGCP bean you have pool size. This property defines the number of mgcp transactions that can be handled simultaniously ( in 20-40ms timeframe ). Higher load means more packets need to be handled , so this number may be incremented. Public Discussion on tuning of this value https://groups.google.com/d/topic/mobicents-public/Jh7kg-C17EY/discussion
    * Endpoints : number of endpoints for each type can be configured by changing the name pattern. If you need 50 endpoints set [1..50] instead of the values currently set. The names of the endpoints can be changed in the name patterns also.
    * RTP connections pool size and local connections pool size: you can configure how many connections each endpoint type can handle. Local Connection is used to connect 2 endpoints locally. RTP Connection is used to create RTP connection to a client.

The current configuration comes with predefined values optimized for the JSR 309 TCK run.

Memory limitations : Default configuration comes with 256MB min and 512MB max. If you increase the number of endpoints/max connections allowed per endpoint high enough, the server may get stuck while  loading the endpoints. You can increase the memory limits by changing one of the following:
if you are running wWindows change the following line in run.bat :

```
set JAVA_OPTS=%JAVA_OPTS% -Xms128m -Xmx512m.
```

Xms is minimum memory size , where Xmx is maximum memory.

if you are running linux change following line in run.sh :

```
JAVA_OPTS="$JAVA_OPTS -Xms256m -Xmx512m -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"
```

Both files are located in bootstrap/target/mms-server.dir/bin folder.
If you are planning to modify the source code and compile it several times you can make the same changes in bootstrap/src/main/config/ folder for all 3 files ( run.sh,run.bat and server-beans.xml

# How to install MMS and verify compliance with JSR 309 #

  1. Download and install [ant](http://ant.apache.org/) and [Maven](http://maven.apache.org/):
  1. Checkout the media server source code from the main git repository, revision [b7ab801ffca8](https://code.google.com/p/mediaserver/source/browse/?r=b7ab801ffca891192a1f8dbc86da0c3a10571c80)
  1. Lets assume $mediaserver is the root folder of the checked out media server code
  1. Building from source - to compile and build the media server run ` mvn clean install ` under the `$mediaserver` folder.
  1. Starting the media server - go to ` $mediaserver/bootstrap/target/mms-server/bin/ ` and run
    * `run.sh` , _or_
    * `run.bat` depending on your OS
  1. Configure the JSR 309 TCK:
> > The JSR 309 TCK is located in $mediaserver/jsr-309-tck folder. You will have to change 2 files :
    * mixeradaptertest.properties
    * TCK.properties
> > In both of them replace [file:///opt/mobsource/media/](file:///opt/mobsource/media/) with the value of $mediaserver.
  1. After these configuration changes run from the same folder $mediaserver/jsr-309-tck:
> > `ant -f build.xml`
    * Test report will be generated in `tck-reports/<date_time>/junit-noframes.html`