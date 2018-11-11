# buckaroo-fs

Experiments implementing Buckaroo in F#

```bash=
dotnet restore ./buckaroo
dotnet build ./buckaroo
dotnet run --project ./buckaroo-cli
dotnet test ./buckaroo-tests
```

## Releases 

To create a release for macOS: 

```bash=
dotnet publish ./buckaroo-cli/ -c Release -r osx-x64 
./buckaroo-cli/bin/Release/netcoreapp2.1/osx-x64/native/buckaroo-cli
```
