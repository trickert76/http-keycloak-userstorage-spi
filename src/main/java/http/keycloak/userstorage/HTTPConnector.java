package http.keycloak.userstorage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.keycloak.util.BasicAuthHelper;

/** 
 * Connector that sends http requests to externalized user management service 
 * 
 * The Connector wants the following URLs for the backend
 * 
 * - GET /user - returns a list of users (offset, limit, search and group)
 * - GET /user/{username} - returns a user with the given username
 * - GET /user/mail/{mail} - returns a user with the given mail address
 * - POST /user/validate/{username} - with password as body returns 200 OK, if password is valid
 * 
 * All writing or deleting operations are yet not supported.
 */
public class HTTPConnector {

  private static final Logger logger = Logger.getLogger(HTTPConnector.class);

  private static final ObjectMapper OBJECT_MAPPER;
  private static final ResteasyJackson2Provider JACKSON_PROVIDER;

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JACKSON_PROVIDER = new ResteasyJackson2Provider() {};
    JACKSON_PROVIDER.setMapper(OBJECT_MAPPER);
  }

  private final String auth;

  private final WebTarget usersTarget;
  private final WebTarget userByNameTarget;
  private final WebTarget userByMailTarget;
  private final WebTarget userValidateTarget;

  public HTTPConnector(HTTPConfig cfg) {
    auth = BasicAuthHelper.createHeader(cfg.getUsername(), cfg.getPassword());

    usersTarget =
        ResteasyClientBuilder.newBuilder()
            .register(JACKSON_PROVIDER, 100)
            .build()
            .target(cfg.getUrl())
            .path("/user");
    userByNameTarget = usersTarget.path("{username}");
    userByMailTarget = usersTarget.path("mail/{mail}");
    userValidateTarget = usersTarget.path("validate/{username}");
  }

  /**
   * Helper method to build endpoint url for users resource
   *
   * @param realmId realm in which users are stored
   * @return request builder
   */
  private WebTarget usersEndpoint(Optional<Integer> offset, Optional<Integer> limit) {
    if (offset.isPresent() && limit.isPresent())
      return usersTarget.queryParam("offset", offset.get()).queryParam("limit", limit.get());
    return usersTarget;
  }

  /**
   * Helper method to build endpoint url for user resource
   *
   * @param userId userId to search for
   * @return request builder
   */
  private WebTarget userByIdEndpoint(String userId) {
    return userByNameTarget.resolveTemplate("username", userId);
  }

  /**
   * Helper method to build endpoint url for user resource
   *
   * @param username username of user to search
   * @return request builder
   */
  private WebTarget userByNameEndpoint(String username) {
    return userByNameTarget.resolveTemplate("username", username);
  }

  /**
   * Helper method to build endpoint url for user resource
   *
   * @param mail Mail address of user
   * @return request builder
   */
  private WebTarget userByMailEndpoint(String mail) {
    return userByMailTarget.resolveTemplate("mail", mail);
  }

  /**
   * Helper method to build endpoint url for user resource
   *
   * @param username username of user
   * @return request builder
   */
  private WebTarget validateUserPassword(String username) {
    return userValidateTarget.resolveTemplate("username", username);
  }

  public Optional<HTTPUserModel> getUserByExternalId(String realmId, String externalId) {
    logger.infof("getUserByExternalId(s:%s, s:%s)", realmId, externalId);
    Response resolvedUser =
        userByIdEndpoint(externalId)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, auth)
            .get();
    if (isSuccessful(resolvedUser)) {
      final Optional<HTTPUserModel> result = Optional.of(resolvedUser.readEntity(HTTPUserModel.class));
      logger.infof("getUserByExternalId(%s, %s) = %s", realmId, externalId, result);
      return result;
    }
    logger.infof("getUserByExternalId(%s, %s) = empty", realmId, externalId);
    return Optional.empty();
  }

  public Optional<HTTPUserModel> getUserByUsername(String realmId, String username) {
    logger.infof("getUserByUsername(s:%s, s:%s)", realmId, username);

    Response resolvedUser =
        userByNameEndpoint(username)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, auth)
            .get();

    logger.infof("uri: %s", userByNameEndpoint(username).getUri());

    if (isSuccessful(resolvedUser)) {
      final Optional<HTTPUserModel> result =
          Optional.ofNullable(resolvedUser.readEntity(HTTPUserModel.class));
      logger.infof("getUserByUsername(%s, %s) = %s", realmId, username, result);
      return result;
    }
    logger.infof("getUserByUsername(%s, %s) = empty", realmId, username);
    return Optional.empty();
  }

  private boolean isSuccessful(Response resolvedUser) {
    return resolvedUser.getStatusInfo().toEnum() == Response.Status.OK && resolvedUser.hasEntity();
  }

  public Optional<HTTPUserModel> getUserByEmail(String realmId, String email) {
    logger.infof("getUserByEmail(%s, %s)", realmId, email);
    Response resolvedUser =
        userByMailEndpoint(email)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, auth)
            .get();

    logger.infof("uri: %s", userByMailEndpoint(email).getUri());

    if (isSuccessful(resolvedUser)) {
      logger.info("success");
      return Optional.ofNullable(resolvedUser.readEntity(HTTPUserModel.class));
    }
    return Optional.empty();
  }

  public Optional<Integer> getUsersCount(String realmId) {
    // dummy
    if (realmId != null) return Optional.of(getUsers(realmId, 0, 1).size());

    logger.infof("getUsersCount(%s)", realmId);
    final Response response =
        usersEndpoint(Optional.empty(), Optional.empty())
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, auth)
            .get();
    if (isSuccessful(response)) {
      List<HTTPUserModel> users = response.readEntity(new GenericType<List<HTTPUserModel>>() {});
      return Optional.of(users.isEmpty() ? 0 : users.size());
    }
    return Optional.empty();
  }

  /**
   * Helper method to build search methods on users resource
   *
   * @param realmId realm within which users exist
   * @param offset common parameter for each search method for offset-based pagination
   * @param limit common parameter for each search method for offset-based pagination
   * @param appendQueryParameters function that adds additional queryParameters, used by search
   *     methods
   * @return list of {@linkplain HTTPUserModel} that satisfy criteria
   */
  private List<HTTPUserModel> getUsersTemplate(
      String realmId, int offset, int limit, Function<WebTarget, WebTarget> appendQueryParameters) {
    final WebTarget usersEndpointWithAdditionalQueryParameters =
        appendQueryParameters.apply(usersEndpoint(Optional.of(offset), Optional.of(limit)));

    logger.infof("uri: %s", usersEndpointWithAdditionalQueryParameters.getUri());
    final Response response =
        usersEndpointWithAdditionalQueryParameters
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, auth)
            .get();
    if (isSuccessful(response)) {
      return response.readEntity(new GenericType<List<HTTPUserModel>>() {});
    } else if (response.getStatusInfo().toEnum() == Response.Status.BAD_REQUEST) {
      throw new RuntimeException(response.readEntity(String.class));
    }
    logger.errorf(
        "getUsersTemplate(%s, %s, %s, %s}) = %s",
        realmId, offset, limit, "appendQueryParameters", response);
    return Collections.emptyList();
  }

  public List<HTTPUserModel> getUsers(String realmId, int offset, int limit) {
    logger.infof("getUsers(%s, %s, %s)", realmId, offset, limit);
    final List<HTTPUserModel> result = getUsersTemplate(realmId, offset, limit, Function.identity());
    logListOfUserModel(result);
    return result;
  }

  private void logListOfUserModel(List<HTTPUserModel> result) {
    logger.infof("list of user models: %s", result);
  }

  public List<HTTPUserModel> searchForUser(String realmId, String search, int offset, int limit) {
    logger.infof("searchForUser(%s, %s, %d, %d)", realmId, search, offset, limit);
    final List<HTTPUserModel> result =
        getUsersTemplate(realmId, offset, limit, target -> target.queryParam("search", search));
    logListOfUserModel(result);
    return result;
  }

  /**
   * @param realmId realm within which user exists
   * @param params the filter to search for
   * @param offset
   * @param limit
   * @return
   */
  public List<HTTPUserModel> searchForUserByParams(
      String realmId, Map<String, String> params, int offset, int limit) {
    logger.infof("searchForUserByParams(p'%s', %d, %d)", params, offset, limit);
    final Function<WebTarget, WebTarget> appendQueryParametersToTarget =
        target -> {
          for (Map.Entry<String, String> entry : params.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
          }
          return target;
        };
    final List<HTTPUserModel> result =
        getUsersTemplate(realmId, offset, limit, appendQueryParametersToTarget);
    logListOfUserModel(result);
    return result;
  }

  /**
   * @param realmId realm within which user exists
   * @param externalId
   * @return
   */
  public Optional<Boolean> isConfiguredPasswordForExternalId(String realmId, String externalId) {
    logger.infof("isConfiguredPasswordForExternalId(%s, %s)", new Object[] {realmId, externalId});
    return Optional.of(true);
  }

  /**
   * Creates a User in the backend
   *
   * @param realmId realm within which user exists
   * @param user the new user
   * @param isManualSetUp
   * @return
   */
  public HTTPUserModel createUser(String realmId, HTTPUserModel user, boolean isManualSetUp) {
    logger.infof("createUser(%s, %s)", realmId, user);
    throw new RuntimeException("Creating user in http storage has failed");
  }

  /**
   * Verify non-null password for a given user
   *
   * @param realmId realm within which user exists
   * @param userId user service id
   * @param password password from UI that needs to be verified. If it is null then this method
   *     returns false
   * @return true - if is valid, false otherwise
   */
  public boolean verifyPassword(String realmId, String userId, String password) {
    if (password == null) {
      logger.infof("verifyPassword(%s, %s, null) = false", realmId, userId);
      return false;
    }

    try {
      logger.infof("uri: %s", validateUserPassword(userId).getUri());
      final Response response =
          validateUserPassword(userId)
              .request(MediaType.APPLICATION_JSON_TYPE)
              .header(HttpHeaders.AUTHORIZATION, auth)
              .post(Entity.entity(password, MediaType.APPLICATION_JSON));
      logger.infof("response: %d", response.getStatus());
      return response.getStatusInfo().toEnum() == Response.Status.OK;
    } catch (Exception e) {
      logger.error("could not validate password", e);
    }
    return false;
  }

  /**
   * Removes a user in the backend.
   *
   * @param realmId realm within which user exists
   * @param externalId UserId to remove
   * @return true, if user was removed
   */
  public boolean removeUserByExternalId(String realmId, String externalId) {
    return false;
  }

  /**
   * Updates a usermodel in the backend.
   *
   * @param realmId realm within which user exists
   * @param updatedUserModel updated user model
   * @param isManualSetUp
   */
  public void updateUser(String realmId, HTTPUserModel updatedUserModel, boolean isManualSetUp) {
    logger.infof("updateUser(%s, %s)", realmId, updatedUserModel);
  }
}
