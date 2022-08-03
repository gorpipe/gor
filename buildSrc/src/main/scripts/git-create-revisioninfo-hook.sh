#!/bin/bash

## Automatically generate a file with git branch and revision info
##
## Example:
##   [master]v2.0.0-beta-191(a830382)
## Install:
##   cp git-create-revisioninfo-hook.sh .git/hooks/post-commit
##   cp git-create-revisioninfo-hook.sh .git/hooks/post-checkout
##   cp git-create-revisioninfo-hook.sh .git/hooks/post-merge
##   chmod +x .git/hooks/post-*

## - latesttag=$(git describe --tags --abbrev=0 --always) && echo "${latesttag:1:999}"

FILENAME='GITVERSION'

exec 1>&2
branch=`git rev-parse --abbrev-ref HEAD`
shorthash=`git log --pretty=format:'%h' -n 1`
revcount=`git log --oneline | wc -l`
latesttag=`git describe --tags --abbrev=0  --always`

#VERSION="[$branch]$latesttag-$revcount($shorthash)"
VERSION=${latesttag:1:999}
echo $VERSION > $FILENAME