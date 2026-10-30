package net.interlecto.proho

import net.interlecto.proho.communities.MembershipStatusToStringConverter
import net.interlecto.proho.communities.RoleToStringConverter
import net.interlecto.proho.communities.StringToMembershipStatusConverter
import net.interlecto.proho.communities.StringToRoleConverter
import net.interlecto.proho.identity.AccountStatusToStringConverter
import net.interlecto.proho.identity.StringToAccountStatusConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions

// Spring Data JDBC wants exactly one JdbcCustomConversions bean app-wide
// (two would collide on the same bean name, or fight over which one wins),
// so every module's enum<->column converters are registered together
// here, in the application root, rather than one bean per module.
@Configuration
class JdbcConfig {
    @Bean
    fun jdbcCustomConversions() =
        JdbcCustomConversions(
            listOf(
                AccountStatusToStringConverter(),
                StringToAccountStatusConverter(),
                MembershipStatusToStringConverter(),
                StringToMembershipStatusConverter(),
                RoleToStringConverter(),
                StringToRoleConverter(),
            ),
        )
}
