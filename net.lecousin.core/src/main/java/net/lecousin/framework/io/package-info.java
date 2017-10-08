/**
 * New java Input/Output model adding more flexibility and asynchronous operations.
 * <p>
 * The model provided by Java is very basic and mainly based on streams (reading or writing forward).<br/>
 * Our model add two main additions:<ul>
 * <li>Flexibility by using interfaces that define the capabilities of an Input/Output implementation such
 * as Readable, Writable, Seekable, Resizable, Buffered...</li>
 * <li>Asynchronous operations allowing multi-threading</li>
 * </ul>
 * </p>
 * 
 * <h1>Interface defining capabilities</h1>
 * 
 * Each implementation defines its capabilities through a set of interfaces defined in {@link IO}:<ul>
 * <li>{@link IO.Readable} is similar to an InputStream: data can be read forward</li>
 * <li>{@link IO.Writable} is similar to an OutputStream: data can be written forward</li>
 * <li>{@link IO.Seekable} adds the capability to move backward or forward</li>
 * <li>{@link IO.Readable.Seekable} extends {@link IO.Readable}, {@link IO.Seekable} and add operations
 * to read data directly at a specific position</li>
 * <li>{@link IO.Writable.Seekable} extends {@link IO.Writable}, {@link IO.Seekable} and add operations
 * to write data directly at a specific position</li>
 * <li>{@link IO.PositionKnown} adds the capability to get the current position</li>
 * <li>{@link IO.Resizable} adds the capability to resize the IO</li>
 * <li>{@link IO.SizeKnown} adds the capability to know the current size of an IO</li>
 * <li>{@link IO.ReadableByteStream} adds the capability to read a single byte instead of a buffer</li>
 * <li>{@link IO.WritableByteStream} adds the capability to write a single byte instead of a buffer</li>
 * <li>{@link IO.Readable.Buffered} defines a buffered Readable and adds some operations that are
 * efficient when buffered</li>
 * <li>{@link IO.Writable.Buffered} defines a buffered Writable and adds some operations that are
 * efficient when buffered</li>
 * </ul>
 * 
 * <p>
 * By using those interfaces to define the capabilities of the IO adds much more flexibility:<ul>
 * <li>We can test the capabilities by using <code>instanceof</code> operator</li>
 * <li>We can declare the required capabilities in a method signature using generics like this:
 * <code>
 * public &lt;T extends IO.Readable.Seekable &amp; IO.Writable.Seekable&gt; void myMethod(T io);
 * </code>
 * </li>
 * </ul>
 * </p>
 * 
 * <p>
 * More details on each interface can be found on their respective Javadoc.
 * </p>
 * 
 * <p>
 * Bridges between Java IO and our model are available:
 * {@link net.lecousin.framework.io.IOFromInputStream},
 * {@link net.lecousin.framework.io.IOFromOutputStream},
 * {@link net.lecousin.framework.io.IOAsInputStream},
 * {@link net.lecousin.framework.io.IOAsOutputStream}.
 * </p>
 * 
 * <h1>Asynchronous operations</h1>
 * 
 * Each interface define both synchronous and asynchronous operations.<br/>
 * Synchronous operations should be avoided when not on a Buffered implementation, but are still
 * available when we want to keep an algorithm simple and synchronous, however those operations
 * may block the thread waiting for the operation to complete.<br/>
 * Asynchronous operations allow to design multi-threaded algorithms and to do not block threads.
 */
package net.lecousin.framework.io;
