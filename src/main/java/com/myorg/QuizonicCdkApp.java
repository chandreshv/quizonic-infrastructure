package com.myorg;

import software.amazon.awscdk.App;

public class QuizonicCdkApp {
  public static void main(final String[] args) {
    App app = new App();
    new QuizonicCdkStack(app, "QuizonicCdkStack");
    app.synth();
  }
}
