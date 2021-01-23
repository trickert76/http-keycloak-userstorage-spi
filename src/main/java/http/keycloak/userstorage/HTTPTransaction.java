package http.keycloak.userstorage;


import org.keycloak.models.AbstractKeycloakTransaction;

public class HTTPTransaction extends AbstractKeycloakTransaction {

    private final HTTPConnector httpConnector;

    private final HTTPUserModelDelegate delegate;

    private boolean isEnlisted = false;

    public boolean isEnlisted() {
        return isEnlisted;
    }

    public void setEnlisted(boolean enlisted) {
        isEnlisted = enlisted;
    }

    public HTTPTransaction(HTTPConnector httpConnector, HTTPUserModelDelegate delegate) {
        this.httpConnector = httpConnector;
        this.delegate = delegate;
    }

    @Override
    protected void commitImpl() {
        if (delegate.isNotPersistedInHttpStorage()) {
            httpConnector.createUser(delegate.getRealmId(), delegate.getDelegatedUserModel(), delegate.isAdminTool());
            delegate.setPersistedInHttpStorage(true);
        } else {
            httpConnector.updateUser(delegate.getRealmId(), delegate.getDelegatedUserModel(), delegate.isAdminTool());
        }
    }

    @Override
    protected void rollbackImpl() {
    }

}