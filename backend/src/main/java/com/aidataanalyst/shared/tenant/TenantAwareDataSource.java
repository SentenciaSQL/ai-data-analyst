package com.aidataanalyst.shared.tenant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Applies the current tenant to each borrowed connection. PostgreSQL row-level
 * security reads {@code app.current_tenant_id}. The value is taken from the
 * request thread, never from client-supplied SQL.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final Set<String> STATEMENT_METHODS = Set.of(
            "createStatement", "prepareStatement", "prepareCall");

    public TenantAwareDataSource(DataSource delegate) {
        super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrap(obtainTargetDataSource().getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrap(obtainTargetDataSource().getConnection(username, password));
    }

    private Connection wrap(Connection connection) throws SQLException {
        applyTenant(connection);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        try {
                            clearTenant(connection);
                        } catch (SQLException ignored) {
                            // Return the connection to the pool even if reset fails.
                        }
                    } else if (STATEMENT_METHODS.contains(method.getName())) {
                        applyTenant(connection);
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException ex) {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        if (cause instanceof RuntimeException runtime) {
                            throw runtime;
                        }
                        if (cause instanceof Error error) {
                            throw error;
                        }
                        if (cause instanceof SQLException sql) {
                            throw sql;
                        }
                        throw new SQLException(cause);
                    }
                });
    }

    private static void applyTenant(Connection connection) throws SQLException {
        setTenant(connection, TenantContext.tenantId().map(id -> id.toString()).orElse(""));
    }

    private static void clearTenant(Connection connection) throws SQLException {
        setTenant(connection, "");
    }

    private static void setTenant(Connection connection, String tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('app.current_tenant_id', ?, false)")) {
            statement.setString(1, tenantId);
            statement.execute();
        }
    }
}
