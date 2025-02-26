package com.chandresh.quizonic.stack;

import java.util.List;
import software.amazon.awscdk.CfnParameter;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.*;
import software.constructs.Construct;

public class QuizonicIAMStack extends Stack {
  public QuizonicIAMStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public QuizonicIAMStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    CfnParameter arnUserParameter =
        CfnParameter.Builder.create(this, "UserArn")
            .type("String")
            .description("ARN of the user who will assume the role")
            .build();

    Role cdkDeployeRole =
        Role.Builder.create(this, "CDKDeployerRole")
            .assumedBy(new ArnPrincipal(arnUserParameter.getValueAsString()))
            .roleName("CDKDeployerRole")
            .build();

    PolicyStatement policyStatementForCDKDeployment =
        PolicyStatement.Builder.create()
            .effect(Effect.ALLOW)
            .actions(
                List.of("cloudformation:*", "iam:PassRole", "s3:*", "ec2:*", "ssm:GetParameter", "ssm:PutParameter"))
            .resources(List.of("*"))
            .build();

    PolicyStatement policyStatementToAssumeRole =
        PolicyStatement.Builder.create()
            .actions(List.of("sts:AssumeRole"))
            .effect(Effect.ALLOW)
            .resources(List.of("*"))
            .build();

    cdkDeployeRole.addToPolicy(policyStatementForCDKDeployment);
    cdkDeployeRole.addToPolicy(policyStatementToAssumeRole);
  }
}
