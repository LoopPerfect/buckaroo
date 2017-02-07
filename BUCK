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

java_library(
  name = 'buckaroo',
  source = '8',
  target = '8',
  srcs = glob([
    'src/main/java/com/**/*.java',
  ]),
  deps = [
    ':guava',
    ':gson',
  ],
)

java_test(
  name = 'buckaroo-test',
  srcs = glob([
    'src/test/java/com/**/*.java',
  ]),
  deps = [
    ':buckaroo',
    ':guava',
    ':gson',
    ':hamcrest',
    ':junit',
  ],
)
