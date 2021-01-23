# http-keycloak-userstorage-spi

An example for having a custom user storage for Keycloak users, groups and roles.

Keycloak allows to have your own UserStorage backend. So you don't need to have an AD or LDAP. A possible use-case is for example an existing legacy system with its own user storage and you want to build some new services around it. When you then start with OIDC, you can use Keycloak as a OIDC provider and that can use your legacy backend for user storage.

This project uses the Keycloak UserStorage service provider interface to allow an HTTP client to read users from such a backend.

The backend itself needs at least this REST endpoints.

- GET /user - returns a list of HTTPUserModel. It supports paging and filtering (offset, limit) with the query param search for random search or group.
- GET /user/{username} - returns a single HTTPUserModel that matches the given username. If the HTTP response is not 200, there is no match.
- GET /user/mail/{email} - returns a single HTTPUserModel that matches the given mail address. If the HTTP response is not 200, there is no match.
- POST /user/validate/{username} - the POST body contains the password. This is used for validating the users password.

The HTTPUserModel contains some basic informations about the user for Keycloak, like the username, first and last name, email and attributes. If you want to apply groups and roles to the user (which is useful, if your services depends on different roles) your backend needs to fill the HashMap<String,List<String>>. Where the key is the group name and the List<String> is the list of role names. Of course you can build complexer GroupModels and RoleModels, if you want.