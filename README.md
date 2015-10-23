# OSMJoin
This java program merges multiple JXMapMatchV3 output files and afterwards creates a combined simulation area OSM.xml which only roads where network measurements were conducted on.
The output can afterwards be used to feed the OMNet TBUS model to allow trace-based cellular network simulation. The main functions of this java program are:
- identify different cellular mobile cell sectors
- aggregate and/or filter measurements per road segment
- merge multiple output files of JXMapMatchV3

##Compile
javac OSMJoin.jav

##Run
java OSMJoin
