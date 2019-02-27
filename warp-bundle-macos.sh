#!/bin/bash

wget -O warp-packer https://github.com/dgiagio/warp/releases/download/v0.3.0/macos-x64.warp-packer

chmod +x ./warp-packer
./warp-packer --version

dotnet publish ./buckaroo-cli/ -c Release -r osx-x64

mkdir -p warp
rm -rf ./warp/buckaroo-macos

./warp-packer --arch macos-x64 --exec buckaroo-cli --input_dir ./buckaroo-cli/bin/Release/netcoreapp2.1/osx-x64   --output warp/buckaroo-macos
./warp/buckaroo-macos
