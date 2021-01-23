package http.keycloak.userstorage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

@JsonIgnoreProperties(value = {"groups", "realmRoleMappings", "roleMappings", "groupsCount"})
public class HTTPUserModel implements UserModel {
  private static final Logger logger = Logger.getLogger(HTTPUserModel.class);

  private String id;

  private String username;

  private String password;

  private Long createdTimestamp;

  private boolean enabled;

  private Map<String, List<String>> attributes = new HashMap<>();

  private Set<String> requiredActions;

  private String email;

  private String firstName;

  private String lastName;

  private boolean emailVerified;

  private Map<String, List<String>> groupsAndRoles = new HashMap<>();

  private RealmModel realm = null;

  @ConstructorProperties("id")
  public HTTPUserModel(String id) {
    this.id = id;
  }

  /** {@inheritDoc} */
  @Override
  public String getId() {
    return id;
  }

  /** {@inheritDoc} */
  @Override
  public String getUsername() {
    return username;
  }

  /** {@inheritDoc} */
  @Override
  public void setUsername(String username) {
    this.username = username;
  }

  /** {@inheritDoc} */
  public void setPassword(String password) {
    this.password = password;
  }
  
  public String getPassword() {
    return password;
  }

  /** {@inheritDoc} */
  @Override
  public Long getCreatedTimestamp() {
    return createdTimestamp;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /** {@inheritDoc} */
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, List<String>> getAttributes() {
    return attributes;
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getRequiredActions() {
    return requiredActions;
  }

  /** {@inheritDoc} */
  @Override
  public String getEmail() {
    return email;
  }

  /** {@inheritDoc} */
  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  /** {@inheritDoc} */
  @Override
  public String getFirstName() {
    return firstName;
  }

  /** {@inheritDoc} */
  @Override
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /** {@inheritDoc} */
  @Override
  public String getLastName() {
    return lastName;
  }

  /** {@inheritDoc} */
  @Override
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /** {@inheritDoc} */
  @Override
  public void setCreatedTimestamp(Long timestamp) {
    this.createdTimestamp = timestamp;
  }

  /** {@inheritDoc} */
  @Override
  public void setSingleAttribute(String name, String value) {
    attributes.put(name, Collections.singletonList(value));
  }

  /** {@inheritDoc} */
  @Override
  public void setAttribute(String name, List<String> values) {
    attributes.put(name, values);
  }

  /** {@inheritDoc} */
  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  /** {@inheritDoc} */
  @Override
  public String getFirstAttribute(String name) {
    return attributes.containsKey(name) ? attributes.get(name).get(0) : null;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getAttribute(String name) {
    return attributes.getOrDefault(name, Collections.emptyList());
  }

  /** {@inheritDoc} */
  @Override
  public void addRequiredAction(String action) {
    requiredActions.add(action);
  }

  /** {@inheritDoc} */
  @Override
  public void removeRequiredAction(String action) {
    requiredActions.remove(action);
  }

  /** {@inheritDoc} */
  @Override
  public void addRequiredAction(RequiredAction action) {
    requiredActions.add(action.toString());
  }

  /** {@inheritDoc} */
  @Override
  public void removeRequiredAction(RequiredAction action) {
    requiredActions.remove(action.toString());
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmailVerified() {
    return emailVerified;
  }

  /** {@inheritDoc} */
  @Override
  public void setEmailVerified(boolean verified) {
    emailVerified = verified;
  }

  /** {@inheritDoc} */
  @Override
  public Set<GroupModel> getGroups() {
    logger.infof("getGroups() with realm %s",realm);
    return groupsAndRoles.keySet().stream()
        .map(group -> new HTTPGroupModel(group, groupsAndRoles.get(group), realm))
        .collect(Collectors.toSet());
  }

  /** {@inheritDoc} */
  @Override
  public void joinGroup(GroupModel group) {
    // no op
  }

  /** {@inheritDoc} */
  @Override
  public void leaveGroup(GroupModel group) {
    // no op
  }

  /** {@inheritDoc} */
  @Override
  public boolean isMemberOf(GroupModel group) {
    return getGroups().contains(group);
  }

  /** {@inheritDoc} */
  @Override
  public String getFederationLink() {
    // Not implemented
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void setFederationLink(String link) {
    // Not implemented
  }

  /** {@inheritDoc} */
  @Override
  public String getServiceAccountClientLink() {
    // Not implemented
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void setServiceAccountClientLink(String clientInternalId) {
    // Not implemented
  }

  /** {@inheritDoc} */
  @Override
  public Set<RoleModel> getRealmRoleMappings() {
    return getRoleMappings();
  }

  /** {@inheritDoc} */
  @Override
  public Set<RoleModel> getClientRoleMappings(ClientModel app) {
    return getRoleMappings();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasRole(RoleModel role) {
    return getRoleMappings().contains(role);
  }

  /** {@inheritDoc} */
  @Override
  public void grantRole(RoleModel role) {
    // no op
  }

  /** {@inheritDoc} */
  @Override
  public Set<RoleModel> getRoleMappings() {
    logger.infof("getRoleMappings() with realm %s",realm);
    return groupsAndRoles.values().stream()
        .flatMap(List::stream)
        .map(role -> new HTTPRoleModel(role, realm))
        .distinct()
        .collect(Collectors.toSet());
  }

  /** {@inheritDoc} */
  @Override
  public void deleteRoleMapping(RoleModel role) {
    // no op
  }

  public void setRealm(RealmModel realm) {
    this.realm = realm;
  }

  public Map<String, List<String>> getGroupsAndRoles() {
    return groupsAndRoles;
  }

  public void setGroupsAndRoles(Map<String, List<String>> groupsAndRoles) {
    this.groupsAndRoles = groupsAndRoles;
  }

  @Override
  public String toString() {
    return String.format(
        "HTTPUserModel(id=%s, username=%s, firstName=%s, lastName=%s, email=%s, emailVerified=%b, createdTimestamp=%d, enabled=%b, groupsAndRoles=%s)",
        id,
        username,
        firstName,
        lastName,
        email,
        emailVerified,
        createdTimestamp,
        enabled,
        groupsAndRoles);
  }

  public void setRequiredActions(Set<String> requiredActions) {
    this.requiredActions = requiredActions;
  }
}
