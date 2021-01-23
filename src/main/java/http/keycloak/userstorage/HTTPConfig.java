package http.keycloak.userstorage;

import org.keycloak.common.util.MultivaluedHashMap;

/**
 * A model for all MACHWeb specific configurations
 */
public class HTTPConfig {
  private final MultivaluedHashMap<String, String> config;

  public HTTPConfig(MultivaluedHashMap<String, String> config) {
    this.config = config;
  }

  public String getUrl() {
    return config.getFirst(HTTPConstants.CONFIG_URL);
  }

  public String getUsername() {
    return config.getFirst(HTTPConstants.CONFIG_USERNAME);
  }

  public String getPassword() {
    return config.getFirst(HTTPConstants.CONFIG_PASSWORD);
  }

  public boolean isPagination() {
    // for later - can be configurable
    return false;
  }

  public int getBatchSizeForSync() {
    // for later - can be configurable, if isPagination is true
    return 100;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (!(obj instanceof HTTPConfig))
      return false;

    HTTPConfig that = (HTTPConfig) obj;

    if (!config.equals(that.config))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    return config.hashCode() * 13;
  }

  @Override
  public String toString() {
    MultivaluedHashMap<String, String> copy = new MultivaluedHashMap<String, String>(config);
    copy.remove(HTTPConstants.CONFIG_PASSWORD);
    return new StringBuilder(copy.toString()).toString();
  }
}
