package com.cycos.circuit.impl;

public class ProcesssContext {
    public String circuitUserId;
    public String command;

    public ProcesssContext(String circuitUserId, String command) {
        this.circuitUserId = circuitUserId;
        this.command = command;
    }
}