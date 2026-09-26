package org.vstu.compprehension.authorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemCapability;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.businesslogic.auth.Capability;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemRbacConsistencyTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    /** Каждое системное право и каждая системная способность заведены в таблице permission. */
    @Test
    void everySystemRightExistsInDatabase() {
        // Arrange.
        var expected = systemRightNames();

        // Act.
        var actual = new HashSet<>(jdbc.queryForList("select name from permission", String.class));

        // Assert.
        var missing = new HashSet<>(expected);
        missing.removeAll(actual);
        assertTrue(missing.isEmpty(), "Rights missing in DB: " + missing);
    }

    /** Системные права и способности роли в role_permission совпадают с SystemRole; права доменов не учитываются. */
    @ParameterizedTest
    @EnumSource(value = SystemRole.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
    void systemRoleRightsMatchDatabase(SystemRole role) {
        // Arrange.
        var expected = Stream.concat(
                        role.getPermissions().stream().map(Permission::id),
                        role.getCapabilities().stream().map(Capability::id))
                .collect(Collectors.toSet());

        // Act.
        var roleExists = jdbc.queryForObject("select count(*) from role where name = ?", Long.class, role.id());
        var actual = new HashSet<>(jdbc.queryForList("""
                select p.name
                from role r
                join role_permission rp on rp.role_id = r.id
                join permission p       on p.id = rp.permission_id
                where r.name = ?
                """, String.class, role.id()));
        actual.retainAll(systemRightNames());

        // Assert.
        assertEquals(1L, roleExists, "Role missing in DB: " + role.id());
        assertEquals(expected, actual, "Rights of role " + role.id());
    }

    private static Set<String> systemRightNames() {
        return Stream.concat(
                        Arrays.stream(SystemPermission.values())
                                .filter(p -> p != SystemPermission.UNKNOWN)
                                .map(SystemPermission::id),
                        Arrays.stream(SystemCapability.values()).map(SystemCapability::id))
                .collect(Collectors.toSet());
    }
}
