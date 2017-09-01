# Buckaroo
A source-only C++ package manager that will take you to your happy place 🏝️

[![Travis](https://img.shields.io/travis/LoopPerfect/buckaroo.svg)](https://travis-ci.org/LoopPerfect/buckaroo)
[![AppVeyor](https://img.shields.io/appveyor/ci/njlr/buckaroo.svg)](https://ci.appveyor.com/project/njlr/buckaroo)
[![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/LoopPerfect/buckaroo)

[buckaroo.pm](https://www.buckaroo.pm/)

The Buckaroo workflow looks like this:

```
# Create your project file
buckaroo init

# Install dependencies
buckaroo install boost/thread

# Run your code
buck run :my-app
```

## Getting Started

## As a User
If you would like to use Buckaroo (as opposed to develop Buckaroo), the best place to start is [the documentation](http://buckaroo.readthedocs.io/).

## As a Developer
If you would like to develop Buckaroo, then you will need to install [Buck](https://buckbuild.com/setup/getting_started.html) on your system.

Then to build:
```
buck build :buckaroo
```

To run the CLI:
```
buck run :buckaroo-cli
```

To run the unit-tests:
```
buck test :buckaroo-unit
```

To run the integration-tests:
```
buck test :buckaroo-integration
```

You can generate project files for your IDE using `buck project`. Please do not commit these to Git!

Please see [CONTRIBUTING](CONTRIBUTING.md) for more information.

## FAQ

### What platforms is Buckaroo available for?

Buckaroo is available for macOS, Linux and Windows. Please see [the documentation](http://buckaroo.readthedocs.io/) for more information.

### What packages are available?

Official packages can be browsed at [buckaroo.pm](https://www.buckaroo.pm/).

### How can I request a package?

Package requests are handled on [the wishlist](https://github.com/LoopPerfect/buckaroo-wishlist).

### How should I report a bug?

If the bug is for the Buckaroo client, please report it [here](https://github.com/LoopPerfect/buckaroo/issues). If the bug is for a specific package, please report it on [the recipes repo](https://github.com/LoopPerfect/buckaroo-recipes).

Please see [CONTRIBUTING](CONTRIBUTING.md) for more information.

### What is your contribution policy?

Buckaroo is fully open-source and we are accepting external contributions. Please see [CONTRIBUTING](CONTRIBUTING.md) for more information.

First time contributor? Take a look at the issue tracker for [issues marked "first commit"](https://github.com/LoopPerfect/buckaroo/labels/first%20commit).

Another way to contribute is by writing recipes! Send a PR to [this repo](https://github.com/LoopPerfect/buckaroo-recipes) to add a recipe to the official cookbook. If you are looking for a library to port, [the wishlist](https://github.com/LoopPerfect/buckaroo-wishlist) is a good place to start.
