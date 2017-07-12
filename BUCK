include_defs('//maven_jar.bucklet')

maven_jar(
  name = 'javatuples',
  id = 'org.javatuples:javatuples:1.2',
  src_sha1 = 'a7495f5370bdfcf46c6f3c6ed0badf52877aa467',
  bin_sha1 = '507312ac4b601204a72a83380badbca82683dd36',
)

maven_jar(
  name = 'guava',
  id = 'com.google.guava:guava:21.0',
  src_sha1 = 'b9ed26b8c23fe7cd3e6b463b34e54e5c6d9536d5',
  bin_sha1 = '3a3d111be1be1b745edfa7d91678a12d7ed38709',
)

maven_jar(
  name = 'okio',
  id = 'com.squareup.okio:okio:1.6.0',
  src_sha1 = 'fb6ec0fbaa0229088b0d3dfe3b1f9d24add3e775',
  bin_sha1 = '98476622f10715998eacf9240d6b479f12c66143',
)

maven_jar(
  name = 'okhttp',
  id = 'com.squareup.okhttp3:okhttp:3.2.0',
  src_sha1 = 'c6e2cb305d0da8820c335f20db73bfc69b2156ed',
  bin_sha1 = 'f7873a2ebde246a45c2a8d6f3247108b4c88a879',
)

maven_jar(
  name = 'reactive-streams',
  id = 'org.reactivestreams:reactive-streams:1.0.0.final',
  src_sha1 = 'be2d37620962b2c851f2c292d5931b7ee9398d16',
  bin_sha1 = 'e62537f8218067557606489b2790cd74dda39059',
)

maven_jar(
  name = 'rxjava',
  id = 'io.reactivex.rxjava2:rxjava:2.1.0',
  src_sha1 = 'e11f8b99796174be4f0f255d717f295d5e49a53b',
  bin_sha1 = '2fdf84dedcaaeabb9d70cde9dbb8aad4eccb80a1',
)

maven_jar(
  name = 'junit',
  id = 'junit:junit:4.12',
  src_sha1 = 'a6c32b40bf3d76eca54e3c601e5d1470c86fcdfa',
  bin_sha1 = '2973d150c0dc1fefe998f834810d68f278ea58ec',
)

maven_jar(
  name = 'hamcrest',
  id = 'org.hamcrest:hamcrest-all:1.3',
  src_sha1 = '47e033b7ab18c5dbd5fe29fc1bd5a40afe028818',
  bin_sha1 = '63a21ebc981131004ad02e0434e799fd7f3a8d5a',
)

maven_jar(
  name = 'gson',
  id = 'com.google.code.gson:gson:2.8.0',
  src_sha1 = 'baf95d8519fc1a11d388f8543cb40cd2bb9d66dc',
  bin_sha1 = 'c4ba5371a29ac9b2ad6129b1d39ea38750043eff',
)

maven_jar(
  name = 'jparsec',
  id = 'org.jparsec:jparsec:3.0',
  src_sha1 = '58f1043c4808436dd5a08bd3607d3685ca588b48',
  bin_sha1 = 'd58e152623bb664f4c8e6d7ce8db7fa7fda2f7d5',
)

maven_jar(
  name = 'jimfs',
  id = 'com.google.jimfs:jimfs:1.1',
  src_sha1 = 'a2e6f6d75b7fa7e8eedb3062e5bfd24cc9fe8591',
  bin_sha1 = '8fbd0579dc68aba6186935cc1bee21d2f3e7ec1c',
)

maven_jar(
  name = 'jsch',
  id = 'com.jcraft:jsch:0.1.54',
  src_sha1 = '91d6069df9be9e076bdb124e82fc2a9af9547616',
  bin_sha1 = 'da3584329a263616e277e15462b387addd1b208d',
)

maven_jar(
  name = 'mustache',
  id = 'com.github.spullara.mustache.java:compiler:0.8.9',
  src_sha1 = 'bacb2f264f8d920fa9147a6d8c8e4ed68428fd09',
  bin_sha1 = 'dba18d9ea10323c02ae5bc4c2121048853b233e1',
)

maven_jar(
  name = 'jansi',
  id = 'org.fusesource.jansi:jansi:1.15',
  src_sha1 = 'c72a0cb311211de27f2a3acd867b9281edf7c320',
  bin_sha1 = '5292bc138cb1412ea940551c667f8ec4da52d249',
)

remote_file(
  name = 'jgit-jar',
  out = 'jgit-4.5.0.jar',
  url = 'mvn:org.eclipse.jgit:org.eclipse.jgit:jar:4.5.0.201609210915-r',
  sha1 = '3e3d0b73dcf4ad649f37758ea8502d92f3d299de',
)

# We need to strip out the jar signing
# TODO: Find a better approach!
genrule(
  name = 'jgit-jar-fixed',
  srcs = [
    '//:jgit-jar',
  ],
  cmd =
    'cp buck-out/gen/jgit-jar/jgit-4.5.0.jar $OUT && ' +
    'zip -d $OUT META-INF/*.RSA META-INF/*.SF META-INF/*.MF',
  cmd_exe =
    'copy $SRCDIR\\buck-out\\gen\\jgit-jar\\jgit-4.5.0.jar $OUT && ' +
    'zip -d $OUT META-INF/*.RSA META-INF/*.SF META-INF/*.MF',
  out = 'jgit-4.5.0-fixed.jar',
)

prebuilt_jar(
  name = 'jgit',
  source_jar = ':jgit-jar-fixed',
  binary_jar = ':jgit-jar-fixed',
)

java_library(
  name = 'buckaroo',
  source = '8',
  target = '8',
  srcs = glob([
    'src/main/java/com/**/*.java',
  ]),
  resources = glob([
    'src/main/resources/**/*.mustache',
    'src/main/resources/**/*.cpp',
    'src/main/resources/**/*.txt',
  ]),
  resources_root = 'src/main/resources',
  deps = [
    ':javatuples',
    ':rxjava',
    ':reactive-streams',
    ':okio',
    ':okhttp',
    ':guava',
    ':gson',
    ':jparsec',
    ':jgit',
    ':mustache',
    ':jsch',
    ':jimfs',
    ':jansi',
  ],
)

java_binary(
  name = 'buckaroo-cli',
  main_class = 'com.loopperfect.buckaroo.Main',
  deps = [
    ':buckaroo',
  ],
)

java_test(
  name = 'buckaroo-unit',
  source = '8',
  target = '8',
  srcs = glob([
    'src/test/java/com/**/*.java',
  ]),
  deps = [
    ':buckaroo',
    ':javatuples',
    ':rxjava',
    ':reactive-streams',
    ':okhttp',
    ':guava',
    ':gson',
    ':hamcrest',
    ':junit',
    ':jimfs',
    ':jparsec',
    ':jansi',
  ],
)

java_test(
  name = 'buckaroo-integration',
  source = '8',
  target = '8',
  srcs = glob([
    'src/integration/java/com/**/*.java',
  ]),
  deps = [
    ':buckaroo',
    ':javatuples',
    ':rxjava',
    ':reactive-streams',
    ':okhttp',
    ':guava',
    ':gson',
    ':hamcrest',
    ':jgit',
    ':junit',
    ':jimfs',
    ':jparsec',
    ':jansi',
  ],
)

genrule(
  name = 'debian',
  out  = 'out',
  srcs = [
    'debian/buckaroo',
    'debian/buckaroo.equivs',
    'Changelog',
    'LICENSE',
    'README.md',
  ],
  cmd = '&&'.join([
    'mkdir $OUT',
    'cp -r $SRCDIR/* $OUT',
    'cp -r $SRCDIR/debian/* $OUT',
    'cp $(location :buckaroo-cli) $OUT/buckaroo.jar',
    'cd $OUT',
    'equivs-build buckaroo.equivs'
  ])
)
