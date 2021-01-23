package com.wix.mysql.config;

import com.wix.mysql.distribution.Version;
import de.flapdoodle.embed.process.config.ExecutableProcessConfig;
import de.flapdoodle.embed.process.config.ISupportConfig;
import de.flapdoodle.embed.process.distribution.IVersion;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MysqldConfig extends ExecutableProcessConfig {

    private final int port;
    private final Charset charset;
    private final User rwUser;
    private final User roUser;
    private final TimeZone timeZone;
    private final Timeout timeout;
    private final List<ServerVariable> serverVariables;
    private final String tempDir;
    private final boolean enforceGtid;

    protected MysqldConfig(
            final IVersion version,
            final int port,
            final Charset charset,
            final User rwUser,
            final User roUser,
            final TimeZone timeZone,
            final Timeout timeout,
            final List<ServerVariable> serverVariables,
            final String tempDir,
            final Boolean enforceGtid) {
        super(version, new ISupportConfig() {
            public String getName() {
                return "mysqld";
            }

            public String getSupportUrl() {
                return "https://github.com/wix/wix-embedded-mysql/issues";
            }

            public String messageOnException(Class<?> context, Exception exception) {
                return "no message";
            }
        });

        if (rwUser.name.equals("root")) {
            throw new IllegalArgumentException("Usage of username 'root' is forbidden as it's reserved for system use");
        }

        if (roUser.name.equals("root")) {
            throw new IllegalArgumentException("Usage of username 'root' is forbidden as it's reserved for system use");
        }

        this.port = port;
        this.charset = charset;
        this.rwUser = rwUser;
        this.roUser = roUser;
        this.timeZone = timeZone;
        this.timeout = timeout;
        this.serverVariables = serverVariables;
        this.tempDir = tempDir;
        this.enforceGtid = enforceGtid;
    }

    public Version getVersion() {
        return (Version) version;
    }

    public Charset getCharset() {
        return charset;
    }

    public int getPort() {
        return port;
    }

    public long getTimeout(TimeUnit target) {
        return this.timeout.to(target);
    }

    public String getRWUsername() {
        return rwUser.name;
    }

    public String getRWPassword() {
        return rwUser.password;
    }

    public String getROUsername() {
        return roUser.name;
    }

    public String getROPassword() {
        return roUser.password;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public List<ServerVariable> getServerVariables() {
        return serverVariables;
    }

    public String getTempDir() {
        return tempDir;
    }

    public Boolean getEnforceGtid() {
        return enforceGtid;
    }

    public static Builder aMysqldConfig(final Version version) {
        return new Builder(version);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class Builder {
        private IVersion version;
        private int port = 3310;
        private Charset charset = Charset.defaults();
        private User rwUser = new User("rwUser", "rw");
        private User roUser = new User("roUser", "ro");
        private TimeZone timeZone = TimeZone.getTimeZone("UTC");
        private Timeout timeout = new Timeout(30, SECONDS);
        private final List<ServerVariable> serverVariables = new ArrayList<>();
        private String tempDir = "target/";
        private Boolean enforceGtid = true;

        public Builder(IVersion version) {
            this.version = version;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withFreePort() throws IOException {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return withPort(socket.getLocalPort());
            }
        }

        public Builder withTimeout(long length, TimeUnit unit) {
            this.timeout = new Timeout(length, unit);
            return this;
        }

        public Builder withCharset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder withRWUser(String username, String password) {
            this.rwUser = new User(username, password);
            return this;
        }

        public Builder withROUser(String username, String password) {
            this.roUser = new User(username, password);
            return this;
        }

        public Builder withTimeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder withTimeZone(String timeZoneId) {
            return withTimeZone(TimeZone.getTimeZone(timeZoneId));
        }

        /**
         * Provide mysql server option
         *
         * See <a href="mysqld-option-tables">http://dev.mysql.com/doc/refman/5.7/en/mysqld-option-tables.html</a>
         */
        public Builder withServerVariable(String name, boolean value) {
            serverVariables.add(new ServerVariable<>(name, value));
            return this;
        }

        /**
         * Provide mysql server int variable
         *
         * See <a href="mysqld-option-tables">http://dev.mysql.com/doc/refman/5.7/en/mysqld-option-tables.html</a>
         */
        public Builder withServerVariable(String name, int value) {
            serverVariables.add(new ServerVariable<>(name, value));
            return this;
        }

        /**
         * Provide mysql server string or enum variable
         *
         * See <a href="mysqld-option-tables">http://dev.mysql.com/doc/refman/5.7/en/mysqld-option-tables.html</a>
         */
        public Builder withServerVariable(String name, String value) {
            serverVariables.add(new ServerVariable<>(name, value));
            return this;
        }

        public Builder withTempDir(String tempDir) {
            this.tempDir = tempDir;
            return this;
        }

        public Builder withEnforceGtid(Boolean enforceGtid) {
            this.enforceGtid = enforceGtid;
            return this;
        }

        public MysqldConfig build() {
            return new MysqldConfig(version, port, charset, rwUser, roUser, timeZone, timeout, serverVariables, tempDir, enforceGtid);
        }
    }

    private static class User {
        private final String name;
        private final String password;

        User(String name, String password) {
            this.name = name;
            this.password = password;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toStringExclude(this, "password");
        }
    }

    private static class Timeout {
        private final long length;
        private final TimeUnit unit;

        Timeout(long length, TimeUnit unit) {
            this.length = length;
            this.unit = unit;
        }

        long to(TimeUnit target) {
            return target.convert(length, unit);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class ServerVariable<T> {
        private final String name;
        private final T value;

        ServerVariable(final String name, final T value) {
            this.name = name;
            this.value = value;
        }

        public String toCommandLineArgument() {
            return String.format("--%s=%s", name, value);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    public static class SystemDefaults {
        public final static String USERNAME = "root";
        public final static String SCHEMA = "information_schema";
    }

}
