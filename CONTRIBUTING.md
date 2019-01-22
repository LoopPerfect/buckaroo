# Contributing to Buckaroo 🎉

Hello! Thanks for taking the time to contribute to Buckaroo. Buckaroo is a community project, and will only succeed through the work of contributors such as yourself. This guide will help you get started.


## Important Resources

The Buckaroo project is spread across a few different repositories:

 1. [LoopPerfect/buckaroo](https://github.com/LoopPerfect/buckaroo) contains the source-code for the Buckaroo client that users install on their machines.
 2. [LoopPerfect/buckaroo-wishlist](https://github.com/LoopPerfect/buckaroo-wishlist) is an issue-tracker where users can suggest recipes for inclusion in the official cookbook.
 3. [LoopPerfect/homebrew-lp](https://github.com/LoopPerfect/homebrew-lp) is the [Homebrew](https://brew.sh/) (and [Linuxbrew](http://linuxbrew.sh/)) tap that contains the Buckaroo formula.

Official packages live in a [dedicated GitHub account](https://github.com/buckaroo-pm/).


## Getting Help

If you need quick interaction, a good avenue for talking to the developers is [Gitter.im](https://gitter.im/LoopPerfect/buckaroo). If you have a complex problem, reporting an issue is the best route.


## Reporting Issues

Since Buckaroo has a few different components, it is important that issues are reported in the right place.

 * Report **installation problems** to the [buckaroo issue tracker](https://github.com/LoopPerfect/buckaroo/issues).
 * Report **bugs in the client** to the [buckaroo issue tracker](https://github.com/LoopPerfect/buckaroo/issues).
 * Report **issues with a specific package** to that package's issue tracker.
 * Report **package requests** to the [buckaroo-wishlist](https://github.com/LoopPerfect/buckaroo-wishlist/issues).
 * Report **feature requests** to the [buckaroo issue tracker](https://github.com/LoopPerfect/buckaroo/issues).

If you are not sure, just report to the [buckaroo issue tracker](https://github.com/LoopPerfect/buckaroo/issues). 😌


### Security

If you have a sensitive issue, such as a security bug, please send an email to security@buckaroo.pm.


## Making a Contribution

The procedure for contributing to Buckaroo is:

 1. Fork [LoopPerfect/buckaroo](https://github.com/LoopPerfect/buckaroo) on GitHub
 2. Make some changes, adding unit-tests where appropriate
 3. Ensure that all tests pass
 4. Make a pull request (usually to `master`)
 5. Bask in the kudos! 👏👑


## What should I work on?

We endeavor to keep [the issue tracker](https://github.com/LoopPerfect/buckaroo/issues) up-to-date, so that is the best place to start. Keep an eye out for [issues marked "help wanted"](https://github.com/LoopPerfect/buckaroo/labels/help%20wanted).

### First-time Contributor ❤️

First-time contributor? Take a look at the issue tracker for [issues marked "first commit"](https://github.com/LoopPerfect/buckaroo/labels/first%20commit) for smaller, self-contained tasks. We would also be happy to walk you through any of the existing code on [Gitter.im](https://gitter.im/LoopPerfect/buckaroo).

### Packages

Another way to contribute is by writing packages! Because Buckaroo is decentralized, you can do this in your repo without approvals from us. However, we can also help guide the process on [Gitter.im](https://gitter.im/LoopPerfect/buckaroo).

If you are looking for a library to port, [the wishlist](https://github.com/LoopPerfect/buckaroo-wishlist) is a good place to start.

### Your Own Feature

If you would like to contribute a feature that you have thought of, please [create an issue](https://github.com/LoopPerfect/buckaroo/issues) first so that we can ensure the design is in-keeping with the general direction of Buckaroo.

## Environment

Development of Buckaroo requires [F# and dotNET Core](https://docs.microsoft.com/en-us/dotnet/fsharp/get-started/get-started-command-line). We also recommend [Visual Studio Code](https://code.visualstudio.com/) and [Ionide](http://ionide.io/) for code-completion features.

To fetch the source-code:

```bash=
git clone https://github.com/LoopPerfect/buckaroo.git
cd buckaroo
```

To build the project:

```bash=
dotnet build ./buckaroo-cli
```

To run the project:

```bash=
dotnet run --project ./buckaroo-cli
```


### Which branch should I use? 🤔

The convention for branches in Buckaroo is:

 * `master` - the very latest code with the latest features, but not recommended for production use
 * `release/version` - the branch from which a release version is tagged
 * `bugfix/bug` - a branch for fixing a specific bug
 * `feature/widget` - a branch for implementing a specific feature
 * `improvement/widget` - a branch for making a specific improvement, such as refactoring

Most developers should branch from `master` in order to have their changes included in the next release.

If you would like to patch an old release, you should branch off of `release/version`.


## Testing

Buckaroo uses automated testing and [Travis CI](https://travis-ci.org/LoopPerfect/buckaroo) to prevent regressions.

To run the tests:

```bash=
dotnet test
```

## Bundling

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


## Making a Pull Request

Once your submission is ready, you should make a pull request on GitHub to the appropriate branch.

 * If you are implementing a new feature or bug-fix for the next release, then you should `base` your pull request on `master`.

 * If you are making a bug-fix for an old release, you should `base` on the appropriate `release/version` branch.

We review pull requests within 24 hours.


## Releases

Buckaroo releases are semantically versioned Git tags. You can see [the releases on GitHub](https://github.com/LoopPerfect/buckaroo/releases).

Each release has a corresponding branch named `release/version`. This allows `master` to progress beyond the current release, whilst still allowing for easy patching of old versions.


### Installing a Cutting-edge Release ✋⚠️

If you would like to use the latest version of Buckaroo from `master`, then you can do this using [Homebrew](https://brew.sh/) or [Linuxbrew](http://linuxbrew.sh/):

```bash=
brew install --HEAD loopperfect/lp/buckaroo
```
