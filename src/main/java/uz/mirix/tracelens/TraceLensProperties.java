package uz.mirix.tracelens;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("tracelens")
public class TraceLensProperties {

    private boolean enabled = true;
    private double sampleRate = 1.0d;
    private int maxSpans = 100;
    private Duration slowThreshold = Duration.ofMillis(250);

    private final Logging logging = new Logging();
    private final ServerTiming serverTiming = new ServerTiming();
    private final Web web = new Web();
    private final Sql sql = new Sql();
    private final HttpClient httpClient = new HttpClient();
    private final Redis redis = new Redis();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public double getSampleRate() { return sampleRate; }
    public void setSampleRate(double sampleRate) {
        if (sampleRate < 0 || sampleRate > 1) {
            throw new IllegalArgumentException("tracelens.sample-rate must be between 0 and 1");
        }
        this.sampleRate = sampleRate;
    }

    public int getMaxSpans() { return maxSpans; }
    public void setMaxSpans(int maxSpans) {
        if (maxSpans < 1) {
            throw new IllegalArgumentException("tracelens.max-spans must be >= 1");
        }
        this.maxSpans = maxSpans;
    }

    public Duration getSlowThreshold() { return slowThreshold; }
    public void setSlowThreshold(Duration slowThreshold) { this.slowThreshold = slowThreshold; }

    public Logging getLogging() { return logging; }
    public ServerTiming getServerTiming() { return serverTiming; }
    public Web getWeb() { return web; }
    public Sql getSql() { return sql; }
    public HttpClient getHttpClient() { return httpClient; }
    public Redis getRedis() { return redis; }

    public static final class Logging {
        private boolean enabled = true;
        private Mode mode = Mode.ALL;
        private boolean includeTraceId = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode; }
        public boolean isIncludeTraceId() { return includeTraceId; }
        public void setIncludeTraceId(boolean includeTraceId) { this.includeTraceId = includeTraceId; }

        public enum Mode { ALL, SLOW_ONLY }
    }

    public static final class ServerTiming {
        private boolean enabled = true;
        private int maxMetrics = 20;
        private int maxDescriptionLength = 80;
        private boolean includeDescriptions = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxMetrics() { return maxMetrics; }
        public void setMaxMetrics(int maxMetrics) { this.maxMetrics = Math.max(1, maxMetrics); }
        public int getMaxDescriptionLength() { return maxDescriptionLength; }
        public void setMaxDescriptionLength(int maxDescriptionLength) {
            this.maxDescriptionLength = Math.max(16, maxDescriptionLength);
        }
        public boolean isIncludeDescriptions() { return includeDescriptions; }
        public void setIncludeDescriptions(boolean includeDescriptions) {
            this.includeDescriptions = includeDescriptions;
        }
    }

    public static final class Web {
        private List<String> exclude = new ArrayList<>(List.of(
            "/actuator/**",
            "/favicon.ico",
            "/error"
        ));

        public List<String> getExclude() { return exclude; }
        public void setExclude(List<String> exclude) {
            this.exclude = exclude == null ? new ArrayList<>() : new ArrayList<>(exclude);
        }
    }

    public static final class Sql {
        private boolean enabled = true;
        private int maxLength = 180;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = Math.max(32, maxLength); }
    }

    public static final class HttpClient {
        private boolean enabled = true;
        private boolean includePath = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isIncludePath() { return includePath; }
        public void setIncludePath(boolean includePath) { this.includePath = includePath; }
    }

    public static final class Redis {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
