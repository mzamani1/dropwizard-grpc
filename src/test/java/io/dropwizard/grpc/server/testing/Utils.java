package io.dropwizard.grpc.server.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.SSLException;

import io.dropwizard.cli.CheckCommand;
import io.dropwizard.cli.Command;
import io.dropwizard.grpc.server.testing.junit.TestApplication;
import io.dropwizard.grpc.server.testing.junit.TestConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.DropwizardTestSupport;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Utility class with convenience methods for testing.
 *
 * @author gfecher
 */
public final class Utils {
    /**
     * Creates a <code>ManagedChannel</code> connecting to the <b>plaintext</b> gRPC server in
     * <code>TestApplication</code> in <code>testSupport</code>.
     *
     * @param testSupport the already initialised (started) <code>DropwizardTestSupport</code> instance
     * @return the channel connecting to the server (to be used in a client)
     */
    public static ManagedChannel createPlaintextChannel(final DropwizardTestSupport<TestConfiguration> testSupport) {
        final TestApplication application = testSupport.getApplication();
        return ManagedChannelBuilder.forAddress("localhost", application.getServer().getPort()).usePlaintext(true)
            .build();
    }

    /**
     * Creates a <code>ManagedChannel</code> connecting to an <b>encrypted</b> gRPC server in
     * <code>TestApplication</code> in <code>testSupport</code>. The certificate is taken from the
     * <code>GrpcServerFactory</code> in the configuration.
     *
     * @param testSupport the already initialised (started) <code>DropwizardTestSupport</code> instance
     * @return the channel connecting to the server (to be used in a client)
     */
    public static ManagedChannel createClientChannelForEncryptedServer(
            final DropwizardTestSupport<TestConfiguration> testSupport) throws SSLException {
        final SslContext sslContext = GrpcSslContexts.forClient()
            .trustManager(testSupport.getConfiguration().getGrpcServerFactory().getCertChainFile().toFile()).build();
        final TestApplication application = testSupport.getApplication();
        return NettyChannelBuilder.forAddress("localhost", application.getServer().getPort()).sslContext(sslContext)
            .overrideAuthority("grpc-dropwizard.example.com").build();
    }

    /**
     * Shuts down the given channel if not <code>null</code>, waiting for up to 1 second.
     *
     * @param channel the channel to shut down
     */
    public static void shutdownChannel(final ManagedChannel channel) {
        if (channel != null) {
            channel.shutdown();
            try {
                channel.awaitTermination(1, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                // silently swallow exception
            }
        }
    }

    /**
     * Converts the given resource path string to a valid URI string.
     *
     * @param resourceClassPathLocation the path to the resource that is on the classpath
     * @return a valid <code>file://</code> URI string to the given resource path
     */
    public static String getURIForResource(final String resourceClassPathLocation) {
        return "file:///" + resourceFilePath(resourceClassPathLocation).replaceAll("\\\\", "/");
    }

    /**
     * Runs the check command for the {@link TestApplication} with the given yaml configuration.
     *
     * @param yamlConfig a <code>String</code> containing the yaml configuration
     * @return the TestApplication if successful
     * @throws Exception if the command failed
     */
    @SuppressWarnings("unchecked")
    public static TestApplication runTestWithConfig(final String yamlConfig) throws Exception {
        return runTestWithConfig(yamlConfig, CheckCommand::new);
    }

    /**
     * Runs a command for the {@link TestApplication} with the given yaml configuration.
     *
     * @param yamlConfig a <code>String</code> containing the yaml configuration
     * @return the TestApplication if successful
     * @throws Exception if the command failed
     */
    public static TestApplication runTestWithConfig(final String yamlConfig,
            final Function<TestApplication, Command> commandInstantiator) throws Exception {
        final TestApplication application = new TestApplication();
        final Bootstrap<TestConfiguration> bootstrap = new Bootstrap<>(application);
        bootstrap.setConfigurationSourceProvider(new StringConfigurationSourceProvider());
        final Command command = commandInstantiator.apply(application);
        command.run(bootstrap, new Namespace(Collections.singletonMap("file", yamlConfig)));
        return application;
    }

    private Utils() {
        // private constructor to prevent instantiation
    }
}
