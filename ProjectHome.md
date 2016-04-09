This project studies variability in Linux with a specific focus on Kbuild. This is just a brief overview of our work and the Makefile constraint extractor, Makex, we used to extract the constraints from Kbuild. I will be updating this page soon to provide more details. In the meantime, more details can be found in the following papers:

Sarah Nadi and Ric Holt. Mining Kbuild to Detect Variability Anomalies in Linux. In CSMR'12: Proceedings 16th European Conference on Software Maintenance and Reengineering, Szeged, Hungary, 2012. (http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=6178858&tag=1)

Sarah Nadi and Ric Holt. Make It or Break It: Mining Anomalies in Linux Kbuild. In WCRE'11: Proceedings of the 18th Working Conference on Reverse Engineering, Limerick, Ireland, 2011. (http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=6079857)

In order to investigate the variability in Kbuild, we have implemented Makex which extracts the constraints used to compile source files in Linux (this is an enhanced version of the one I used in the CSMR 2012 paper). Makex is implemented in Java, and has a current source file coverage rate of 85%. Since this is a text based parsing, and Kbuild notation is exteremely complicated, there are still some limitations which we will try to address. Makex has been tested on releases 2.6.30 - 3.6, but is not guaranteed to run correctly on older/newer versions.

This is the latest version of Makex: Makex (referred to as Linux Makefile Parser in the listed papers)

To run it, run the following command from within a Linux tree:

java -classpath LinuxMakeFileParser.jar -Xms1024M -Xmx2048M makefiles.Makex

The constraints in Kbuild will be outputted in a "models" directory, with one .makemodel file for each Linux architecture present. Files which are compiled unconditionally will have no constraints.

In order to identify tristate features, we rely on the kconfig models extracted by Undertaker. We basically need the list of features (i.e., which also have a _MODULE variation) defined as tristate in the Kconfig files. A list of such features for releases 2.6.34-3.6 is available at http://swag.uwaterloo.ca/~snadi/module-items/. Add the modules.txt file to your work directory before running Makex. You can simply add an empty modules.txt file, but no_MODULE variation will be generated in the make constraints which may lead to false positives.

You can use this script (http://swag.uwaterloo.ca/~snadi/undertakerAnalysis) as an example of how to run the analysis to detect dead and undead blocks with and without the make constraints.