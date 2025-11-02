package graph.common;

public interface MetricsInterface {
    void reset();
    long getElapsedTime();
    void setElapsedTime(long nanos);
    String getSummary();
}

