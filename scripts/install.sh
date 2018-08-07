#!/bin/sh
#
# installs the GraphWriter application
#

echo "installing the the GraphWriter application ..."
cd ${HOME}
# remove previous file
rm -fr GraphWriter-1.0

unzip -ouK ${HOME}/git-aicoin/GraphWriter/target/GraphWriter-1.0.zip

cd ${HOME}/GraphWriter-1.0
chmod a+x run-graph-writer.sh

cd ${HOME}/GraphWriter-1.0/log
rm GraphWriter.log


