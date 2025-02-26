package com.chandresh.quizonic.stack;

import software.amazon.awscdk.App;

public class QuizonicCdkApp {
  public static void main(final String[] args) {
    App app = new App();
    // new QuizonicCdkStack(app, "QuizonicCdkStack");
    // new QuizonicPipelineStack(app, "QuizonicPipelineStack");
    new QuizonicIAMStack(app, "QuizonicIAMStack");
    new QuizonicPipelineStack(app, "QuizonicPipelineStack");
    app.synth();
  }
}
