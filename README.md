# KButtplug

Kotlin implementation of the [Buttplug Rust FFI](https://github.com/buttplugio/buttplug-rs-ffi).

[![](https://img.shields.io/maven-central/v/dev.franckyi.kbuttplug/kbuttplug?label=kbuttplug)](https://search.maven.org/artifact/dev.franckyi.kbuttplug/kbuttplug)
[![](https://img.shields.io/maven-central/v/dev.franckyi.kbuttplug/kbuttplug-natives?label=kbuttplug-natives)](https://search.maven.org/artifact/dev.franckyi.kbuttplug/kbuttplug-natives)
[![](https://img.shields.io/maven-central/v/dev.franckyi.kbuttplug/kbuttplug-loghandler-slf4j?label=kbuttplug-loghandler-slf4j)](https://search.maven.org/artifact/dev.franckyi.kbuttplug/kbuttplug-loghandler-slf4j)
[![](https://img.shields.io/github/license/Franckyi/kbuttplug)](https://mit-license.org/)

> ***NOTE: This is a work in progress and the API might still change significantly. Make sure to check the changelog
before upgrading versions.***

* [Installation](#Installation)
  * [Gradle (Groovy)](#Gradle-(Groovy))
  * [Gradle (Kotlin)](#Gradle-(Kotlin))
  * [Maven](#Maven)
* [Usage](#Usage)
  * [Initializing](#Initializing)
  * [Logging](#Logging)
  * [Connecting](#Connecting)
  * [Scanning](#Scanning)
  * [Controlling](#Controlling)
  * [Disconnecting](#Disconnecting)
* [Building](#Building)

## Installation

The KButtplug API is located in the `kbuttplug` module.

In order to use the API, the `buttplug_rs_ffi` native library needs to be on the classpath during runtime. KButtplug
packages a library containing this native library: `kbuttplug-natives`.
You can either use the multiplatform artifact (without classifier) or a platform-specific artifact using one of
the `windows`, `macos` or `linux` classifiers.

### Gradle (Groovy)

```groovy
implementation 'dev.franckyi.kbuttplug:kbuttplug:0.2.2'
runtimeOnly 'dev.franckyi.kbuttplug:kbuttplug-natives:2.0.4'
```

### Gradle (Kotlin)

```kotlin
implementation("dev.franckyi.kbuttplug:kbuttplug:0.2.2")
runtimeOnly("dev.franckyi.kbuttplug:kbuttplug-natives:2.0.4")
```

### Maven

```xml
<dependency>
    <groupId>dev.franckyi.kbuttplug</groupId>
    <artifactId>kbuttplug</artifactId>
    <version>0.2.2</version>
</dependency>
<dependency>
    <groupId>dev.franckyi.kbuttplug</groupId>
    <artifactId>kbuttplug-natives</artifactId>
    <version>2.0.4</version>
</dependency>
```

## Usage

### Initializing

First, you need to register a ButtplugClient:

```kotlin
// Instanciate a ButtplugClient
val client = ButtplugClient.create("My Client")
```

You can register callbacks on the client to handle events received from the server, like for example:

```kotlin
// Prints error messages to stdout
client.onError = { error -> println("Error: $error") }

// Prints new device names to stdout
client.onDeviceAdded = { device -> println("New device added: ${device.name}") } 
```

### Logging

KButtplug supports registering a log handler for log messages sent by the native library.
You can either activate the default log handler that prints the log messages of the library to `stdout`,
or you can register a custom log handler that you can use to log messages to your own logger.

```kotlin
// Activate the builtin log handler that prints log messages to stdout (cannot deactivate)
ButtplugLogHandler.activateBuiltinLogger()

// Register a simple log handler
val logHandler = ButtplugLogHandler.createSimpleLogger { println(it) }

// Register a SLF4J log handler
val logger = LoggerFactory.getLogger("MyLogger")
val logHandler = ButtplugLogHandler.createSlf4jLogger(logger)

// Remember to close the log handler when you are done
logHandler.close()
```

### Connecting

Then, you can connect to a ButtplugServer using websockets,
or you can use an embedded server that runs within your application:

```kotlin
// Connect to an external server (for example: a server running with Intiface Desktop)
client.connectWebsocket("ws://localhost:12345")

// Connect to an embedded server (not recommended, causes memory leaks in some cases)
client.connectLocal("My Server")
```

> Note: Most API methods on ButtplugClient and ButtplugDevice are executed asynchronously
> and return a CompletableFuture.

### Scanning

After the connection is done, you can start scanning for devices:

```kotlin
client.startScanning()
```

Every time a new device is found, it will be added to the `devices` map of the client.

### Controlling

The client exposes the `devices` map that you can use to control the device of your choice:

```kotlin
// Vibrate all devices at 50% speed
client.devices.forEach { it.value.vibrate(0.5) }
```

### Disconnecting

Once you are done, you can disconnect:

```kotlin
// Disconnect from the server
client.disconnect().whenComplete { _, _ ->
  // Remember to  close the client when you are done
  client.close()
}
```

## Building

You can build KButtplug using these commands:

```shell
git clone https://github.com/Franckyi/KButtplug
cd KButtplug
./gradlew build
```
