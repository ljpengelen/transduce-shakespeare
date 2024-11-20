# Java Concurrency Stress Tests - Volatile

This Gradle project contains two [Java Concurrency Stress tests](https://openjdk.org/projects/code-tools/jcstress/)
that demonstrate how the order of statements involving a volatile field in one thread influences what is visible
in another thread.

Run the tests by executing `./gradlew jcstress`.
