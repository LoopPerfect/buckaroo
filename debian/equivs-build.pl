#!/usr/bin/perl

# Copyright 1999 by Martin Bialasinski
# Copyright 2006 by Peter Samuelson
# This programm is subject to the GNU General Public License Version 2

# doc-base Copyright (C) 1997,1998 Christian Schwarz,
# Also licensed under the GPL2

# We had problems with this file changing across systems, so now it is tracked in source-control. 

use strict;
use warnings;

use Cwd;
use Getopt::Long qw( :config no_ignore_case bundling );
use File::Copy;
use File::Basename;
use File::Temp ('tempdir');

my $builddir = tempdir('equivs.XXXXXX', DIR => ($ENV{TMPDIR} || cwd), CLEANUP => 1) or
  die "Cannot create temporary build dir: $!\n";
my %control;

sub usage {
  print STDERR <<EOU;
Usage: equivs-build [--full|-f] [--source|-s] [--arch=foo|-a=foo] controlfile
controlfile is the name of an equivs controlfile.
You can use "equivs-control filename" to create one.

--full   Full build including signing, etc., suitable for upload to Debian
--source Source build including signing, etc., suitable for upload to a PPA
--arch   Build package for a different architecture.
         Used e.g. for building Hurd packages under Linux.
EOU
  exit 1;
}

my ($full_package, $source_only, $arch);
GetOptions('full|f' => \$full_package, 'source|s' => \$source_only, 'arch|a=s' => \$arch) or usage();

my $debug = 0;

umask(022);

my $controlfile = $ARGV[0];
if (! $controlfile) {
  print STDERR "No control file was specified\n";
  usage();
}

system("cp -R /usr/share/equivs/template/* \"$builddir\"") == 0 or
  die "Error on copy of the template files: exit status " . ($?>>8) . "\n";

# Parse the equivs control file

read_control_file($builddir, \%control, $arch, $controlfile);

if ($debug) {
  my ($k, $v);
  while (($k, $v) = each %control ) {
    print "$k -> $v\n";
  }
}

# Copy any additional files

my @extra_files = split ",", $control{'Extra-Files'} || "";
my %install_files = ();
for (split "\n", $control{'Files'} || "") {
    die "Cannot parse Files line: '$_'\n"
        unless m:^\s*(\S+)\s+(\S+)/?\s*$:;
    $install_files{"$2/$1"} = $1;
    # $install_files{"$2/".basename($1)} = $1;
}
my %create_links = ();
for (split "\n", $control{'Links'} || "") {
    die "Cannot parse Links line: '$_'\n"
        unless m:^\s*(\S+)\s+(\S+)/?\s*$:;
    $create_links{"$2"} = $1;
}
my %create_files = ();
for (@{$control{'File'} || []}) {
  if (m/^\s*(\S+)(?:\s+(\d+))?\s*\n(.*)$/s) {
    my ($f,$m,$b) = ($1,$2,$3);
    $m = (oct $m||0) || 0644;
    $b =~ s/^ //mg;
    $b =~ s/^[.]([.]*)$/$1/mg;
    $create_files{$f} = [$b,$m];
  }
}

mkdir "$builddir/install", 0755;
open INSTALL, '>', "$builddir/debian/install" or
  die "Cannot open $builddir/debian/install for writing: $!\n";
foreach my $target (keys %install_files, keys %create_files, keys %create_links) {
  $target =~ s/ +//g;
  my $dest;
  my $cnt = 0;
  if ($target =~ m/^(preinst|postinst|prerm|postrm)$/) {
    $dest = "debian/$target";
  } else {
    do {
      $dest = "install/$cnt";
      mkdir "$builddir/$dest" unless -d "$builddir/$dest";
      $dest .= "/" . basename($target);
      $cnt++;
    } while ( -e "$builddir/$dest" );
    print INSTALL "$dest " . dirname($target) . "\n";
  }
  $dest = "$builddir/$dest";
  if (defined $install_files{$target}) {
    my $file = $install_files{$target};
    copy $file, $dest
      or die "Cannot copy $file to $dest: $!\n";
    chmod -x $file ? 0755 : 0644, $dest
      or die "Cannod chmod $dest: $!\n";
  } elsif (defined $create_links{$target}) {
    my $file = $create_links{$target};
    symlink ($file, $dest)
      or die "Cannot create symlink $dest pointing to $file: $!\n";
  } else {
    my ($content, $mode) = @{$create_files{$target}};
    open CREATE, '>', $dest
      or die "Cannot create file $dest: $!\n";
    print CREATE "$content\n";
    close CREATE;
    chmod $mode, $dest
      or die "Cannot chmod $dest: $!\n";
  }
}
close INSTALL;

mkdir "$builddir/docs", 0755;
open DOCS, '>', "$builddir/debian/docs" or
  die "Cannot open $builddir/debian/docs for writing: $!\n";

foreach my $file (@extra_files){
  $file =~ s/ +//g;
  my $dest = 'docs/' . basename($file);
  copy $file, "$builddir/$dest" or
    die "Cannot copy $file to $builddir/$dest: $!\n";
  print DOCS "$dest\n";
}
close DOCS;

foreach my $script (qw(Preinst Postinst Prerm Postrm)) {
    next unless defined $control{$script};
    my $dest = 'debian/' . lc($script);
    copy $control{$script}, "$builddir/$dest" or
      die "Cannot copy $script to $builddir/$dest: $!\n";
}

write_control_file($builddir, \%control);

if ($control{'Changelog'}) {
  copy $control{'Changelog'}, "$builddir/debian/changelog" or
    die "Cannot copy changelog file $control{'Changelog'}: $!\n";
} else {
  make_changelog($builddir, \%control);
}


if ($control{'Readme'}) {
  copy $control{'Readme'}, "$builddir/debian/README.Debian.in" or
    die "Cannot copy README file $control{'Readme'}: $!\n";
}

# Make substitutions in the Readme
make_readme($builddir, \%control);

# Copy a copyright file, otherwise use GPL2
if ($control{'Copyright'}) {
  copy $control{'Copyright'}, "$builddir/debian/copyright" or
    die "Cannot copy copyright file $control{'Copyright'}: $!\n";
}

chdir $builddir;
unlink glob "debian/*.in";

my @build_cmd;
if ($full_package) {
    @build_cmd = (qw(dpkg-buildpackage -rfakeroot), ($arch ? "-a$arch" : ()));
} elsif ($source_only) {
    @build_cmd = (qw(dpkg-buildpackage -S -rfakeroot));
} else {
    @build_cmd = (($arch ? ("dpkg-architecture", "-a$arch", "-c") : ()),
                  qw(fakeroot debian/rules binary));
}
system @build_cmd;
my $err = $? >> 8;
chdir '..';
die "Error in the build process: exit status $err\n" if $err;

print "\nThe package has been created.\n";
print "Attention, the package has been created in the current directory,\n";
print "not in \"..\" as indicated by the message above!\n";
exit 0;

sub read_control_file {
  my ($builddir, $control, $specific_arch, $file) = @_;
  my @control = ();
  my $in;

  open($in, "$builddir/debian/control.in") or
    die "Cannot open control file: $!\n";
  read_control_file_section($in, \@control) or
    die "error: empty control file\n";
  close $in;

  # Set some field defaults: Maintainer, Architecture
  my $fullname = $ENV{DEBFULLNAME};
  ($fullname) = split ',', (getpwuid $>)[6]
    unless defined $fullname;

  my ($username, $systemname);
  for (qw(DEBEMAIL EMAIL)) {
      ($username, $systemname) = split '@', $ENV{$_}
      if !$username and defined $ENV{$_};
  }
  $username ||= $ENV{USER} || $ENV{LOGNAME} || (getpwuid $>)[0];
  chomp($systemname ||= qx(cat /etc/mailname 2>&- || hostname --fqdn));

  %{$control} = @control;

  $control->{'Maintainer'} = "$fullname <$username\@$systemname>";

  $control->{'Architecture'} = $specific_arch ? 'any' : 'all';

  open($in, $file) or
    die "Cannot open control file $file: $!\n";

  @control = ();
  read_control_file_section($in, \@control) or
    die "error: empty control file\n";
  close $in;

  for (my $i = 0; $i < $#control; $i += 2) {
    my $k = $control[$i];
    my $v = $control[$i+1];
    if ($k eq "File") {
      my $vv = [];
      $vv = $control->{$k} if defined $control->{$k};
      push @{$vv}, $v;
      $control->{$k} = $vv;
    } else {
      $control->{$k} = $v;
    }
  }

  # If no Source entry was specified, copy Package:
  $control->{'Source'} ||= $control->{'Package'};

  # remove trailing whitespace
#  foreach my $key (keys %$control) {
#    $control->{$key} =~ s/\s$//;
#  }

}

sub read_control_file_section {
  my ($fh, $pfields) = @_;

  my ($cf,$v);
  while (<$fh>) {
    chomp;
    next if (m/^\s*$/ or m/^#/);

    # new field?
    if (/^(\S+)\s*:\s*(.*?)\s*$/) {
      ($cf,$v) = (ucfirst lc $1, $2);
      $cf =~ s/(?<=-)([a-z])/uc $1/eg;
      push @{$pfields}, $cf, $v;
    } elsif (/^(\s+\S.*)$/) {
      $v = $1;
      defined($cf) or die "syntax error in control file: no field specified\n";
      $pfields->[-1] .= "\n$v";
    } else {
      die "syntax error in control file: $_\n";
    }
  }

  return 1;
}


# Write control fields
sub control_fields {
  my $retval;
  my ($control, @fields) = @_;

  foreach my $str (@fields) {
    my $t = $control->{$str};
    if ($t) {
      $retval .= "$str: $t\n";
    }
  }

  return $retval;
}


sub write_control_file {
  my ($builddir, $control) = @_;
  open OUT, '>', "$builddir/debian/control" or
    die "Cannot open $builddir/debian/control for writing: $!\n";

  print OUT control_fields($control,
			   "Source",
			   "Section",
			   "Priority",
			   "Maintainer",
			   "Homepage",
			   "Build-Depends",
			   "Standards-Version");
  print OUT "\n";
  print OUT control_fields($control,
			   "Package",
			   "Architecture",
			   "Pre-Depends",
			   "Depends",
			   "Recommends",
			   "Suggests",
			   "Conflicts",
			   "Breaks",
			   "Provides",
			   "Replaces",
			   "Multi-Arch",
			   "Description");
  close OUT;
}


sub make_changelog {
  my ($builddir, $control) = @_;
  my ($version, $suite, $date);

  $version = $control->{'Version'} || "1.0";
  $suite = $control->{'Suite'} || "unstable";
  chomp ($date = qx(date -R));

  open OUT, '>', "$builddir/debian/changelog" or
    die "Couldn't write changelog: $!\n";
  print OUT <<EOINPUT;
$control->{Source} ($version) $suite; urgency=low

  * First version

 -- $control->{'Maintainer'}  $date
EOINPUT
  close OUT;
}


# Create the README.Debian file
sub make_readme {
  my ($builddir, $control) = @_;
  my ($content, $deps);

  open IN, "$builddir/debian/README.Debian.in" or
    die "Cannot open the README file: $!\n";
  $content = join '', <IN>;
  close IN;

  $content =~ s/\@packagename\@/$control->{'Package'}/g;

  $deps = control_fields($control,
			 "Pre-Depends",
			 "Depends",
			 "Recommends",
			 "Suggests",
			 "Conflicts",
                         "Breaks",
			 "Provides",
			 "Replaces");
  $deps ||= " ";
  $content =~ s/\@depends\@/$deps/g;
  open OUT, '>', "$builddir/debian/README.Debian" or
    die "Cannot open $builddir/debian/README.Debian for writing: $!\n";
  print OUT $content;
  close OUT;
}
