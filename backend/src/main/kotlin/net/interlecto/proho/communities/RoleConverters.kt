package net.interlecto.proho.communities

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class RoleToStringConverter : Converter<Role, String> {
    override fun convert(source: Role) = source.value
}

@ReadingConverter
class StringToRoleConverter : Converter<String, Role> {
    override fun convert(source: String) = Role.fromValue(source)
}
