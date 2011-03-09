package com.sun.jersey.qe.tests.servlet3.preliminaryapp;

import java.util.Collections;
import java.util.Set;
import javax.ws.rs.core.Application;

public class OneApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Collections.<Class<?>>singleton(One.class);
    }
}