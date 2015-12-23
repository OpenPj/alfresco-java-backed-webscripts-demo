Alfresco Java-Backed WebScripts samples shows the examples described in the official Alfresco Wiki that are packaged in a standard AMP and testable on-the-fly using Maven Alfresco Lifecycle (Alfresco Maven SDK).

[Download](http://code.google.com/p/alfresco-java-backed-webscripts-demo/downloads/list) the related AMP for your Alfresco installation.

To automagically run an Alfresco instance embedded in Jetty using H2 database, run the following command from the root of the project:

**mvn integration-test -Pamp-to-war**

Please note that if you are using the 4.1 branch (for Alfresco versions until 4.1):

**mvn clean integration-test -P webapp**


To build the AMP run the following command from the root of the project:

**mvn clean package**


All the examples included with this project are related to the Java-backed Web Scripts Samples of the [Alfresco Wiki](http://wiki.alfresco.com/wiki/Java-backed_Web_Scripts_Samples)

You can quickly identify the demo WebScripts under the family "Alfresco Java-Backed WebScripts Demo".

The project was implemented using Apache Maven 3.0.4, so if you want to build your own customizations using this project, please update your Maven installation if you have installed an older version.

If you have any problem during the installation or the execution of this sample module, please create a new ticket in the [Issues page](http://code.google.com/p/alfresco-java-backed-webscripts-demo/issues/list).

The project consists of two main branches: the 4.1 branch that supports Alfresco up to the 4.1.x and the main trunk that supports versions starting from 4.2. This means that now it supports all the Alfresco versions :)