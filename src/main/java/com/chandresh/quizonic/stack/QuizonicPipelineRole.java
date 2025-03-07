package com.chandresh.quizonic.stack;

import java.util.List;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.iam.*;
import software.constructs.Construct;

public class QuizonicPipelineRole extends Construct {

  public Role pipelineRole;

  public QuizonicPipelineRole(final Construct scope, final String id) {
    super(scope, id);

    pipelineRole =
        Role.Builder.create(this, "QuizonicPipelineRole")
            .assumedBy(new ServicePrincipal("codepipeline.amazonaws.com"))
            .description("IAM Role that is assumed by CodePipeline for execution")
            .managedPolicies(
                List.of(
                    ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess"),
                    ManagedPolicy.fromAwsManagedPolicyName("AWSSSMReadOnlyAccess")))
            .build();

    pipelineRole.addToPolicy(
        PolicyStatement.Builder.create()
            .effect(Effect.ALLOW)
            .actions(List.of("sts:AssumeRole"))
            .resources(
                List.of(
                    "arn:aws:iam::" + Stack.of(this).getAccount() + ":role/QuizonicCodeBuildRole"))
            .build());

    pipelineRole.addToPolicy(
        PolicyStatement.Builder.create()
            .effect(Effect.ALLOW)
            .actions(
                List.of(
                    "codebuild:StartBuild",
                    "codebuild:BatchGetBuilds",
                    "codebuild:StopBuild",
                    "codebuild:ListBuilds"))
            .resources(
                List.of(
                    "arn:aws:ia,::"
                        + Stack.of(this).getAccount()
                        + ":project/QuizonicBuildProject"))
            .build());
  }
}
