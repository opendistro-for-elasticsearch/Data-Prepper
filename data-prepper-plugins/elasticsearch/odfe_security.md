## Open Distro For Elasticsearch


The Elasticsearch sink can send trace data to opendistro-for-elasticsearch (ODFE) cluster by administrative credentials as follows:

```
sink:
  - elasticsearch:
      ...
      username: "admin"
      password: "admin"
```

or by using user credential assigned with a role that has the below required permissions.

### Cluster permissions

- `cluster_all`
- `indices:admin/template/get`
- `indices:admin/template/put`

Note that `indices:admin/template/*` need to be in cluster permissions.

### Index permissions

- `Index`: `otel-v1*`; `Index permissions`: `indices_all`
- `Index`: `.opendistro-ism-config`; `Index permissions`: `indices_all`
- `Index`: `*`; `Index permission`: `manage_aliases`

`Field level security` and `Anonymization` should be left with default values.

---------------

With administrative privilege, one can create an internal user, a role and map the user to the role by following the ODFE [instructions](https://opendistro.github.io/for-elasticsearch-docs/docs/security/access-control/users-roles/).
