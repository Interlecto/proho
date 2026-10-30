package net.interlecto.proho.identity

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

// The single JdbcCustomConversions bean that registers these (and every
// other module's) converters lives in net.interlecto.proho.JdbcConfig --
// Spring Data JDBC wants exactly one such bean app-wide, so it isn't
// declared per module.

@WritingConverter
class AccountStatusToStringConverter : Converter<AccountStatus, String> {
    override fun convert(source: AccountStatus) = source.value
}

@ReadingConverter
class StringToAccountStatusConverter : Converter<String, AccountStatus> {
    override fun convert(source: String) = AccountStatus.fromValue(source)
}
