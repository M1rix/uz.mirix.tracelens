package uz.mirix.tracelens;

/** High-level category of work captured inside a request trace. */
public enum TraceCategory { APPLICATION, SQL, HTTP, REDIS, SERIALIZATION, CUSTOM }
