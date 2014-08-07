**Work in progress!!**

Master Thesis Extractor (MTE)
=============================

## About

The Master Thesis Extractor is a temporal information extraction system,
which outputs RDF from Wikipedia Infoboxes.

It reuses the [DBPedia Extraction Framework](https://github.com/dbpedia/extraction-framework) [Mapping Extractor](http://wiki.dbpedia.org/DeveloperDocumentation/Extractor?v=vqu#h110-5)
to extract values and reuses [Heidel Time](https://code.google.com/p/heideltime/) to extract temporal expressions.

## Datasets

Currently the main focus of MTE is to extract a time-based dataset about 'companies'.
The latest release of the dataset can be downloaded from:
[tiny.cc/tmpcompany](tiny.cc/tmpcompany)

## Run 

In order to run an extraction on your own either download a binary release or build it on your own.

MTE uses a [MongoDB](http://www.mongodb.org/) to cache wiki articles and revisions. By default MTE connects to the Mongo DB listening on 127.0.0.1:27017. Set the environment variable MTE_MONGODB to a valid [connection string](http://docs.mongodb.org/manual/reference/connection-string/) in order to overwrite the default value.

Run the app using a binary release:

1. Unzip the binary release file
1. Execute 'bin/mte', respectively 'bin/mte.bat'

Run your own build:

1. Follow the instructions to compile your own build 
1. Execute 'sbt start' from the MTE project directory

For further details see the [play documentation](http://www.playframework.com/documentation/2.2.x/Production).


## Build 

MTE is a Play Framework application and therefore uses [sbt](http://www.scala-sbt.org/) as its build tool.
For further information please see the
[play documentation](http://www.playframework.com/documentation/2.2.x/Home)

MTE expects all its dependencies being available in a [Apache Ivy](http://ant.apache.org/ivy/) or a [Apache Maven](http://maven.apache.org/) repository.

TODO: Provide details on where to download dependencies, which are not on maven central.


Requirements: Java 8, sbt

## License 
The source code is published under the terms of the [GNU General Public License, version 2](http://www.gnu.org/licenses/gpl-2.0.html).



