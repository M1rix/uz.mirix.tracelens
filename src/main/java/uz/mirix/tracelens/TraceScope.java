package uz.mirix.tracelens;
/** Auto-closeable custom span returned by TraceLens.span(...). */
@FunctionalInterface public interface TraceScope extends AutoCloseable { @Override void close(); }
