# Dành cho build trên replit:
[+] .replit:
```compile = "mvn -B clean compile"
run = "mvn -B clean package && echo 'Build completed! Find your plugin at target/ReZeroReturnByDeath-1.0-SNAPSHOT.jar'"

entrypoint = "pom.xml"
hidden = ["**/*.class"]

[packager]
language = "java"

[packager.features]
packageSearch = true

[languages.java]
pattern = "**/*.java"

[languages.java.languageServer]
start = "jdt-language-server"

[nix]
channel = "stable-23_11"

[debugger]
support = false```
[+] replit.nix:
```{ pkgs }: {
  deps = [
    pkgs.jdk21
    pkgs.maven
  ];
  env = {
    JAVA_HOME = "${pkgs.jdk21}";
  };
}```
