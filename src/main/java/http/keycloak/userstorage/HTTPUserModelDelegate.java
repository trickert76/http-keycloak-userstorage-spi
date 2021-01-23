package http.keycloak.userstorage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AbstractKeycloakTransaction.TransactionState;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

/**
 * Delegation pattern. Used to delegate managing roles and groups to Keycloak federated storage and
 * entity data such as username, email, etc. to http storage
 */
public class HTTPUserModelDelegate extends AbstractUserAdapterFederatedStorage {

  private static final Logger logger = Logger.getLogger(HTTPUserModelDelegate.class);

  /** Is this delegate represents persisted entity in http storage? */
  private boolean isPersistedInHttpStorage;

  /** User model that keeps the data */
  private final HTTPUserModel httpUserModel;

  /** Http transaction that updates http storage at one moment */
  private final HTTPTransaction httpTransaction;

  public static HTTPUserModelDelegate createForExistingUser(
      KeycloakSession session,
      RealmModel realm,
      ComponentModel storageProviderModel,
      HTTPUserModel userModel,
      HTTPConnector httpConnector) {
    HTTPUserModelDelegate delegate =
        new HTTPUserModelDelegate(
            session, realm, storageProviderModel, userModel, httpConnector);
    delegate.isPersistedInHttpStorage = true;
    return delegate;
  }

  /**
   * @param session
   * @param realm
   * @param storageProviderModel
   * @param httpUserModel
   * @param httpConnector
   */
  private HTTPUserModelDelegate(
      KeycloakSession session,
      RealmModel realm,
      ComponentModel storageProviderModel,
      HTTPUserModel httpUserModel,
      HTTPConnector httpConnector) {
    super(session, realm, storageProviderModel);
    this.httpUserModel = httpUserModel;
    httpUserModel.setRealm(realm);
    httpTransaction = new HTTPTransaction(httpConnector, this);
  }

  public void ensureTransactionEnlisted() {
    if (TransactionState.NOT_STARTED.equals(httpTransaction.getState())
        && !httpTransaction.isEnlisted()) {
      session.getTransactionManager().enlistAfterCompletion(httpTransaction);
      httpTransaction.setEnlisted(true);
    }
  }

  public boolean isAdminTool() {
    return session.getContext().getUri().getDelegate().getPath().startsWith("/admin/realms/");
  }

  public boolean isNotPersistedInHttpStorage() {
    return !isPersistedInHttpStorage;
  }

  public void setPersistedInHttpStorage(boolean persistedInHttpStorage) {
    isPersistedInHttpStorage = persistedInHttpStorage;
  }

  public HTTPUserModel getDelegatedUserModel() {
    return httpUserModel;
  }

  public String getRealmId() {
    return realm.getId();
  }

  @Override
  public String getUsername() {
    return httpUserModel.getUsername();
  }

  @Override
  public void setUsername(String username) {
    logger.infof("setUsername(%s)", username);
    if (Objects.equals(httpUserModel.getUsername(), username)) {
      return;
    }
    httpUserModel.setUsername(username);
    ensureTransactionEnlisted();
  }

  @Override
  public String getEmail() {
    return httpUserModel.getEmail();
  }

  @Override
  public void setEmail(String email) {
    logger.infof("setEmail(%s)", email);
    if (Objects.equals(httpUserModel.getEmail(), email)) {
      return;
    }
    httpUserModel.setEmail(email);
    ensureTransactionEnlisted();
  }

  @Override
  public String getFirstName() {
    return httpUserModel.getFirstName();
  }

  @Override
  public void setFirstName(String firstName) {
    logger.infof("setFirstName(%s)", firstName);
    if (Objects.equals(httpUserModel.getFirstName(), firstName)) {
      return;
    }
    httpUserModel.setFirstName(firstName);
    ensureTransactionEnlisted();
  }

  @Override
  public String getLastName() {
    return httpUserModel.getLastName();
  }

  @Override
  public void setLastName(String lastName) {
    logger.infof("setLastName(%s)", lastName);
    if (Objects.equals(httpUserModel.getLastName(), lastName)) {
      return;
    }
    httpUserModel.setLastName(lastName);
    ensureTransactionEnlisted();
  }

  @Override
  public boolean isEmailVerified() {
    return httpUserModel.isEmailVerified();
  }

  @Override
  public void setEmailVerified(boolean verified) {
    logger.infof("setEmailVerified(%s)", verified);
    if (httpUserModel.isEmailVerified() == verified) {
      return;
    }
    httpUserModel.setEmailVerified(verified);
    ensureTransactionEnlisted();
  }

  @Override
  public boolean isEnabled() {
    return httpUserModel.isEnabled();
  }

  @Override
  public void setEnabled(boolean enabled) {
    logger.infof("setEnabled(%s)", enabled);
    if (httpUserModel.isEnabled() == enabled) {
      return;
    }
    httpUserModel.setEnabled(enabled);
    ensureTransactionEnlisted();
  }

  @Override
  public Long getCreatedTimestamp() {
    return httpUserModel.getCreatedTimestamp();
  }

  @Override
  public void setCreatedTimestamp(Long timestamp) {
    logger.infof("setCreatedTimestamp(%s)", timestamp);
    if (Objects.equals(httpUserModel.getCreatedTimestamp(), timestamp)) {
      return;
    }
    httpUserModel.setCreatedTimestamp(timestamp);
    ensureTransactionEnlisted();
  }

  @Override
  public void setSingleAttribute(String name, String value) {
    logger.infof("setSingleAttribute(%s, %s)", name, value);
    if (Objects.equals(httpUserModel.getFirstAttribute(name), value)) {
      return;
    }
    httpUserModel.setSingleAttribute(name, value);
    ensureTransactionEnlisted();
  }

  @Override
  public void removeAttribute(String name) {
    logger.infof("removeAttribute(%s)", name);
    httpUserModel.removeAttribute(name);
    ensureTransactionEnlisted();
  }

  @Override
  public void setAttribute(String name, List<String> values) {
    logger.infof("setAttribute(%s, %s)", name, values);
    if (httpUserModel.getAttribute(name).equals(values)) {
      return;
    }
    httpUserModel.setAttribute(name, values);
    ensureTransactionEnlisted();
  }

  @Override
  public String getFirstAttribute(String name) {
    logger.infof("getFirstAttribute(%s)", name);
    return httpUserModel.getFirstAttribute(name);
  }

  @Override
  public Map<String, List<String>> getAttributes() {
    logger.infof("getAttributes()");
    return httpUserModel.getAttributes();
  }

  @Override
  public List<String> getAttribute(String name) {
    logger.infof("getAttribute(%s)", name);
    return httpUserModel.getAttribute(name);
  }

  @Override
  public String getId() {
    if (storageId == null) {
      storageId = new StorageId(storageProviderModel.getId(), httpUserModel.getId());
    }
    return storageId.getId();
  }

  @Override
  public Set<GroupModel> getGroupsInternal() {
    logger.info("getGroupsInternal()");

    return httpUserModel.getGroupsAndRoles().keySet().stream()
        .map(group -> new HTTPGroupModel(group, httpUserModel.getGroupsAndRoles().get(group), realm))
        .collect(Collectors.toSet());
  }

  @Override
  protected Set<RoleModel> getRoleMappingsInternal() {
    logger.info("getRoleMappingsInternal()");
    return httpUserModel.getGroupsAndRoles().values().stream()
        .flatMap(List::stream)
        .map(role -> new HTTPRoleModel(role, realm))
        .distinct()
        .collect(Collectors.toSet());
  }
}
