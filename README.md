
# lecousin.net - Java core framework

The core library provides mainly:
 * A Multi-Threading framework, allowing asynchronous programming
 * A new IO (Input/Output) model, much more flexible, and supporting asynchronous operations

It does not have any dependency, however the library [net.lecousin.framework.system](https://github.com/lecousin/java-framework-system "java-framework-system")
is recommended for better performances on disk operations (detection of physical drives).

## Build status

### Current version - branch master

![build status](https://travis-ci.org/lecousin/java-framework-core.svg?branch=master "Build Status")
![build status](https://ci.appveyor.com/api/projects/status/github/lecousin/java-framework-core?branch=master&svg=true "Build Status")
[![Codecov](https://codecov.io/gh/lecousin/java-framework-core/graph/badge.svg)](https://codecov.io/gh/lecousin/java-framework-core/branch/master)


[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=master&metric=alert_status)](https://sonarcloud.io/dashboard?branch=master&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=master&metric=bugs)](https://sonarcloud.io/dashboard?branch=master&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=master&metric=vulnerabilities)](https://sonarcloud.io/dashboard?branch=master&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=master&metric=code_smells)](https://sonarcloud.io/dashboard?branch=master&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=master&metric=coverage)](https://sonarcloud.io/dashboard?branch=master&id=lecousin_java-framework-core)

#### Version

[![Maven Central](https://img.shields.io/maven-central/v/net.lecousin/core.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.lecousin%22%20AND%20a%3A%22core%22)
 [![Javadoc](https://img.shields.io/badge/javadoc-0.20.7-brightgreen.svg)](https://www.javadoc.io/doc/net.lecousin/core/0.20.7)

### Current development - branch dev

![build status](https://travis-ci.org/lecousin/java-framework-core.svg?branch=dev "Build Status")
![build status](https://ci.appveyor.com/api/projects/status/github/lecousin/java-framework-core?branch=dev&svg=true "Build Status")
[![Codecov](https://codecov.io/gh/lecousin/java-framework-core/branch/dev/graph/badge.svg)](https://codecov.io/gh/lecousin/java-framework-core/branch/dev)

[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=dev&metric=alert_status)](https://sonarcloud.io/dashboard?branch=dev&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=dev&metric=bugs)](https://sonarcloud.io/dashboard?branch=dev&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=dev&metric=vulnerabilities)](https://sonarcloud.io/dashboard?branch=dev&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=dev&metric=code_smells)](https://sonarcloud.io/dashboard?branch=dev&id=lecousin_java-framework-core)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=lecousin_java-framework-core&branch=dev&metric=coverage)](https://sonarcloud.io/dashboard?branch=dev&id=lecousin_java-framework-core)


## Multi-threading

The multi-threading system is based on _physical_ resources for better performance:
 * One thread by available processor (CPU)
 * One thread by physical drive
 
Each unit of work is a _Task_, that may succeed with a result, fail with an exception, or be cancelled.
A task must use only one physical resource, so a process implying both CPU work and some operations on a drive
must be split into several tasks.

Because the multi-threading system allocates a single thread by CPU, if long running tasks are running, other
tasks may wait for those tasks to finish if all CPUs are used. While this may be acceptable on a single application
environment, it is recommended to split such long running tasks into smaller tasks.

A thread pool is also available for long running tasks that cannot be split,
or tasks using a library with functionalities that may use several physical resources.
They will be executed in separate threads so they won't block other tasks.

A monitoring of tasks executed is done so if a task is exceeding a maximum time, it is put aside to run a new thread
and continue executing other tasks. After a second maximum time, the thread is killed.

A task should not, but is allowed to block. In this case the blocked thread is interrupted and a new thread
is automatically launched to process other tasks for the same physical resource. Once the task is unblocked,
the thread is resumed as soon as another thread is available and can be stopped. For this, synchronized
sections should be avoided as much as possible (or be very short), instead a _synchronization point_ should
be used.

Different kinds of _synchronization point_ are available in the package net.lecousin.framework.concurrent.async,
such as JoinPoint, Async, AsyncSupplier... They allow to wait for one or more asynchronous operations
to finish (successfully or not), by listening to them.

By default, the order tasks are executed is based on tasks' priority,
then for the same priority in a first-in-first-out order.
This may be changed by providing a new implementation of TaskPriorityManager.

The multi-threading system handles CPU and drives tasks, for network asynchronous operations you can
use the library [net.lecousin.framework.network.core](https://github.com/lecousin/java-framework-network-core "java-framework-network-core").

For a better management of drives, you can use the library
[net.lecousin.framework.system](https://github.com/lecousin/java-framework-system "java-framework-system")
which will detect drives, their type and capabilities.

## IO Model

The model provided by Java is very basic and mainly based on streams (reading or writing forward).

Our model adds much more flexibility, by using interfaces that define the capabilities of an Input/Output
implementation such as Readable, Writable, Seekable, Resizable, Buffered...
By using those interfaces we can know which operations can be performed on an IO, but allow also a method
to specify what are the minimum expected capabilities.

For example a method that needs an IO on which it can write data, it can seek (move forward and backward),
and it can resize the IO can be defined as follow:

	public <T extends IO.Writable.Seekable & IO.Resizable> myMethod(T io) { ... }

In addition, the model add asynchronous operations (non-blocking) to improve multi-threading.

Various kind of I/O are available:
 - FileIO for files
 - LinkedIO to aggregate several IO into a single one
 - SubIO to extract a part as an IO
 - buffered implementation such as BufferedIO, PreBufferedReadable, ByteArrayIO, IOInMomoryOrFile...
 - OutputToInput allowing to see on one side a writable IO (a producer of data), and on antoher side
 a readable IO to consume the data
  - and more...

The package net.lecousin.framework.io.util provides also utilities such as AsyncProducer and AsyncConsumer,
BroadcastIO, LimitWriteOperations to queue writing operations...

See the javadoc of package net.lecousin.framework.io for more information. 
 
## Startup

To start the framework, one of the _start_ method can be called on the Application class.
This will initialize the environment for a single application.

Once initialized, the application instance can be retrieved through the LCCore.getApplication() method.

Multi-application environment implementations may come in the future, so the framework is designed such
as multiple applications may share the same environment, including the same multi-threading system.

## Logging

A logging system is also provided, in a similar way as other logging frameworks (using loggers and appenders).

The reason to provide again another logging system is to have a logging system capable to use our
multi-threading system and asynchronous IO operations.

Each time something is logged, this is done by using asynchronous operations and tasks such as the code
logging information is not blocked to avoid reducing performance because of logging.

Bridges between other logging systems and this logging system are available in 
[java-framework-log-bridges](https://github.com/lecousin/java-framework-log-bridges "java-framework-log-bridges")

## Memory management

It often happens that data is kept in memory to improve performance, typically a cache. Such implementations
can declare themselves to the MemoryManager. The MemoryManager is monitoring memory usage, so when available
memory becomes low, it will ask the implementations to free some memory.

In addition, when an application is idle (doing almost nothing) since several minutes, the MemoryManager may
decide to ask to free some memory to reduce the memory footprint of the application when it is idle.

## Utilities

Various other utilities are also available, such as collections, encoding, more flexible strings...