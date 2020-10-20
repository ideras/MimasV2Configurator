#!/bin/bash

if [ ! -d bin ]; then
  mkdir -p bin
fi
rm -rf bin/*

javac -d bin -sourcepath src/ -cp lib/jna-4.4.0.jar:lib/purejavacomm.jar src/mc/Main.java
cp ./mimasv2conf.gif bin/
cp -R ./src/icons bin
