#!/bin/bash

# If parameter was given, package app into jar first
if [ -n "$1" ]; then
  boot doc
fi

# Upload the documents folder
rsync -r doc/code/ maxi@maximilian-kuschewski.de:/deployments/beer-game/doc
