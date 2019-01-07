<p align="center">
  <img src="www/logo-medium.png?raw=true" alt="Buckaroo" />
</p>

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

## Why Buckaroo?

Package managers like Yarn and Cargo have shown how productive developers can be when they can esaily integrate a large ecosystem of projects. Buckaroo fills this gap for C++.

C++ has unique requirements, so Buckaroo is a highly sophisticated piece of software.

Some features at a glance:

 * Pull dependencies directly from GitHub, BitBucket, GitLab, hosted Git and HTTP
 * Fully reproducible builds and dependency resolution
 * Completely decentralized - there is no central server or publishing process
 * Allows any build configuration (even on a package-by-package basis)
 * Private and public dependencies to avoid "dependency hell"
 * Multiple libraries per package, so tools like Lerna are unnecessary
 * Pull individual packages out of mono-repos
 * Live at head! Move fast by depending directly on Git branches, but in a controlled way
 * Blazing fast resolution using clever heuristics
 * TOML configuration files for convenient editting by computers and humans


## Installation

### Portable (All Platforms)

Buckaroo is shipped as a self-contained executable, so all you need to do is download the bundle from the [releases page](https://github.com/LoopPerfect/buckaroo/releases).

For example:

```bash=
$ wget https://github.com/LoopPerfect/buckaroo/releases/download/buckaroo-redux-alpha-6/buckaroo-linux -O buckaroo
$ chmod +x ./buckaroo
$ ./buckaroo
```
