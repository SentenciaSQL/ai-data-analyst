package com.aidataanalyst.identity.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthGateway {

    private final JdbcTemplate jdbc;

    public AuthGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean emailExists(String email) {
        Boolean exists = jdbc.queryForObject("SELECT app.email_exists(?)", Boolean.class, email);
        return Boolean.TRUE.equals(exists);
    }

    public boolean slugExists(String slug) {
        Boolean exists = jdbc.queryForObject("SELECT app.slug_exists(?)", Boolean.class, slug);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<LoginUserRow> findLoginUser(String email) {
        return jdbc.query(
                """
                SELECT user_id, tenant_id, email, password_hash, user_status, tenant_status
                FROM app.find_login_user(?)
                """,
                (rs, rowNum) -> mapLogin(rs),
                email).stream().findFirst();
    }

    public Optional<RefreshTokenRow> findRefreshToken(String tokenHash) {
        return jdbc.query(
                """
                SELECT token_id, tenant_id, user_id, expires_at, revoked_at
                FROM app.find_refresh_token(?)
                """,
                (rs, rowNum) -> mapRefresh(rs),
                tokenHash).stream().findFirst();
    }

    private LoginUserRow mapLogin(ResultSet rs) throws SQLException {
        return new LoginUserRow(
                rs.getObject("user_id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("user_status"),
                rs.getString("tenant_status"));
    }

    private RefreshTokenRow mapRefresh(ResultSet rs) throws SQLException {
        return new RefreshTokenRow(
                rs.getObject("token_id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                instant(rs, "expires_at"),
                instant(rs, "revoked_at"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record LoginUserRow(
            UUID userId,
            UUID tenantId,
            String email,
            String passwordHash,
            String userStatus,
            String tenantStatus) {
    }

    public record RefreshTokenRow(
            UUID tokenId,
            UUID tenantId,
            UUID userId,
            Instant expiresAt,
            Instant revokedAt) {
    }
}
