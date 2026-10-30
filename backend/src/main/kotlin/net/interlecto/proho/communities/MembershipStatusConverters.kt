package net.interlecto.proho.communities

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class MembershipStatusToStringConverter : Converter<MembershipStatus, String> {
    override fun convert(source: MembershipStatus) = source.value
}

@ReadingConverter
class StringToMembershipStatusConverter : Converter<String, MembershipStatus> {
    override fun convert(source: String) = MembershipStatus.fromValue(source)
}
