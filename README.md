<p align="center">
  <img src="www/logo-medium.png?raw=true" alt="Buckaroo" />
</p>

# Buckaroo

The decentralized package manager for C++ and friends.

[![](https://img.shields.io/travis/LoopPerfect/buckaroo/buckaroo-redux.svg)](https://travis-ci.org/LoopPerfect/buckaroo) [![](https://img.shields.io/appveyor/ci/njlr/buckaroo/buckaroo-redux.svg)](https://ci.appveyor.com/project/njlr/buckaroo)
[![](https://img.shields.io/badge/docs-wiki-blue.svg)](https://github.com/LoopPerfect/buckaroo/wiki)

## Why Buckaroo?

Package managers like Yarn and Cargo have shown how productive developers can be when they can easily integrate a large ecosystem of projects. Buckaroo fills this gap for C++.

The Buckaroo workflow looks like this:

```bash=
# Create your project file
$ buckaroo init

# Install dependencies
$ buckaroo add github.com/buckaroo-pm/boost-thread@branch=master

# Run your code
$ buck run :my-app
```

We have an [FAQ](https://github.com/LoopPerfect/buckaroo/wiki/FAQ).

### Package Registries

Pull dependencies directly from GitHub, BitBucket, GitLab, hosted Git and HTTP. [How?](https://github.com/LoopPerfect/buckaroo/wiki/Git-as-a-Package-Registry)

<p align="center">
  <img src="www/registries.png?raw=true" alt="Package Registries" width="500px" />
</p>

### IDE Integrations

Support for major IDEs and tools. Integration guides can be found in [the docs](https://github.com/LoopPerfect/buckaroo/wiki/).

<p align="center">
  <img src="www/ides.png?raw=true" alt="IDE Integrations" width="500px" />
</p>

### Features

C++ has unique requirements, so Buckaroo is a highly sophisticated piece of software.

 * Pull dependencies directly from GitHub, BitBucket, GitLab, hosted Git and HTTP
 * Fully reproducible builds and dependency resolution
 * Completely decentralized - there is no central server or publishing process
 * Allows any build configuration (even on a package-by-package basis)
 * Private and public dependencies to avoid "dependency hell"
 * Multiple libraries per package, so tools like Lerna are unnecessary
 * Pull individual packages out of mono-repos
 * Full support for semantic versioning (but only when you want it!)
 * Live at head! Move fast by depending directly on Git branches, but in a controlled way
 * Blazing fast resolution using clever heuristics
 * Version equivalency checks to reduce dependency conflicts
 * TOML configuration files for convenient editing by computers and humans
 * Works offline (with a populated cache)
 * Enable Upgrade Bot to keep everything up-to-date with a single click

### Get Started

Please refer to [the Wiki](https://github.com/LoopPerfect/buckaroo/wiki) for [installation instructions](https://github.com/LoopPerfect/buckaroo/wiki/installation)! ✌️

#### Quick Install

Buckaroo is shipped as a self-contained executable, so all you need to do is download the bundle from the [releases page](https://github.com/LoopPerfect/buckaroo/releases).

##### Linux

```bash
$ wget https://github.com/LoopPerfect/buckaroo/releases/download/v2.2.0/buckaroo-linux -O buckaroo
$ chmod +x ./buckaroo
$ ./buckaroo
```

##### macOS

```bash
$ wget https://github.com/LoopPerfect/buckaroo/releases/download/v2.2.0/buckaroo-macos -O buckaroo
$ chmod +x ./buckaroo
$ ./buckaroo
```

##### Windows

Download [buckaroo.exe](https://github.com/LoopPerfect/buckaroo/releases/download/v2.0.3/buckaroo-windows.exe) from the [release page](https://github.com/LoopPerfect/buckaroo/releases/v2.2.0).

### How Buckaroo Works

The Buckaroo model is very simple. Packages live in source-control, and a manifest file is used to describe dependencies. This points to further manifests to create a dependency graph. Buckaroo works directly over Git and HTTP.

<p align="center">
  <img src="www/how-buckaroo-works.png?raw=true" alt="Buckaroo" />
</p>

Head over to [the Wiki](https://github.com/LoopPerfect/buckaroo/wiki) for more detailed information.

## Attribution

SVG graphics in diagrams are made by [Freepik](http://www.freepik.com/) from [www.flaticon.com](https://www.flaticon.com/) and are licensed by [Creative Commons BY 3.0](http://creativecommons.org/licenses/by/3.0/).
