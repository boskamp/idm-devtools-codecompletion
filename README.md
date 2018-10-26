# idm-devtools-codecompletion
JavaScript code completion for SAP&reg; Identity Management

## About
This a command line tool for generating code completion metadata for the JavaScript API of [SAP&reg; Identity Management (IDM)](https://www.sap.com/products/identity-management.html). It supports the following code completion systems:

* [YASnippet](http://joaotavora.github.com/yasnippet/) for GNU Emacs
* [Tern](http://ternjs.net/) for GNU Emacs, Vim, Eclipse and others

With this metadata, any text editor or IDE supported by these systems can provide code completion for SAP&reg; IDM's internal JavaScript functions.

idm-devtools-codecompletion is free and open source software available under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt). 

## Build
You need [Git](https://git-scm.com/) and [Maven 3.x](https://maven.apache.org/) to build the software. Maven downloads dependencies from the Internet by default, so your build machine will need to be connected to the Internet.

     git clone https://github.com/boskamp/idm-devtools-codecompletion
     cd idm-devtools-codecompletion
     mvn package
     
The build produces an executable JAR file **idm-devtools-codecompletion-&lt;VERSION&gt;.jar**  in the target subdirectory. Run this JAR file with the `--help` option for more information.

