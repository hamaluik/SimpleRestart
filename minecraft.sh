#!/bin/sh
BINDIR="\$(dirname "\$(readlink -fn "\$0")")"
cd "\$BINDIR"
while true
do
	java -Xincgc -Xmx1G -jar craftbukkit-0.0.1-SNAPSHOT.jar
done