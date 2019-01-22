<p align="center">
  <img src="www/logo-medium.png?raw=true" alt="Buckaroo" />
</p>

# Buckaroo

The decentralized C++ package manager.

[![](https://img.shields.io/travis/LoopPerfect/buckaroo/buckaroo-redux.svg)](https://travis-ci.org/LoopPerfect/buckaroo) [![](https://img.shields.io/appveyor/ci/njlr/buckaroo/buckaroo-redux.svg)](https://ci.appveyor.com/project/njlr/buckaroo)

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

### Features

C++ has unique requirements, so Buckaroo is a highly sophisticated piece of software.

 * Pull dependencies directly from GitHub, BitBucket, GitLab, hosted Git and HTTP
 * Fully reproducible builds and dependency resolution
 * Completely decentralized - there is no central server or publishing process
 * Allows any build configuration (even on a package-by-package basis)
 * Private and public dependencies to avoid "dependency hell"
 * Multiple libraries per package, so tools like Lerna are unnecessary
 * Pull individual packages out of mono-repos
 * Live at head! Move fast by depending directly on Git branches, but in a controlled way
 * Blazing fast resolution using clever heuristics
 * Version equivalency checks to reduce dependency conflicts
 * TOML configuration files for convenient editing by computers and humans
 * Works offline (with a populated cache)
 * Enable Upgrade Bot to keep everything up-to-date with a single click

## Key Commands

### Init

```bash=
$ buckaroo init
```

Create a Buckaroo manifest in the current working directory.

### Resolve

```bash=
$ buckaroo resolve
```

Generates a fresh lock-file from the existing manifest.

To resolve with an upgrading strategy:

```bash=
$ buckaroo resolve --upgrade
```

### Install

```bash=
$ buckaroo install
```

Installs the packages as described in the current lock-file.

### Add

```bash=
$ buckaroo add <package>@<version>...
```

Adds the given package(s) to the current manifest, updates the lock-file and installs it to the packages folder.

If no satisfactory resolution can be found then nothing is changed.

### Upgrade

```bash=
$ buckaroo upgrade [ <package> [ @<version> ] ]
```

Upgrades the given package(s) to the highest version that meets the constraints in the manifest.
Optionally, a version can be specified to move the package to.

If no packages are specified, then all are upgraded.

### Remove

```bash=
$ buckaroo remove <package>...
```

Removes an existing package from the manifest, updates the lock-file and deletes it from the packages folder.

If no satisfactory resolution can be found then nothing is changed.

### Version

```bash=
$ buckaroo version
```

Displays the version of this installation of Buckaroo.


## Installation

### Portable (All Platforms)

Buckaroo is shipped as a self-contained executable, so all you need to do is download the bundle from the [releases page](https://github.com/LoopPerfect/buckaroo/releases).

For example:

```bash=
$ wget https://github.com/LoopPerfect/buckaroo/releases/download/buckaroo-redux-alpha-6/buckaroo-linux -O buckaroo
$ chmod +x ./buckaroo
$ ./buckaroo
```

Buckaroo uses [Buck](https://buckbuild.com/) as a packaging format, so to build packages you will also need to [install that](https://buckbuild.com/setup/getting_started.html).

## Quick Start

 1. Install Buckaroo and Buck
 2. Run `$ buckaroo quickstart`
 3. Run the generated app:

```bash=
$ buck run :app
Parsing buck files: finished in 0.7 sec (100%)
Building: finished in 1.0 sec (100%) 6/6 jobs, 6 updated
  Total time: 1.9 sec
Hello, world.
```

 4. Add a dependency:

```bash=
$ buckaroo add github.com/buckaroo-pm/ericniebler-range-v3@branch=master
```

 5. Now you can use range-v3. Update `main.cpp` to:

```c++=
#include <iostream>
#include <vector>
#include <range/v3/all.hpp>

int main() {
  auto const xs = std::vector<int>({ 1, 2, 3, 4, 5 });
  auto const ys = xs
    | ranges::view::transform([](auto x) { return x * x; })
    | ranges::to_vector;

  for (auto const& i : ys) {
    std::cout << i << std::endl;
  }

  return 0;
}
```


### 🚨 WARNING 

If your C++ compiler does not default to C++ 14, then you will need to add this to your `.buckconfig` file: 

```ini=
[cxx]
  cxxflags = -std=c++14
```

## Version Constraints DSL

When adding a dependency on a package, you must specify a constraint on the version. Since Buckaroo can work directly on Git, this is a little different to other package managers: 

 * `v1`, `1`, `1.0.0`, `1.0.0` take only semantic version `1.0.0`
 * `>= 1.2` take only semantic versions >= `1.2.0`
 * `^1.2` works as in [NPM](https://docs.npmjs.com/about-semantic-versioning)
 * `~1.2` works as in [NPM](https://docs.npmjs.com/about-semantic-versioning)
 * `branch=master` take only commits on branch `master`
 * `tag=some-tag` take only commits at tag `some-tag`
 * `revision=dc16c91af2519b6129a33bd6e1675ebf73739103` take only revision `dc16c91...`
 * `any(branch=master tag=abc 1.2.3)` take versions that satisfy at least one of: 
   - `branch=master`
   - `tag=abc`
   - `1.2.3`
 * `all(branch=master branch=develop)` take versions that satisfy all of: 
   - `branch=master`
   - `branch=develop`
 * `!tag=abc` take any version that does *not* satisfy `tag=abc`
 
As you might expect, these building blocks can be combined to write complex expressions: 

 * `all(!branch=master any(tag=abc tag=def 1.0.0))`
 * `all(>=1.0.0 !any(1.0.0 1.3.4 revision=dc16c91af2519b6129a33bd6e1675ebf73739103))`

## Official Packages

Since Buckaroo works directly from Git, we don't know how many packages are in the wild!

However, we have a collection of [official packages](https://github.com/buckaroo-pm) over at Buckaroo PM. 

If you cannot find the package you need, create an issue over on [the wishlist](https://github.com/LoopPerfect/buckaroo-wishlist) and we will take a look! 

## Creating a Package

Creating a Buckaroo package is really easy!

You will need to create a few files:

 * `BUCK` containing a build rule ([example](https://github.com/buckaroo-pm/hello/blob/master/BUCK#L1))
 * `buckaroo.toml` containing `targets = [ "<some-build-rule>" ]` ([example](https://github.com/buckaroo-pm/hello/blob/master/buckaroo.toml))

Push these to GitHub, then install as follows:

```bash=
$ buckaroo add github.com/<org>/<project>@branch=master
```

You can also look at our [demo package](github.com/buckaroo-pm/hello) or the many [official packages](https://github.com/buckaroo-pm).

## Buckaroo Macros

Buckaroo will install some macros for use in your build scripts. If you have used quickstart, then you do not need to set this up.

However, if you need something more custom, this is how they work:

```python=
# Load the macros
load('//:buckaroo_macros.bzl', 'buckaroo_cell', 'buckaroo_deps', 'buckaroo_deps_from_package')

# Or just load the one you need
# load('//:buckaroo_macros.bzl', 'buckaroo_deps')

# buckaroo_deps gives you all list of all dependencies in buckaroo.toml
# This is the function you will use most of the time.
all_deps = buckaroo_deps()
# [ 'buckaroo.github.buckaroo-pm.hello//:hello' ]

# buckaroo_cell gives you the cell name for a give package
hello_cell = buckaroo_cell('github.com/buckaroo-pm/hello')
# 'buckaroo.github.buckaroo-pm.hello'

# buckaroo_deps_from_package gives you all deps for a given package
# Unlike most package managers, Buckaroo supports multiple projects per package!
hello_deps = buckaroo_deps_from_package('github.com/buckaroo-pm/hello')
# [ 'buckaroo.github.buckaroo-pm.hello//:hello' ]
```
