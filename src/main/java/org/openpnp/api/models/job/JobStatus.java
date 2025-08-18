package org.openpnp.api.models.job;

/**
 * DTO для статуса выполнения задания
 */
public class JobStatus {
    public enum State {
        STOPPED,
        PAUSED,
        RUNNING,
        PAUSING,
        STOPPING,
        ERROR
    }

    private State state;
    private String currentStep;
    private int totalSteps;
    private int completedSteps;
    private int errorCount;
    private String lastError;
    private long startTime;
    private long elapsedTime;
    private long estimatedTimeRemaining;

    public JobStatus() {
    }

    public JobStatus(State state, String currentStep, int totalSteps, int completedSteps) {
        this.state = state;
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
        this.completedSteps = completedSteps;
    }

    // Геттеры и сеттеры
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public long getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }

    public void setEstimatedTimeRemaining(long estimatedTimeRemaining) {
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }

    public double getProgressPercentage() {
        if (totalSteps == 0) {
            return 0.0;
        }
        return (double) completedSteps / totalSteps * 100.0;
    }
}