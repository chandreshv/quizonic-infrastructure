package com.chandresh.quizonic.stack;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubTrigger;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.constructs.Construct;

public class QuizonicPipelineStack extends Stack {
  public QuizonicPipelineStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public QuizonicPipelineStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    Artifact sourceArtifact = new Artifact();
    Artifact buildArtifact = new Artifact();

    Bucket artifactBucket =
        Bucket.Builder.create(this, "QuizonicPipelineArtifactBucket")
            .removalPolicy(RemovalPolicy.DESTROY)
            .build();

    SsmClient ssmClient = SsmClient.create();
    GetParameterRequest parameterRequest = GetParameterRequest.builder()
            .name("/quizonic/github-token")  // Change this to your actual SSM parameter name
            .withDecryption(true)
            .build();

    GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
    String githubToken = parameterResponse.parameter().value();

    // Use the retrieved value as SecretValue in GitHubSourceAction
    SecretValue githubSecret = SecretValue.unsafePlainText(githubToken);

    GitHubSourceAction sourceAction =
        GitHubSourceAction.Builder.create()
            .actionName("GitHub_Source")
            .owner("chandreshv")
            .repo("quizonic-service")
            .branch("main")
            .oauthToken(githubSecret)
            .output(sourceArtifact)
            .trigger(GitHubTrigger.WEBHOOK)
            .build();

    PipelineProject buildProject =
        PipelineProject.Builder.create(this, "QuizonicBuildProject")
            .projectName("QuizonicBuildProject")
            .environment(
                BuildEnvironment.builder()
                    .buildImage(LinuxBuildImage.fromCodeBuildImageId("aws/codebuild/amazonlinux2-x86_64-standard:5.0"))
                    .computeType(ComputeType.SMALL)
                    .build())
            .buildSpec(
                BuildSpec.fromObject(
                    Map.of(
                        "version", "0.2",
                        "phases",
                            Map.of(
                                "install", Map.of("commands", List.of("mvn install")),
                                "build",
                                    Map.of(
                                        "commands", List.of("mvn spotless:check", "mvn package"))),
                        "artifacts", Map.of("files", List.of("target/*.jar")))))
            .build();

    CodeBuildAction buildAction =
        CodeBuildAction.Builder.create()
            .actionName("CodeBuild")
            .project(buildProject)
            .input(sourceArtifact)
            .outputs(List.of(buildArtifact))
            .build();

    Pipeline pipeline =
        Pipeline.Builder.create(this, "QuizonicPipeline")
            .pipelineName("QuizonicPipeline")
            .artifactBucket(artifactBucket)
            .stages(
                List.of(
                    StageProps.builder().stageName("Source").actions(List.of(sourceAction)).build(),
                    StageProps.builder().stageName("Build").actions(List.of(buildAction)).build()))
            .build();
  }
}
