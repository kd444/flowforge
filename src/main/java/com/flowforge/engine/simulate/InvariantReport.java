package com.flowforge.engine.simulate;

import java.util.ArrayList;
import java.util.List;

public final class InvariantReport {
    private final List<String> violations = new ArrayList<>();

    public void add(String violation) {
        violations.add(violation);
    }

    public boolean ok() {
        return violations.isEmpty();
    }

    public boolean isOk() {
        return ok();
    }

    public List<String> violations() {
        return List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations();
    }
}
