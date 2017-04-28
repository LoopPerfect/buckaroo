remote_file(
  name = 'guava-jar',
  out = 'guava-21.0.jar',
  url = 'mvn:com.google.guava:guava:jar:21.0',
  sha1 = '3a3d111be1be1b745edfa7d91678a12d7ed38709',
)

prebuilt_jar(
  name = 'guava',
  source_jar = ':guava-jar',
  binary_jar = ':guava-jar',
  javadoc_url = 'https://google.github.io/guava/releases/21.0/api/docs/',
)

remote_file(
  name = 'gson-jar',
  out = 'gson-2.8.0.jar',
  url = 'mvn:com.google.code.gson:gson:jar:2.8.0',
  sha1 = 'c4ba5371a29ac9b2ad6129b1d39ea38750043eff',
)

prebuilt_jar(
  name = 'gson',
  source_jar = ':gson-jar',
  binary_jar = ':gson-jar',
  javadoc_url = 'https://google.github.io/gson/apidocs/com/google/gson/Gson.html',
)

remote_file(
  name = 'junit-jar',
  out = 'junit-4.12.jar',
  url = 'mvn:junit:junit:jar:4.12',
  sha1 = '2973d150c0dc1fefe998f834810d68f278ea58ec',
)

prebuilt_jar(
  name = 'junit',
  source_jar = ':junit-jar',
  binary_jar = ':junit-jar',
  javadoc_url = 'http://junit.sourceforge.net/javadoc/',
)

remote_file(
  name = 'hamcrest-jar',
  out = 'hamcrest-all-1.3.jar',
  url = 'mvn:org.hamcrest:hamcrest-all:jar:1.3',
  sha1 = '63a21ebc981131004ad02e0434e799fd7f3a8d5a',
)

prebuilt_jar(
  name = 'hamcrest',
  source_jar = ':hamcrest-jar',
  binary_jar = ':hamcrest-jar',
  javadoc_url = 'http://hamcrest.org/JavaHamcrest/javadoc/1.3/',
)

remote_file(
  name = 'slf4j-api-jar',
  out = 'slf4j-api-1.7.6.jar',
  url = 'mvn:org.slf4j:slf4j-api:jar:1.7.6',
  sha1 = '562424e36df3d2327e8e9301a76027fca17d54ea',
)

prebuilt_jar(
  name = 'slf4j-api',
  source_jar = ':slf4j-api-jar',
  binary_jar = ':slf4j-api-jar',
)

remote_file(
  name = 'slf4j-nop-jar',
  out = 'slf4j-nop-1.7.6.jar',
  url = 'mvn:org.slf4j:slf4j-nop:jar:1.7.6',
  sha1 = '3d219ee4ed4965348a630ff6ef2a5418032b9466',
)

prebuilt_jar(
  name = 'slf4j-nop',
  source_jar = ':slf4j-nop-jar',
  binary_jar = ':slf4j-nop-jar',
)

remote_file(
  name = 'jsch-jar',
  out = 'jsch-0.1.54.jar',
  url = 'mvn:com.jcraft:jsch:jar:0.1.54',
  sha1 = 'da3584329a263616e277e15462b387addd1b208d',
)

prebuilt_jar(
  name = 'jsch',
  source_jar = ':jsch-jar',
  binary_jar = ':jsch-jar',
)

remote_file(
  name = 'jparsec-jar',
  out = 'jparsec-3.0.jar',
  url = 'mvn:org.jparsec:jparsec:jar:3.0',
  sha1 = 'd58e152623bb664f4c8e6d7ce8db7fa7fda2f7d5',
)

prebuilt_jar(
  name = 'jparsec',
  source_jar = ':jparsec-jar',
  binary_jar = ':jparsec-jar',
)

remote_file(
  name = 'jgit-jar',
  out = 'jgit-4.5.0.jar',
  url = 'mvn:org.eclipse.jgit:org.eclipse.jgit:jar:4.5.0.201609210915-r',
  sha1 = '3e3d0b73dcf4ad649f37758ea8502d92f3d299de',
)


remote_file(
  name = 'jimfs-jar',
  out = 'jimfs-1.1.jar',
  url = 'mvn:com.google.jimfs:jimfs:jar:1.1',
  sha1 = '8fbd0579dc68aba6186935cc1bee21d2f3e7ec1c',
)

prebuilt_jar(
  name = 'jimfs',
  source_jar = ':jimfs-jar',
  binary_jar = ':jimfs-jar',
  javadoc_url = 'https://google.github.io/jimfs/releases/1.1/api/docs/',
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
  out = 'jgit-4.5.0-fixed.jar',
)

prebuilt_jar(
  name = 'jgit',
  source_jar = ':jgit-jar-fixed',
  binary_jar = ':jgit-jar-fixed',
)

remote_file(
  name = 'mustache-jar',
  out = 'mustache-0.8.9.jar',
  url = 'mvn:com.github.spullara.mustache.java:compiler:jar:0.8.9',
  sha1 = 'dba18d9ea10323c02ae5bc4c2121048853b233e1',
)

prebuilt_jar(
  name = 'mustache',
  source_jar = ':mustache-jar',
  binary_jar = ':mustache-jar',
)

remote_file(
  name = 'httpclient-jar',
  out = 'httpclient-4.3.6.jar',
  url = 'mvn:org.apache.httpcomponents:httpclient:jar:4.3.6',
  sha1 = '4c47155e3e6c9a41a28db36680b828ced53b8af4',
)

prebuilt_jar(
  name = 'httpclient',
  source_jar = ':httpclient-jar',
  binary_jar = ':httpclient-jar',
)

remote_file(
  name = 'httpcore-jar',
  out = 'httpcore-4.4.6.jar',
  url = 'mvn:org.apache.httpcomponents:httpcore:jar:4.4.6',
  sha1 = 'e3fd8ced1f52c7574af952e2e6da0df8df08eb82',
)

prebuilt_jar(
  name = 'httpcore',
  source_jar = ':httpcore-jar',
  binary_jar = ':httpcore-jar',
)

remote_file(
  name = 'commons-logging-jar',
  out = 'commons-logging-1.2.jar',
  url = 'mvn:commons-logging:commons-logging:jar:1.2',
  sha1 = '4bfc12adfe4842bf07b657f0369c4cb522955686',
)

prebuilt_jar(
  name = 'commons-logging',
  source_jar = ':commons-logging-jar',
  binary_jar = ':commons-logging-jar',
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
    ':guava',
    ':gson',
    ':jparsec',
    ':jgit',
    ':mustache',
    ':slf4j-api',
    ':slf4j-nop',
    ':jsch',
    ':jimfs',
    ':httpclient',
    ':httpcore',
    ':commons-logging',
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
  name = 'buckaroo-test',
  source = '8',
  target = '8',
  srcs = glob([
    'src/test/java/com/**/*.java',
  ]),
  deps = [
    ':buckaroo',
    ':guava',
    ':gson',
    ':hamcrest',
    ':junit',
    ':jimfs',
    ':jparsec',
  ],
)
