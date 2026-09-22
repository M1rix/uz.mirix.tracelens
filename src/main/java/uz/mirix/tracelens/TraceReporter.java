package uz.mirix.tracelens;
/** Extension point for consuming completed traces. Implementations must be fast and thread-safe. */
@FunctionalInterface public interface TraceReporter { void report(TraceReport report); }
