package com.myorg;

import java.util.List;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.constructs.Construct;

public class QuizonicCdkStack extends Stack {

  private final Vpc vpc;

  public QuizonicCdkStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public QuizonicCdkStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    this.vpc =
        Vpc.Builder.create(this, "QuizonicVPC")
            .maxAzs(2)
            .natGateways(1)
            .subnetConfiguration(
                List.of(
                    SubnetConfiguration.builder()
                        .cidrMask(24)
                        .name("PublicSubnet")
                        .subnetType(SubnetType.PUBLIC)
                        .build(),
                    SubnetConfiguration.builder()
                        .cidrMask(24)
                        .name("PrivateSubnet")
                        .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                        .build()))
            .build();
  }

  public Vpc getVpc() {
    return vpc;
  }
}
