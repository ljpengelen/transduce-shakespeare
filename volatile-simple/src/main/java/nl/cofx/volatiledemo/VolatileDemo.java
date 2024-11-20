package nl.cofx.volatiledemo;

import java.util.concurrent.Executors;

/**
 * Adaptation of <a href="https://stackoverflow.com/questions/10764120/complete-example-for-use-of-volatile-key-word-in-java/10764147#10764147">
 * an answer on Stack Overflow</a>.
 * <p>
 * The first runnable submitted to the executor service will terminate once the value of
 * <code>STOP_RUNNING_VOLATILE</code> changes. The other runnable will not terminate when the value of
 * <code>STOP_RUNNING_NON_VOLATILE</code> changes.
 */
public class VolatileDemo {

    private static volatile boolean STOP_RUNNING_VOLATILE;
    private static boolean STOP_RUNNING_NON_VOLATILE;

    public static void main(String[] args) throws InterruptedException {
        try (var executorService = Executors.newCachedThreadPool()) {
            executorService.submit(() -> {
                var count = 0;
                while (!STOP_RUNNING_VOLATILE) {
                    count++;
                }

                System.out.println("Runnable checking volatile field terminated: " + count);
            });
            executorService.submit(() -> {
                var count = 0;
                while (!STOP_RUNNING_NON_VOLATILE) {
                    count++;
                }

                System.out.println("Runnable checking non-volatile field terminated: " + count);
            });
            Thread.sleep(10);
            STOP_RUNNING_VOLATILE = true;
            STOP_RUNNING_NON_VOLATILE = true;
        }
    }
}
