package io.warren.shared.ai.bt;

/**
 * Represents the execution status of a behavior tree node.
 */
public enum Status {
    /**
     * The node executed successfully
     */
    SUCCESS,
    
    /**
     * The node failed to execute
     */
    FAILURE,
    
    /**
     * The node is still executing and needs more time
     */
    RUNNING,
    /*
     * The node is sleeping
     */
    SLEEPING
}