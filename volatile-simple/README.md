# Volatile demo

Run this application with `java src/main/java/nl/cofx/volatiledemo/VolatileDemo.java`.
It shows that if the value of a non-volatile variable is changed on one thread,
other threads do not see that change immediately.
If the value of a volatile variable is changed, however, other threads will see the new value immediately.
This is because the value of volatile variables is read from main memory directly, not from a cache.

This application requires Java 19 or newer.
In previous versions, `ExecutorService` did not implement `AutoCloseable`.
