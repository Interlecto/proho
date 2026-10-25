package net.interlecto.proho.identity

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions

@WritingConverter
class AccountStatusToStringConverter : Converter<AccountStatus, String> {
    override fun convert(source: AccountStatus) = source.value
}

@ReadingConverter
class StringToAccountStatusConverter : Converter<String, AccountStatus> {
    override fun convert(source: String) = AccountStatus.fromValue(source)
}

@Configuration
class IdentityJdbcConfig {
    @Bean
    fun jdbcCustomConversions() =
        JdbcCustomConversions(
            listOf(AccountStatusToStringConverter(), StringToAccountStatusConverter()),
        )
}
