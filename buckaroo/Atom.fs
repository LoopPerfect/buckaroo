namespace Buckaroo

type Atom =
  {
    Package : PackageIdentifier;
    Versions : Set<Version>
  }
  override this.ToString () =
    PackageIdentifier.show this.Package + "@" + Version.show this.Versions.MinimumElement
