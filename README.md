#How to install:

**WARNING: Do not trust any compiled jar file from any other source than this repository as it may be malicious. **

**If you want to be extra safe, compile it from the source yourself.**

####Prerequisite:

This program requires WinPcap (Windows) or libpcap (OSX or Linux) to be able to capture network traffic in order export your bank from the game. This needs to be installed separately.
You also need java 8 or newer installed.

####Installation:

Eventually there will be a zip file with the compiled jar and the required files included as a release on this repository. As this isn't the case yet you will have to compile from source:




###Compiling from source

Make sure you have JDK8 or newer installed.


#####With the included gradle wrapper:

 - Clone or download the source code from this page.
 - Open the command line or terminal in the main folder of the source(the one with the `gradlew` file in it). 
 - Run the command `gradlew compileJar`

The compiled jar file should appear in the build/libs/ folder. Put this file in a directory of choice along with the `items.txt` and `cache.json` files.

You should now be able to run the jar file and BankViewer should work correctly.
