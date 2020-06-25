# CxSAST Maven Plugin
The Checkmarx Maven Plugin is a reliable and easy to use plugin for the Apache Maven tool.
The plugin allows you to run the Checkmarx Static Application Security Test (SAST), as a stand-alone scan, or as part of any of the Maven life-cycle.

### Maven
Maven is a software project management and build automation tool used primarily for Java projects and can also be used to build and manage projects written in C#, Ruby, Scala, and other languages. Based on the concept of a Project Object Model (POM), Maven can manage a project's build, reporting and documentation from a central piece of information. The Maven project is hosted by the Apache Software Foundation. Maven is built using a plugin-based architecture that allows it to make use of any application controllable through standard input. The Maven project is hosted by the Apache Software Foundation.

## Overview
### CxSAST 
CxSAST is a security solution provided by Checkmarx that scans application source code for vulnerabilities. You can integrate CxSAST with any Maven code build process, enabling a project XML file to automatically initiate a CxSAST scan. Integration is achieved with the Checkmarx's CxSAST Maven plugin. The plugin can be found on the central repository and is simple to install and configure.

### CxOSA 
CxOSA (v8.7.0 and up) - Maven now uses a new core library with better compatibility and increased result accuracy. The new capability extracts dependencies resolving manifest files in customer side, therefore supports scanning of Maven pom.xml files. For all Maven configuration files, Cx Manager will download the necessary packages, calculate metadata, and submitting them to Cloud engine. Repositories must be accessible to the manager.

## Prerequisites for CxSAST and CxOSA - Apache Maven

Apache Maven installed (3.2.0 and up)
Maven requires Java Development Kit (3.1 and up)
Checkmarx CxSAST installed (8.4.1 and up)
CxOSA (8.7.0 and up) - Maven should be installed locally
Java 8 (for running OSA scans)
CxSAST Maven Plugin installed (8.41.0 and up):
Release – Predefined for automatic download and installation.

## Build
mvn clean install

## Contributing
Please read through our [contributing guidelines](CONTRIBUTING.md).