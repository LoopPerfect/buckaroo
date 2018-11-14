# buckaroo-fs

Experiments implementing Buckaroo in F#

```bash=
dotnet restore ./buckaroo
dotnet build ./buckaroo
dotnet run --project ./buckaroo-cli
dotnet test ./buckaroo-tests
```

## Releases 

Releases are bundled using [Warp](https://github.com/dgiagio/warp): 

```bash=
wget -O warp-packer https://github.com/dgiagio/warp/releases/download/v0.3.0/macos-x64.warp-packer 
./warp-packer
```

To create a release for macOS: 

```bash=
dotnet publish ./buckaroo-cli/ -c Release -r osx-x64 
./warp-packer --arch macos-x64 --exec native/buckaroo-cli --input_dir ./buckaroo-cli/bin/Release/netcoreapp2.1/osx-x64/ --output buckaroo-macos
./buckaroo-macos
```
