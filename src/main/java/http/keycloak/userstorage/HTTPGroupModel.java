package http.keycloak.userstorage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 * A specific HTTP Group model based on a JSON. Groups and roles are only named (no identifier, because name is unique)
 */
public class HTTPGroupModel implements GroupModel {
  private final String name;
  private final RealmModel realm;
  private final List<String> roles;
  private GroupModel parent;
  private Set<GroupModel> childs = new LinkedHashSet<>();
  private HashMap<String,List<String>> attributes = new HashMap<>();

  public HTTPGroupModel(String name, List<String> roles, RealmModel realm) {
    this.name = name;
    this.roles = roles;
    this.realm = realm;
  }

  @Override
  public Set<RoleModel> getRealmRoleMappings() {
    return getRoleMappings();
  }

  @Override
  public Set<RoleModel> getClientRoleMappings(ClientModel app) {
    return getRoleMappings();
  }

  @Override
  public boolean hasRole(RoleModel role) {
    return getRoleMappings().contains(role);
  }

  @Override
  public void grantRole(RoleModel role) {
    roles.add(role.getName());
  }

  @Override
  public Set<RoleModel> getRoleMappings() {
    return roles.stream().map(role -> new HTTPRoleModel(role, realm)).collect(Collectors.toSet());
  }

  @Override
  public void deleteRoleMapping(RoleModel role) {
    roles.remove(role.getName());
  }

  @Override
  public String getId() {
    return name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    // no op
  }

  @Override
  public void setSingleAttribute(String name, String value) {
    attributes.put(name, Arrays.asList(value));
  }

  @Override
  public void setAttribute(String name, List<String> values) {
    attributes.put(name, values);
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public String getFirstAttribute(String name) {
    if (attributes.containsKey(name)) return attributes.get(name).get(0);
    return null;
  }

  @Override
  public List<String> getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Map<String, List<String>> getAttributes() {
    return attributes;
  }

  @Override
  public GroupModel getParent() {
    return parent;
  }

  @Override
  public String getParentId() {
    if (parent != null) return parent.getId();
    return null;
  }

  @Override
  public Set<GroupModel> getSubGroups() {
    return childs;
  }

  @Override
  public void setParent(GroupModel group) {
    this.parent = group;
  }

  @Override
  public void addChild(GroupModel subGroup) {
    childs.add(subGroup);
  }

  @Override
  public void removeChild(GroupModel subGroup) {
    childs.remove(subGroup);
  }
  
  public RealmModel setRealm() {
    return realm;
  }

  public String toString() {
    return String.format("HTTPGroupModel(name=%s, roles=%s, childs=%s, attributes=%s)", name, roles, childs, attributes);
  }
}
