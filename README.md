
# lecousin.net - Java core framework

The core library provides:
 * A Multi-Threading framework, allowing asynchronous programming
 * A new IO (Input/Output) model, flexible, and providing asynchronous operations

It does not have any dependency, however the library [net.lecousin.framework.system](https://github.com/lecousin/java-framework-system "java-framework-system")
is recommended for better performances on disk operations (detection of physical drives).

## Build status

Current version: 0.8.0

Master: ![build status](https://travis-ci.org/lecousin/java-framework-core.svg?branch=master "Build Status")

Branch 0.8: ![build status](https://travis-ci.org/lecousin/java-framework-core.svg?branch=0.8 "Build Status")

Branch 0.9: ![build status](https://travis-ci.org/lecousin/java-framework-core.svg?branch=0.9 "Build Status") 

## Multi-threading

The multi-threading system of this library is based on _physical_ resources for better performance:
 * One thread by available processor (CPU)
 * One thread by physical drive
 
Each unit of work is a _Task_, that may succeed with a result, fail with an exception, or be cancelled.
A task must use only one physical resource, so a process implying both CPU work and some operations on a drive
must be split into several tasks.

A task should not, but is allowed to block. In this case the blocked thread is interrupted and a new thread
is automatically launched to process other tasks for the same physical resource. Once the task is unblocked,
the thread is resumed as soon as another thread is available and can be stopped.

By default, the order tasks are executed is based on tasks' priority,
then for the same priority in a first-in-first-out order.
This may be changed by providing a new implementation of TaskPriorityManager.

Different kinds of _synchronization point_ are available in the package net.lecousin.framework.concurrent.synch,
such as JoinPoint, SynchronizationPoint, AsyncWork... They allow to wait for one or more asynchronous operations
to finish (successfully or not), by listening to them.

## IO Model

The model provided by Java is very basic and mainly based on streams (reading or writing forward).

Our model add two main additions:
 * Flexibility by using interfaces that define the capabilities of an Input/Output implementation such as Readable, Writable, Seekable, Resizable, Buffered...
 * Asynchronous operations allowing multi-threading
 
## Startup

To start the framework, one of the _start_ method can be called on the Application class.
This will initialize the environment for a single application.

Once initialized, the application instance can be retrieved through the LCCore.getApplication() method.

Multi-application environment implementations may come in the future, so the framework is designed such
as multiple applications may share the same environment, including the same multi-threading system.

## Logging

A logging system is also provided, in a similar way as other logging frameworks.
TODO

## Memory management

TODO

## Locale

TODO