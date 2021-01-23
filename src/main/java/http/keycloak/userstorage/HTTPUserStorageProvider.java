package http.keycloak.userstorage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

/**
 * Custom HttpUserStorageProvider. Makes possible to store users in externalized user management
 * service.
 */
public class HTTPUserStorageProvider
    implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        UserQueryProvider {

  private static final Logger logger = Logger.getLogger(HTTPUserStorageProvider.class);

  private final HTTPConnector httpConnector;

  private final KeycloakSession session;

  private final ComponentModel model;

  private final FreshlyCreatedUsers freshlyCreatedUsers;

  HTTPUserStorageProvider(HTTPConfig cfg, KeycloakSession session, ComponentModel model) {
    this.session = session;
    // for caching users
    this.freshlyCreatedUsers = new FreshlyCreatedUsers(session);
    this.model = model;
    this.httpConnector = new HTTPConnector(cfg);
  }

  // UserLookupProvider methods

  /** {@inheritDoc} */
  @Override
  public HTTPUserModelDelegate getUserByUsername(String username, RealmModel realm) {
    logger.infof("getUserByUsername(s:'%s')", username);
    Supplier<HTTPUserModelDelegate> remoteCall =
        () ->
            httpConnector
                .getUserByUsername(realm.getId(), username)
                .map(
                    user ->
                        HTTPUserModelDelegate.createForExistingUser(
                            session, realm, model, user, httpConnector))
                .orElse(null);
    return freshlyCreatedUsers.getFreshlyCreatedUserByUsername(username).orElseGet(remoteCall);
  }

  /** {@inheritDoc} */
  @Override
  public HTTPUserModelDelegate getUserById(String id, RealmModel realm) {
    logger.infof("getUserById(s:'%s')", StorageId.externalId(id));
    Supplier<HTTPUserModelDelegate> remoteCall =
        () ->
            httpConnector
                .getUserByExternalId(realm.getId(), StorageId.externalId(id))
                .map(
                    user ->
                        HTTPUserModelDelegate.createForExistingUser(
                            session, realm, model, user, httpConnector))
                .orElseThrow(
                    () ->
                        new RuntimeException(
                            "User is not found by external id = " + StorageId.externalId(id)));

    return freshlyCreatedUsers.getFreshlyCreatedUserById(id).orElseGet(remoteCall);
  }

  /** {@inheritDoc} */
  @Override
  public HTTPUserModelDelegate getUserByEmail(String email, RealmModel realm) {
    logger.infof("getUserByEmail(s:'%s')", email);
    Supplier<HTTPUserModelDelegate> remoteCall =
        () ->
            httpConnector
                .getUserByEmail(realm.getId(), email)
                .map(
                    user ->
                        HTTPUserModelDelegate.createForExistingUser(
                            session, realm, model, user, httpConnector))
                .orElse(null);
    return freshlyCreatedUsers.getFreshlyCreatedUserByEmail(email).orElseGet(remoteCall);
  }

  // UserQueryProvider methods

  /** {@inheritDoc} */
  @Override
  public int getUsersCount(RealmModel realm) {
    logger.info("getUsersCount()");
    return httpConnector
        .getUsersCount(realm.getId())
        .orElseThrow(() -> new RuntimeException("No users count could be retrieved"));
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> getUsers(RealmModel realm) {
    logger.info("getUsers()");
    return httpConnector.getUsers(realm.getId(), 0, Integer.MAX_VALUE).stream()
        .map(
            user ->
                HTTPUserModelDelegate.createForExistingUser(
                    session, realm, model, user, httpConnector))
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> getUsers(RealmModel realm, int offset, int limit) {
    logger.infof("getUsers(%d,%d)", offset, limit);
    return httpConnector.getUsers(realm.getId(), offset, limit).stream()
        .map(
            user ->
                HTTPUserModelDelegate.createForExistingUser(
                    session, realm, model, user, httpConnector))
        .collect(Collectors.toList());
  }

  // UserQueryProvider method implementations

  /** {@inheritDoc} */
  @Override
  public List<UserModel> searchForUser(String search, RealmModel realm) {
    logger.infof("searchForUser(s:'%s')", search);
    return searchForUser(search, realm, 0, Integer.MAX_VALUE);
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> searchForUser(String search, RealmModel realm, int offset, int limit) {
    logger.infof("searchForUser(s:'%s',%d,%d)", search, offset, limit);
    return httpConnector.searchForUser(realm.getId(), search, offset, limit).stream()
        .map(
            user ->
                HTTPUserModelDelegate.createForExistingUser(
                    session, realm, model, user, httpConnector))
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
    logger.infof("searchForUser(p:'%s')", params);
    return searchForUser(params, realm, 0, Integer.MAX_VALUE);
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> searchForUser(
      Map<String, String> params, RealmModel realm, int offset, int limit) {
    logger.infof("searchForUser(p:'%s',%d,%d)", params, offset, +limit);
    return httpConnector.searchForUserByParams(realm.getId(), params, offset, limit).stream()
        .map(
            user ->
                HTTPUserModelDelegate.createForExistingUser(
                    session, realm, model, user, httpConnector))
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> getGroupMembers(
      RealmModel realm, GroupModel group, int offset, int limit) {
    logger.infof("getGroupMembers(g:'%s',%d,%d)", group, offset, limit);
    final Map<String, String> singleParam = Collections.singletonMap("group", group.getName());
    return httpConnector.searchForUserByParams(realm.getId(), singleParam, offset, limit).stream()
        .map(
            user ->
                HTTPUserModelDelegate.createForExistingUser(
                    session, realm, model, user, httpConnector))
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
    logger.info("getGroupMembers()");
    final Map<String, String> singleParam = Collections.singletonMap("group", group.getName());
    return httpConnector.searchForUserByParams(realm.getId(), singleParam, 0, Integer.MAX_VALUE)
        .stream()
        .map(
            user ->
                HTTPUserModelDelegate.createForExistingUser(
                    session, realm, model, user, httpConnector))
        .collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<UserModel> searchForUserByUserAttribute(
      String attrName, String attrValue, RealmModel realm) {
    logger.infof("searchForUserByUserAttribute(%s,%s)", attrName, attrValue);
    final Map<String, String> singleParam = Collections.singletonMap(attrName, attrValue);
    return httpConnector.searchForUserByParams(realm.getId(), singleParam, 0, Integer.MAX_VALUE)
        .stream()
        .map(
            user ->
                HTTPUserModelDelegate.createForExistingUser(
                    session, realm, model, user, httpConnector))
        .collect(Collectors.toList());
  }

  // CredentialInputValidator methods

  /** {@inheritDoc} */
  @Override
  public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
    if (!supportsCredentialType(credentialType)) {
      return false;
    }
    return httpConnector
        .isConfiguredPasswordForExternalId(realm.getId(), StorageId.externalId(user.getId()))
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Couldn't check is password set for a user with id = " + user.getId()));
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsCredentialType(String credentialType) {
    return PasswordCredentialModel.TYPE.equals(credentialType);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
    logger.infof("isValid(username=%s)", user.getUsername());

    if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
      logger.info("credentialtype unknown or not correct model");
      return false;
    }
    UserCredentialModel cred = (UserCredentialModel) input;
    String rawPassword = cred.getChallengeResponse();
    Optional<HTTPUserModelDelegate> freshlyCreatedUserById =
        freshlyCreatedUsers.getFreshlyCreatedUserById(user.getId());
    if (freshlyCreatedUserById.isPresent()) {
      logger.info("user was freshly installed");
      throw new RuntimeException();
    }
    boolean result =
        httpConnector.verifyPassword(
            realm.getId(), StorageId.externalId(user.getId()), rawPassword);
    logger.infof("password valid: %b", result);

    return result;
  }

  // CredentialInputUpdater methods

  /** {@inheritDoc} */
  @Override
  public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
    logger.infof("updateCredential(%s,%s)", user, input);
    if (!(input instanceof UserCredentialModel)) {
      return false;
    }
    if (!PasswordCredentialModel.TYPE.equals(input.getType())) {
      return false;
    }
    if (!(user instanceof HTTPUserModelDelegate)) {
      throw new RuntimeException();
    }
    UserCredentialModel cred = (UserCredentialModel) input;
    HTTPUserModelDelegate delegate = (HTTPUserModelDelegate) user;
    delegate.getDelegatedUserModel().setPassword(cred.getChallengeResponse());
    delegate.ensureTransactionEnlisted();
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    // is not supported
    throw new RuntimeException();
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
    // is not supported
    return Collections.emptySet();
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    // noop
  }
}
