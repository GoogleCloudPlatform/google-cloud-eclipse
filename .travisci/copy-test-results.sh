#!/bin/sh
# Find and copy all screenshots and test logs from $1 to $2
#
# --include '*/'                   include all directories
# --include 'surefire-reports/**'  include contents of surefire-reports
# --include 'screenshots/**'       include contents of screenshots
# --exclude '*'                    exclude all other files
rsync -aR ${verbose:+-v} --prune-empty-dirs \
    --include '*/' \
    --include 'surefire-reports/**' --include 'screenshots/**' \
    --exclude '*' \
    "$1" "$2"
