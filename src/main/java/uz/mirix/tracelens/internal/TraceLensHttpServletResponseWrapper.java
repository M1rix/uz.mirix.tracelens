package uz.mirix.tracelens.internal;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

final class TraceLensHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private final AtomicLong firstWriteNanos = new AtomicLong(-1L);
    private final AtomicBoolean beforeFirstWriteInvoked = new AtomicBoolean();
    private volatile Runnable beforeFirstWrite = () -> { };
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    TraceLensHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    void beforeFirstWrite(Runnable callback) {
        this.beforeFirstWrite = callback == null ? () -> { } : callback;
    }

    long firstWriteNanos() {
        return firstWriteNanos.get();
    }

    private void markWrite() {
        if (beforeFirstWriteInvoked.compareAndSet(false, true)) {
            beforeFirstWrite.run();
        }
        firstWriteNanos.compareAndSet(-1L, System.nanoTime());
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        if (outputStream == null) {
            ServletOutputStream delegate = super.getOutputStream();
            outputStream = new ServletOutputStream() {
                @Override public boolean isReady() { return delegate.isReady(); }
                @Override public void setWriteListener(WriteListener listener) { delegate.setWriteListener(listener); }
                @Override public void write(int value) throws IOException { markWrite(); delegate.write(value); }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                    markWrite();
                    delegate.write(bytes, offset, length);
                }
                @Override public void flush() throws IOException { delegate.flush(); }
                @Override public void close() throws IOException { delegate.close(); }
            };
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called");
        }
        if (writer == null) {
            Writer delegate = super.getWriter();
            writer = new PrintWriter(new Writer() {
                @Override public void write(char[] buffer, int offset, int length) throws IOException {
                    markWrite();
                    delegate.write(buffer, offset, length);
                }
                @Override public void flush() throws IOException { delegate.flush(); }
                @Override public void close() throws IOException { delegate.close(); }
            });
        }
        return writer;
    }
}
