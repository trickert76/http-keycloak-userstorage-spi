package http.keycloak.userstorage;

import java.net.URI;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

public class HTTPUserStorageProviderFactory
    implements UserStorageProviderFactory<HTTPUserStorageProvider> {
  private static final Logger logger = Logger.getLogger(HTTPUserStorageProviderFactory.class);

  private List<ProviderConfigProperty> configProperties = null;

  @Override
  public String getId() {
    return HTTPConstants.PROVIDER_NAME;
  }

  @Override
  public void init(Config.Scope config) {
    logger.info("Initializing HTTP UserStorage SPI");
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    if (configProperties == null) {
      configProperties = ProviderConfigurationBuilder.create().property().name(HTTPConstants.CONFIG_URL)
          .helpText(HTTPConstants.CONFIG_URL_HELP).label(HTTPConstants.CONFIG_URL_LABEL)
          .type(ProviderConfigProperty.STRING_TYPE).add().property().name(HTTPConstants.CONFIG_USERNAME)
          .helpText(HTTPConstants.CONFIG_USERNAME_HELP).label(HTTPConstants.CONFIG_USERNAME_LABEL)
          .type(ProviderConfigProperty.STRING_TYPE).add().property().name(HTTPConstants.CONFIG_PASSWORD)
          .helpText(HTTPConstants.CONFIG_PASSWORD_HELP).label(HTTPConstants.CONFIG_PASSWORD_LABEL)
          .type(ProviderConfigProperty.PASSWORD).secret(true).add().build();
    }
    return configProperties;
  }

  @Override
  public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
      throws ComponentValidationException {
    HTTPConfig cfg = new HTTPConfig(model.getConfig());

    try {
      URI.create(cfg.getUrl());
    } catch (NullPointerException npe) {
      throw new ComponentValidationException("HTTPErrorURLNotSet");
    } catch (IllegalArgumentException iae) {
      throw new ComponentValidationException("HTTPErrorURLNotCorrect");
    }

    if (cfg.getUsername() == null || cfg.getUsername().trim().length() == 0) {
      throw new ComponentValidationException("HTTPErrorUsernameNotSet");
    }
    if (cfg.getPassword() == null || cfg.getPassword().trim().length() == 0) {
      throw new ComponentValidationException("HTTPErrorPasswordNotSet");
    }
  }

  @Override
  public HTTPUserStorageProvider create(KeycloakSession session, ComponentModel model) {
    HTTPConfig cfg = new HTTPConfig(model.getConfig());
    return new HTTPUserStorageProvider(cfg, session, model);
  }
}