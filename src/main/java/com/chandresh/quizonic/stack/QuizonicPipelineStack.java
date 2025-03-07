package com.chandresh.quizonic.stack;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubTrigger;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
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
    GetParameterRequest parameterRequest =
        GetParameterRequest.builder()
            .name("/quizonic/github-token") // Change this to your actual SSM parameter name
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

    Repository quizonicRepository =
        Repository.Builder.create(this, "QuizonicEcrRepo")
            .repositoryName("quizonic-service")
            .removalPolicy(RemovalPolicy.RETAIN) // Retain images even if stack is deleted
            .build();

    String ecrRepositoryUri = quizonicRepository.getRepositoryUri();

    Role quizonicBuildProjectRole =
        Role.Builder.create(this, "QuizonicBuildProjectRole")
            .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
            .build();
    quizonicBuildProjectRole.addToPolicy(
        PolicyStatement.Builder.create()
            .actions(
                List.of(
                    "ecr:GetAuthorizationToken",
                    "ecr:BatchGetImage",
                    "ecr:BatchCheckLayerAvailability",
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:InitiateLayerUpload",
                    "ecr:UploadLayerPart",
                    "ecr:CompleteLayerUpload",
                    "ecr:PutImage"))
            .resources(List.of("*"))
            .build());

    PipelineProject buildProject =
        PipelineProject.Builder.create(this, "QuizonicBuildProject")
            .projectName("QuizonicBuildProject")
            .role(quizonicBuildProjectRole)
            .environment(
                BuildEnvironment.builder()
                    .buildImage(
                        LinuxBuildImage.fromCodeBuildImageId(
                            "aws/codebuild/amazonlinux2-x86_64-standard:5.0"))
                    .computeType(ComputeType.SMALL)
                    .build())
            .buildSpec(
                BuildSpec.fromObject(
                    Map.of(
                        "version", "0.2",
                        "phases",
                            Map.of(
                                "pre_build",
                                Map.of(
                                    "commands",
                                    List.of(
                                        "echo Logging in to Amazon ECR...",
                                        "aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin "
                                            + ecrRepositoryUri,
                                        "IMAGE_TAG=$(echo $CODEBUILD_RESOLVED_SOURCE_VERSION | cut -c 1-7)",
                                        "export IMAGE_TAG",
                                        "echo IMAGE_TAG=$IMAGE_TAG")),
                                "install",
                                Map.of("commands", List.of("mvn install")),
                                "build",
                                Map.of(
                                    "commands",
                                    List.of(
                                        "mvn spotless:check",
                                        "mvn package",
                                        "echo Build completed on `date`",
                                        "echo Building Docker Image...",
                                        "docker build -t " + ecrRepositoryUri + ":$IMAGE_TAG .",
                                        "docker tag "
                                            + ecrRepositoryUri
                                            + ":$IMAGE_TAG "
                                            + ecrRepositoryUri
                                            + ":latest",
                                        "echo docker build completed on `date`")),
                                "post_build",
                                Map.of(
                                    "commands",
                                    List.of(
                                        "echo Now pushing Docker image...",
                                        "docker push " + ecrRepositoryUri + ":$IMAGE_TAG",
                                        "docker push " + ecrRepositoryUri + ":latest",
                                        "echo Writing image definition file...",
                                        "printf '[{\"name\":\"quizonic-service\",\"imageUri\":\"%s\"}]' "
                                            + ecrRepositoryUri
                                            + ":$IMAGE_TAG > imagedefinitions.json"))),
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
