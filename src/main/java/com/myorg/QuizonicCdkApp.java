package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.Arrays;

public class QuizonicCdkApp {
    public static void main(final String[] args) {
        App app = new App();
        new QuizonicCdkStack(app, "QuizonicCdkStack");
        app.synth();
    }
}

