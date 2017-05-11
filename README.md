# Buckaroo
A source-only C++ package manager that will take you to your happy place 🏝️

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

And to run the tests: 
```
buck test
```

You can generate project files for your IDE using `buck project`. Please do not commit these to Git!

## FAQ

### What platforms is Buckaroo available for?

Buckaroo is available for macOS, Linux and Windows. Please see [the documentation](http://buckaroo.readthedocs.io/) for more information. 

### What packages are available?

Official packages can be browsed at [buckaroo.pm](https://www.buckaroo.pm/). 

### How can I request a package?

Package requests are handled on [the wishlist](https://github.com/LoopPerfect/buckaroo-wishlist).

### How should I report a bug?

If the bug is for the Buckaroo client, please report it [here](https://github.com/LoopPerfect/buckaroo/issues). If the bug is for a specific package, please report it on [the recipes repo](https://github.com/LoopPerfect/buckaroo-recipes). 

### What is your contribution policy?

Buckaroo is fully open-source and we are accepting external contributions. If you would like to contribute, please create a pull-request and we will review it promptly. 

Another way to contribute is by writing recipes! Send a PR to [this repo](https://github.com/LoopPerfect/buckaroo-recipes) to add a recipe to the official cookbook. If you are looking for a library to port, [the wishlist](https://github.com/LoopPerfect/buckaroo-wishlist) is a good place to start. 



