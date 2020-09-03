package com.waynelee.tasktimer.debug;

public class TestTiming {
    long taskId;
    long startTime;
    long duration;

    public TestTiming(long taskId, long startTime, long duration) {
        this.taskId = taskId;
        this.startTime = startTime / 1000; // Store seconds, not milliseconds
        this.duration = duration;
    }
}
