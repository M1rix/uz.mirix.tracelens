package uz.mirix.tracelens;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("tracelens")
public class TraceLensProperties {
    private boolean enabled=true; private double sampleRate=1.0d; private int maxSpans=100; private Duration slowThreshold=Duration.ofMillis(250);
    private final Logging logging=new Logging(); private final ServerTiming serverTiming=new ServerTiming(); private final Web web=new Web(); private final Sql sql=new Sql(); private final HttpClient httpClient=new HttpClient(); private final Redis redis=new Redis();
    public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public double getSampleRate(){return sampleRate;} public void setSampleRate(double v){if(v<0||v>1)throw new IllegalArgumentException("tracelens.sample-rate must be between 0 and 1");sampleRate=v;} public int getMaxSpans(){return maxSpans;} public void setMaxSpans(int v){if(v<1)throw new IllegalArgumentException("tracelens.max-spans must be >= 1");maxSpans=v;} public Duration getSlowThreshold(){return slowThreshold;} public void setSlowThreshold(Duration v){slowThreshold=v;}
    public Logging getLogging(){return logging;} public ServerTiming getServerTiming(){return serverTiming;} public Web getWeb(){return web;} public Sql getSql(){return sql;} public HttpClient getHttpClient(){return httpClient;} public Redis getRedis(){return redis;}
    public static final class Logging { private boolean enabled=true; private Mode mode=Mode.ALL; private boolean includeTraceId=true; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public Mode getMode(){return mode;} public void setMode(Mode v){mode=v;} public boolean isIncludeTraceId(){return includeTraceId;} public void setIncludeTraceId(boolean v){includeTraceId=v;} public enum Mode{ALL,SLOW_ONLY} }
    public static final class ServerTiming { private boolean enabled=true; private int maxMetrics=20; private int maxDescriptionLength=80; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public int getMaxMetrics(){return maxMetrics;} public void setMaxMetrics(int v){maxMetrics=Math.max(1,v);} public int getMaxDescriptionLength(){return maxDescriptionLength;} public void setMaxDescriptionLength(int v){maxDescriptionLength=Math.max(16,v);} }
    public static final class Web { private List<String> exclude=new ArrayList<>(List.of("/actuator/**","/favicon.ico","/error")); public List<String> getExclude(){return exclude;} public void setExclude(List<String> v){exclude=v==null?new ArrayList<>():new ArrayList<>(v);} }
    public static final class Sql { private boolean enabled=true; private int maxLength=180; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public int getMaxLength(){return maxLength;} public void setMaxLength(int v){maxLength=Math.max(32,v);} }
    public static final class HttpClient { private boolean enabled=true; private boolean includePath=false; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public boolean isIncludePath(){return includePath;} public void setIncludePath(boolean v){includePath=v;} }
    public static final class Redis { private boolean enabled=true; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} }
}
