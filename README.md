# RingCentral Java Authorization demo


## Ref

https://www.eclipse.org/jetty/documentation/current/advanced-embedding.html


## Download Jetty

```
curl -o jetty-all-uber.jar https://repo1.maven.org/maven2/org/eclipse/jetty/aggregate/jetty-all/9.4.19.v20190610/jetty-all-9.4.19.v20190610-uber.jar
```


## Write code

Create file `MyWebSite.java` with source code as shown in this project.


### Compile

```
mkdir classes
javac -d classes -cp jetty-all-uber.jar MyWebSite.java
```


### Run

```
java -cp classes:jetty-all-uber.jar com.ringcentral.MyWebSite
```
