# Buckaroo
A source-only C++ package manager that will take you to your happy place 🏝️

## Getting Started
You will need to configure Buck so that it can download from Maven. Here is an example `.buckconfig`:

```
[download]
  maven_repo = http://repo.maven.apache.org/maven2/
  in_build = true
```

Then just run `buck build :buckaroo`.

You can generate project files for your IDE using `buck project`. Please do not commit these to Git!
