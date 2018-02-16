#!/bin/sh
# Reapply the settings within eclipse/settings directory
if [ ! -d eclipse/settings -o ! -d plugins ]; then
    echo "Meant to be run from the root of the repository"
    exit 1
fi

set -o errexit
set -x
(cd eclipse/settings && mvn install)
(cd plugins && mvn --no-snapshot-updates -Dtycho.mode=maven eclipse-settings:eclipse-settings)
