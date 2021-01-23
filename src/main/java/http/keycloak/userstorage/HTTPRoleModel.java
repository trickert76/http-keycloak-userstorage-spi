package http.keycloak.userstorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;

public class HTTPRoleModel implements RoleModel {
  private final String name;
  private final RealmModel realm;
  private RoleModel parent = null;
  private Set<RoleModel> childs = new LinkedHashSet<>();
  private HashMap<String,List<String>> attributes = new HashMap<>();

  public HTTPRoleModel(String name, RealmModel realm) {
    this.name = name;
    this.realm = realm;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return name;
  }

  @Override
  public void setDescription(String description) {
    // no op
  }

  @Override
  public String getId() {
    return name;
  }

  @Override
  public void setName(String name) {
    // no op
  }

  @Override
  public boolean isComposite() {
    return childs.size()>0;
  }

  @Override
  public void addCompositeRole(RoleModel role) {
    childs.add(role);
  }

  @Override
  public void removeCompositeRole(RoleModel role) {
    childs.remove(role);
  }

  @Override
  public Set<RoleModel> getComposites() {
    return childs;
  }

  @Override
  public boolean isClientRole() {
    return parent != null;
  }

  @Override
  public String getContainerId() {
    return realm.getId();
  }

  @Override
  public RoleContainerModel getContainer() {
    return realm;
  }

  @Override
  public boolean hasRole(RoleModel role) {
    return childs.contains(role);
  }

  @Override
  public void setSingleAttribute(String name, String value) {
    attributes.put(name, Arrays.asList(value));
  }

  @Override
  public void setAttribute(String name, Collection<String> values) {
    attributes.put(name, new ArrayList<>(values));
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public String getFirstAttribute(String name) {
    if (attributes.containsKey(name) && attributes.get(name).size() > 0) return attributes.get(name).get(0);
    return null;
  }

  @Override
  public List<String> getAttribute(String name) {
    if (attributes.containsKey(name)) return attributes.get(name);
    return null;
  }

  @Override
  public Map<String, List<String>> getAttributes() {
    return attributes;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof HTTPRoleModel) {
      return getId().equals(((HTTPRoleModel) obj).getId());
    }
    return false;
  }

  public String toString() {
    return String.format("HTTPRoleModel(name=%s)", name);
  }
}
