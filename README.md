<p align="center">
  <img src="www/logo-medium.png?raw=true" alt="Buckaroo" />
</p>

# Buckaroo

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
$ buckaroo add github.com/buckaroo-pm/ericniebler-range-v3
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

---
🚨 Note!

If your C++ compiler does not default to C++ 14, then you will need to add this to your `.buckconfig` file:

```ini=
[cxx]
  cxxflags = -std=c++14
```
---

## Creating a Package

Creating a Buckaroo package is really easy!

You will need to create a few files:

 * `BUCK` containing a build rule ([example](https://github.com/buckaroo-pm/hello/blob/master/BUCK#L1))
 * `buckaroo.toml` containing `targets = [ "<some-build-rule>" ]` ([example](https://github.com/buckaroo-pm/hello/blob/master/buckaroo.toml))

Push these to GitHub, then install as follows:

```bash=
$ buckaroo add github.com/<org>/<project>
```

You can also look at our [demo package](github.com/buckaroo-pm/hello) or the many [official packages](https://github.com/buckaroo-pm).
