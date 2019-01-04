![Alt text](www/logo-medium.png?raw=true "Buckaroo")

# buckaroo

The decentralized C++ package manager.

[![](https://img.shields.io/travis/LoopPerfect/buckaroo/buckaroo-redux.svg)](https://travis-ci.org/LoopPerfect/buckaroo)

The Buckaroo workflow looks like this:

```bash=
# Create your project file
$ buckaroo init

# Install dependencies
$ buckaroo add github.com/buckaroo-pm/boost-thread

# Run your code
$ buck run :my-app
```

## Installation

### Portable (All Platforms)

Buckaroo is shipped as a self-contained executable, so all you need to do is download the bundle from the [release page](https://github.com/LoopPerfect/buckaroo/releases).

For example:

```bash=
$ wget https://github.com/LoopPerfect/buckaroo/releases/download/buckaroo-redux-alpha-6/buckaroo-linux -O buckaroo
$ chmod +x ./buckaroo
$ ./buckaroo
```

### macOS



```bash=
dotnet restore ./buckaroo
dotnet build ./buckaroo
dotnet run --project ./buckaroo-cli
dotnet test
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
./warp-packer --arch macos-x64 --exec buckaroo-cli --input_dir ./buckaroo-cli/bin/Release/netcoreapp2.1/osx-x64 --output buckaroo-macos
./buckaroo-macos
```
