package personalAgents;

public class MemoryMonitor {
    private static final long MEGABYTE = 1024L * 1024L;
    private static Runtime runtime;

    public static void memory() {
        if (runtime == null) {
            // Get the Java runtime
            runtime = Runtime.getRuntime();
            // Run the garbage collector
            runtime.gc();
        } else {

            long memory = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Used memory is bytes: " + memory);
            System.out.println("Used memory is megabytes: "
                    + bytesToMegabytes(memory));
            runtime = Runtime.getRuntime();
            // Run the garbage collector
            runtime.gc();
        }
    }

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

}
