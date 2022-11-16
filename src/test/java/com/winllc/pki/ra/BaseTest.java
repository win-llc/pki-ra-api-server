package com.winllc.pki.ra;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.winllc.pki.ra.util.EmailUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.keycloak.admin.client.Keycloak;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles({"test"})
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public abstract class BaseTest {

    private static final String postgresContainerImage = "bitnami/postgresql:11.14.0";
    private static final String elasticSearchContainerImage = "bitnami/elasticsearch:7.17.1";


    @MockBean
    private EmailUtil emailUtil;
    @MockBean
    private Keycloak keycloak;

    public static GenericContainer postgreSQLContainer = new GenericContainer<>(DockerImageName.parse(postgresContainerImage)
                .asCompatibleSubstituteFor("postgres"))
                .waitingFor(new LogMessageWaitStrategy()
                        .withRegEx(".*listening on IPv4 address.*")
                        //.withTimes(2)
                        .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS)))
                .withExposedPorts(5432)
                //.withCommand("postgres", "-c", "fsync=off")
                .withEnv("POSTGRESQL_DATABASE", "apiserver")
                .withEnv("POSTGRESQL_USERNAME", "sa")
                .withEnv("POSTGRESQL_PASSWORD", "sa")
                .withEnv("POSTGRESQL_POSTGRES_PASSWORD", "sa")
                /*
                .withEnv("REPMGR_PASSWORD", "repmgrpassword")
                .withEnv("REPMGR_PRIMARY_HOST", "pg-0")
                .withEnv("REPMGR_NODE_NETWORK_NAME", "pg-0")
                .withEnv("REPMGR_NODE_NAME", "pg-0")
                .withEnv("REPMGR_PARTNER_NODES", "pg-0")
                 */
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(5432), new ExposedPort(5432)))
                ));

    public static GenericContainer elasticsearchContainer = new GenericContainer<>(elasticSearchContainerImage)
            .withEnv("discovery.type", "single-node")
            .withNetworkAliases("elasticsearch-" + Base58.randomString(6))
            .withExposedPorts(9200)
            //.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
            //        new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(9200), new ExposedPort(9200)))
            //))
            .waitingFor(Wait.forLogMessage(".*(\"message\":\\s?\"started[\\s?|\"].*|] started\n$)", 1))
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(9200), new ExposedPort(9200)))
            ));

    @BeforeAll
    static void beforeAll(){
        postgreSQLContainer.start();
        elasticsearchContainer.start();
    }


}
