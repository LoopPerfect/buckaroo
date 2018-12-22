namespace Buckaroo

type ResolvedVersion = {
  Versions : Set<Version>;
  Lock : PackageLock;
  Manifest : Manifest;
}
