package com.sakai.cloud.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CloudApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudApp.class);

    public static void main(String[] args) {
        App app = new App();

        String defaultAccount = System.getenv("CDK_DEFAULT_ACCOUNT");
        String defaultRegion = System.getenv("CDK_DEFAULT_REGION");
        LOGGER.info("CDK_DEFAULT_ACCOUNT: {}", defaultAccount);
        LOGGER.info("CDK_DEFAULT_REGION: {}", defaultRegion);

        // Stage Configurator
        String stageName = (String) app.getNode().tryGetContext("stage");
        if (stageName == null || stageName.isBlank()) stageName = System.getenv("STAGE_NAME");
        if (stageName == null || stageName.isBlank()) stageName = "local-env";

        LOGGER.info("STAGE_NAME={}", stageName);

        StageConfigurator stageConfig;
        try {
            stageConfig = StageConfigurator.fromStage(stageName);
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            String branch = (String) app.getNode().tryGetContext("branch");
            stageConfig = StageConfigurator.fromLocal(branch);
        }

        Environment env = Environment.builder()
                .account(defaultAccount)
                .region(defaultRegion)
                .build();

        // Lambda Pipeline
        StackProps backendServiceStackProps = StackProps.builder()
                .description("SAKAI Service. Pipeline fur Lambda Deploy — Inventory Management System.")
                .env(env)
                .build();

        final LambdaBuildPipelineStack lambdaDeployPipelineStack = new LambdaBuildPipelineStack(app, "SakaiLambdaDeployPipelineStackId", backendServiceStackProps, stageConfig);
        lambdaDeployPipelineStack.initializeStack();
        app.synth();
    }
}
