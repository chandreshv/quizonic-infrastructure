package com.chandresh.quizonic.stack;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

public class QuizonicCodeBuildRole extends Construct {
  private Role codeBuildRole;

  public QuizonicCodeBuildRole(@NotNull Construct scope, @NotNull String id) {
    super(scope, id);

    codeBuildRole =
        Role.Builder.create(this, "QuizonicCodeBuildRole")
            .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
            .managedPolicies(
                List.of(
                    ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess"),
                    ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryFullAccess"),
                    ManagedPolicy.fromAwsManagedPolicyName("CloudWatchLogsFullAccess"),
                    ManagedPolicy.fromAwsManagedPolicyName("AWSCodeBuildDeveloperAccess")))
            .description("IAM Role that is assumed by CodeBuild for execution")
            .build();
  }
}
