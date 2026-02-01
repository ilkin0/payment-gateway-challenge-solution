package com.checkout.payment.gateway;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  private static final String IMPOSTERS_CONFIG;

  static {
    try {
      IMPOSTERS_CONFIG = Files.readString(Path.of("imposters/bank_simulator.ejs"));
    } catch (Exception e) {
      throw new RuntimeException("Failed to load imposters config", e);
    }
  }

  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
      DockerImageName.parse("postgres:18.1-alpine"))
      .withDatabaseName("payments_test")
      .withUsername("test")
      .withPassword("test")
      .withReuse(true);

  private static final GenericContainer<?> BANK_SIMULATOR = new GenericContainer<>(
      DockerImageName.parse("bbyars/mountebank:2.8.1"))
      .withExposedPorts(2525, 8080)
      .withCommand("--configfile", "/imposters/bank_simulator.ejs", "--allowInjection")
      .withCopyToContainer(Transferable.of(IMPOSTERS_CONFIG), "/imposters/bank_simulator.ejs")
      .waitingFor(Wait.forHttp("/").forPort(2525))
      .withReuse(true);

  static {
    POSTGRES.start();
    BANK_SIMULATOR.start();
  }

  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    return POSTGRES;
  }

  public static GenericContainer<?> getBankSimulatorContainer() {
    return BANK_SIMULATOR;
  }
}
