package com.roofingcrm.background;

/**
 * Well-known job types. External integrations will add more; unknown types fail safely in the worker.
 */
public final class BackgroundJobTypes {

    public static final String NO_OP = "NO_OP";

    private BackgroundJobTypes() {
    }
}
