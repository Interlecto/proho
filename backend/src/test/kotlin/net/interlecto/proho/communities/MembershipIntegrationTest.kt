package net.interlecto.proho.communities

import net.interlecto.proho.EmbeddedPostgres
import net.interlecto.proho.audit.AuditEventRepository
import net.interlecto.proho.identity.AccountResponse
import net.interlecto.proho.identity.LoginRequest
import net.interlecto.proho.identity.LoginResponse
import net.interlecto.proho.identity.RegisterAccountRequest
import net.interlecto.proho.residents.CreatePersonRequest
import net.interlecto.proho.residents.PersonResponse
import net.interlecto.proho.tenancy.asCommunity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class MembershipIntegrationTest {
    companion object {
        private val sequence = AtomicInteger()

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = EmbeddedPostgres.register(registry)
    }

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var auditEvents: AuditEventRepository

    private fun <T : Any> call(
        token: String,
        method: HttpMethod,
        path: String,
        body: Any?,
        type: Class<T>,
    ): ResponseEntity<T> = rest.exchange(path, method, HttpEntity(body, HttpHeaders().apply { setBearerAuth(token) }), type)

    private inner class User(val email: String) {
        val token: String

        init {
            rest.postForEntity("/accounts", RegisterAccountRequest(email, "correct-horse-battery", "User"), String::class.java)
            token = rest.postForEntity("/auth/login", LoginRequest(email, "correct-horse-battery"), LoginResponse::class.java).body!!.token
        }

        fun post(
            path: String,
            body: Any? = null,
        ) = call(token, HttpMethod.POST, path, body, String::class.java)

        fun get(path: String) = call(token, HttpMethod.GET, path, null, String::class.java)

        fun communities() = call(token, HttpMethod.GET, "/communities", null, Array<CommunityResponse>::class.java).body!!.map { it.id }

        fun accept(invitationToken: String) = post("/invitations/accept", InvitationTokenRequest(invitationToken))

        fun preview(invitationToken: String) =
            call(token, HttpMethod.POST, "/invitations/preview", InvitationTokenRequest(invitationToken), InvitationPreviewResponse::class.java)
    }

    private fun user(tag: String = "user") = User("$tag-${sequence.incrementAndGet()}@example.com")

    private inner class Fixture {
        val admin = user("admin")
        val id: UUID =
            call(
                admin.token,
                HttpMethod.POST,
                "/communities",
                CreateCommunityRequest("mem-${sequence.incrementAndGet()}", "Los Robles", kind = "ph_residential"),
                CommunityResponse::class.java,
            ).body!!.id
        val base = "/communities/$id"

        fun invite(
            email: String,
            personId: UUID? = null,
        ) = call(admin.token, HttpMethod.POST, "$base/invitations", CreateInvitationRequest(email, personId), InvitationResponse::class.java)

        fun members() = call(admin.token, HttpMethod.GET, "$base/memberships", null, Array<MemberResponse>::class.java).body!!.toList()

        fun membershipOf(email: String) = members().single { it.email == email }

        fun adminMembership() = membershipOf(admin.email).membershipId

        /** A user who has accepted an invitation, and their membership id. */
        fun member(): Pair<User, UUID> {
            val user = user("member")
            user.accept(invite(user.email).body!!.token!!)
            return user to membershipOf(user.email).membershipId
        }

        fun assign(
            membershipId: UUID,
            role: String,
        ) = admin.post("$base/memberships/$membershipId/roles", AssignRoleRequest(role))

        fun person() =
            call(
                admin.token,
                HttpMethod.POST,
                "$base/people",
                CreatePersonRequest("natural", "Ana", "Gómez"),
                PersonResponse::class.java,
            ).body!!
    }

    private fun assertError(
        response: ResponseEntity<String>,
        status: HttpStatus,
        code: String,
    ) {
        assertThat(response.statusCode).isEqualTo(status)
        assertThat(response.body).contains(code)
    }

    // Inviting

    @Test
    fun `an invited user previews and accepts, and becomes an active member with no role`() {
        val f = Fixture()
        val invitee = user("invitee")

        val token = f.invite(invitee.email).body!!.token!!

        val preview = invitee.preview(token).body!!
        assertThat(preview.communityName).isEqualTo("Los Robles")
        assertThat(preview.matchesAccount).isTrue()
        assertThat(invitee.communities()).isEmpty()

        val accepted = invitee.accept(token)

        assertThat(accepted.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(invitee.communities()).containsExactly(f.id)
        assertThat(f.membershipOf(invitee.email).roles).isEmpty()
        assertThat(f.membershipOf(invitee.email).status).isEqualTo("active")
    }

    @Test
    fun `an invitation works for someone who registers afterwards`() {
        val f = Fixture()
        val email = "later-${sequence.incrementAndGet()}@example.com"
        val token = f.invite(email).body!!.token!!

        assertThat(User(email).accept(token).statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `the invited email is matched regardless of case`() {
        val f = Fixture()
        val tag = sequence.incrementAndGet()
        val invitee = User("Mixed.Case-$tag@Example.com")

        val invitation = f.invite("mixed.case-$tag@EXAMPLE.com").body!!

        assertThat(invitation.email).isEqualTo("mixed.case-$tag@example.com")
        assertThat(invitee.accept(invitation.token!!).statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `a forwarded link is useless to another account`() {
        val f = Fixture()
        val invitee = user("invitee")
        val other = user("other")
        val token = f.invite(invitee.email).body!!.token!!

        assertThat(other.preview(token).body!!.matchesAccount).isFalse()
        assertError(other.accept(token), HttpStatus.FORBIDDEN, "invitation_email_mismatch")
        assertThat(other.communities()).isEmpty()
        assertThat(invitee.accept(token).statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `a token works once, and a made-up one looks the same`() {
        val f = Fixture()
        val invitee = user("invitee")
        val token = f.invite(invitee.email).body!!.token!!
        invitee.accept(token)

        assertError(invitee.accept(token), HttpStatus.NOT_FOUND, "invitation_invalid")
        assertError(invitee.post("/invitations/preview", InvitationTokenRequest(token)), HttpStatus.NOT_FOUND, "invitation_invalid")
        assertError(invitee.accept("made-up"), HttpStatus.NOT_FOUND, "invitation_invalid")
    }

    @Test
    fun `an expired invitation can't be accepted, and the email can be invited again`() {
        val f = Fixture()
        val invitee = user("invitee")
        val first = f.invite(invitee.email).body!!
        EmbeddedPostgres.superuser().update("update invitation set expires_at = now() - interval '1 minute' where id = ?", first.id)

        assertError(invitee.accept(first.token!!), HttpStatus.NOT_FOUND, "invitation_invalid")

        val second = f.invite(invitee.email)
        assertThat(second.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(invitee.accept(second.body!!.token!!).statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `a revoked invitation can't be accepted`() {
        val f = Fixture()
        val invitee = user("invitee")
        val invitation = f.invite(invitee.email).body!!

        val revoked = f.admin.post("${f.base}/invitations/${invitation.id}/revoke")

        assertThat(revoked.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertError(invitee.accept(invitation.token!!), HttpStatus.NOT_FOUND, "invitation_invalid")
        val open = call(f.admin.token, HttpMethod.GET, "${f.base}/invitations", null, Array<InvitationResponse>::class.java).body!!
        assertThat(open).isEmpty()
    }

    @Test
    fun `one open invitation per email, and none for a current member`() {
        val f = Fixture()
        val email = "dup-${sequence.incrementAndGet()}@example.com"
        f.invite(email)

        val again = call(f.admin.token, HttpMethod.POST, "${f.base}/invitations", CreateInvitationRequest(email), String::class.java)
        assertError(again, HttpStatus.CONFLICT, "invitation_pending")

        val member = call(f.admin.token, HttpMethod.POST, "${f.base}/invitations", CreateInvitationRequest(f.admin.email), String::class.java)
        assertError(member, HttpStatus.CONFLICT, "already_member")
    }

    @Test
    fun `the token isn't stored or listed, and the audit log doesn't hold the email`() {
        val f = Fixture()
        val email = "secret-${sequence.incrementAndGet()}@example.com"
        val invitation = f.invite(email).body!!

        val listed = f.admin.get("${f.base}/invitations").body!!
        assertThat(listed).contains(invitation.id.toString()).doesNotContain(invitation.token!!)

        val stored =
            EmbeddedPostgres.superuser().queryForObject("select token_hash from invitation where id = ?", String::class.java, invitation.id)
        assertThat(stored).isNotEqualTo(invitation.token)

        val event = asCommunity(f.id) { auditEvents.findAll().single { it.entityId == invitation.id } }
        assertThat(event.action).isEqualTo("created")
        assertThat(event.summary).isNull()
    }

    @Test
    fun `only an administrator manages invitations`() {
        val f = Fixture()
        val (member, _) = f.member()
        val invitation = f.invite("x-${sequence.incrementAndGet()}@example.com").body!!

        assertError(member.post("${f.base}/invitations", CreateInvitationRequest("y@example.com")), HttpStatus.FORBIDDEN, "role_not_held")
        assertError(member.get("${f.base}/invitations"), HttpStatus.FORBIDDEN, "role_not_held")
        assertError(member.post("${f.base}/invitations/${invitation.id}/revoke"), HttpStatus.FORBIDDEN, "role_not_held")
    }

    @Test
    fun `another community can't see or revoke an invitation`() {
        val f = Fixture()
        val other = Fixture()
        val invitation = f.invite("x-${sequence.incrementAndGet()}@example.com").body!!

        assertError(other.admin.post("${other.base}/invitations/${invitation.id}/revoke"), HttpStatus.NOT_FOUND, "not_found_in_community")
        assertError(other.admin.post("${f.base}/invitations/${invitation.id}/revoke"), HttpStatus.FORBIDDEN, "membership_not_active")
        assertThat(other.admin.get("${other.base}/invitations").body).doesNotContain(invitation.id.toString())
    }

    @Test
    fun `the lookup function finds only an exact hash, and the application can't read other invitations`() {
        val f = Fixture()
        val invitation = f.invite("x-${sequence.incrementAndGet()}@example.com").body!!
        val app = net.interlecto.proho.tenancy.TenantJdbc

        assertThat(app.countThroughLookup("not-a-hash")).isZero()
        assertThat(app.countThroughLookup("%")).isZero()
        assertThat(app.countVisibleInvitations(invitation.id)).isZero()
    }

    // Linking through an invitation

    @Test
    fun `an invitation can link the person, who then has their profile`() {
        val f = Fixture()
        val person = f.person()
        val invitee = user("invitee")

        invitee.accept(f.invite(invitee.email, person.id).body!!.token!!)

        val profile = call(invitee.token, HttpMethod.GET, "${f.base}/profile", null, PersonResponse::class.java).body!!
        assertThat(profile.id).isEqualTo(person.id)
    }

    @Test
    fun `an invitation can't promise a person who is taken, archived or elsewhere`() {
        val f = Fixture()
        val other = Fixture()
        val promised = f.person()
        val emailOne = "one-${sequence.incrementAndGet()}@example.com"
        val emailTwo = "two-${sequence.incrementAndGet()}@example.com"
        f.invite(emailOne, promised.id)

        val samePerson =
            call(f.admin.token, HttpMethod.POST, "${f.base}/invitations", CreateInvitationRequest(emailTwo, promised.id), String::class.java)
        assertError(samePerson, HttpStatus.CONFLICT, "person_already_linked")

        val foreign =
            call(f.admin.token, HttpMethod.POST, "${f.base}/invitations", CreateInvitationRequest(emailTwo, other.person().id), String::class.java)
        assertError(foreign, HttpStatus.BAD_REQUEST, "not_found_in_community")

        val archived = f.person()
        f.admin.post("${f.base}/people/${archived.id}/archive")
        val gone =
            call(f.admin.token, HttpMethod.POST, "${f.base}/invitations", CreateInvitationRequest(emailTwo, archived.id), String::class.java)
        assertError(gone, HttpStatus.BAD_REQUEST, "not_found_in_community")
    }

    @Test
    fun `someone joins without the link if the person was archived after the invitation`() {
        val f = Fixture()
        val person = f.person()
        val invitee = user("invitee")
        val token = f.invite(invitee.email, person.id).body!!.token!!
        f.admin.post("${f.base}/people/${person.id}/archive")

        assertThat(invitee.accept(token).statusCode).isEqualTo(HttpStatus.OK)

        assertThat(f.membershipOf(invitee.email).personId).isNull()
    }

    // Roles

    @Test
    fun `a role works on the next request, and stops when it ends`() {
        val f = Fixture()
        val (member, id) = f.member()
        val createUnit = "${f.base}/buildings"
        val request = mapOf("name" to "Torre")
        assertThat(member.post(createUnit, request).statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        assertThat(f.assign(id, "administrator").statusCode).isEqualTo(HttpStatus.OK)
        assertThat(member.post(createUnit, request).statusCode).isEqualTo(HttpStatus.CREATED)

        assertThat(f.admin.post("${f.base}/memberships/$id/roles/administrator/end").statusCode).isEqualTo(HttpStatus.OK)
        assertThat(member.post(createUnit, request).statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `a role can't be held twice, ended when absent, or made up`() {
        val f = Fixture()
        val (_, id) = f.member()
        f.assign(id, "treasurer")

        assertError(f.assign(id, "treasurer"), HttpStatus.CONFLICT, "role_already_held")
        assertError(f.admin.post("${f.base}/memberships/$id/roles/auditor/end"), HttpStatus.CONFLICT, "role_not_assigned")
        assertError(f.assign(id, "owner"), HttpStatus.BAD_REQUEST, "invalid_request")
        assertError(f.admin.post("${f.base}/memberships/$id/roles/owner/end"), HttpStatus.BAD_REQUEST, "invalid_request")
    }

    @Test
    fun `an ended role can be assigned again, and history stays`() {
        val f = Fixture()
        val (_, id) = f.member()
        f.assign(id, "treasurer")
        f.admin.post("${f.base}/memberships/$id/roles/treasurer/end")

        assertThat(f.assign(id, "treasurer").statusCode).isEqualTo(HttpStatus.OK)

        val rows = EmbeddedPostgres.superuser().queryForObject("select count(*) from role_assignment where membership_id = ?", Int::class.java, id)
        assertThat(rows).isEqualTo(2)
        assertThat(f.membershipOf(f.members().single { it.membershipId == id }.email).roles).containsExactly("treasurer")
    }

    @Test
    fun `only an administrator manages roles and status, not even a treasurer`() {
        val f = Fixture()
        val (treasurer, id) = f.member()
        f.assign(id, "treasurer")
        val admin = f.adminMembership()

        val attempts =
            listOf(
                treasurer.post("${f.base}/memberships/$id/roles", AssignRoleRequest("administrator")),
                treasurer.post("${f.base}/memberships/$admin/roles/administrator/end"),
                treasurer.post("${f.base}/memberships/$admin/suspend"),
                treasurer.post("${f.base}/memberships/$admin/remove"),
                treasurer.get("${f.base}/memberships"),
            )

        attempts.forEach { assertError(it, HttpStatus.FORBIDDEN, "role_not_held") }
    }

    @Test
    fun `another community's membership isn't found`() {
        val f = Fixture()
        val other = Fixture()
        val (_, id) = f.member()

        val attempts =
            listOf(
                other.assign(id, "administrator"),
                other.admin.post("${other.base}/memberships/$id/suspend"),
                other.admin.post("${other.base}/memberships/$id/remove"),
            )

        attempts.forEach { assertError(it, HttpStatus.NOT_FOUND, "not_found_in_community") }
    }

    // Status

    @Test
    fun `a suspended member loses access at once, and gets it back`() {
        val f = Fixture()
        val (member, id) = f.member()

        f.admin.post("${f.base}/memberships/$id/suspend")

        assertError(member.get("${f.base}/units"), HttpStatus.FORBIDDEN, "membership_not_active")
        assertThat(member.communities()).isEmpty()
        assertThat(f.members().single { it.membershipId == id }.status).isEqualTo("suspended")

        f.admin.post("${f.base}/memberships/$id/reactivate")

        assertThat(member.get("${f.base}/units").statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `an invitation can't lift a suspension`() {
        val f = Fixture()
        val (member, id) = f.member()
        f.admin.post("${f.base}/memberships/$id/suspend")

        val invite = call(f.admin.token, HttpMethod.POST, "${f.base}/invitations", CreateInvitationRequest(member.email), String::class.java)
        assertError(invite, HttpStatus.CONFLICT, "already_member")

        // An invitation sent before they joined by some other route is still refused.
        val other = user("other")
        val token = f.invite(other.email).body!!.token!!
        val accountId = call(other.token, HttpMethod.GET, "/me", null, AccountResponse::class.java).body!!.id
        EmbeddedPostgres.superuser().update(
            "insert into membership (id, account_id, community_id, status) values (gen_random_uuid(), ?, ?, 'suspended')",
            accountId,
            f.id,
        )

        assertError(other.accept(token), HttpStatus.CONFLICT, "already_member")
        assertThat(other.communities()).isEmpty()
        assertThat(member.communities()).isEmpty()
    }

    @Test
    fun `a removed member loses access and roles, frees their person, and can be invited again`() {
        val f = Fixture()
        val person = f.person()
        val member = user("member")
        member.accept(f.invite(member.email, person.id).body!!.token!!)
        val id = f.membershipOf(member.email).membershipId
        f.assign(id, "treasurer")

        assertThat(f.admin.post("${f.base}/memberships/$id/remove").statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        assertError(member.get("${f.base}/units"), HttpStatus.FORBIDDEN, "membership_not_active")
        assertThat(f.members().map { it.membershipId }).doesNotContain(id)
        assertError(f.admin.post("${f.base}/memberships/$id/remove"), HttpStatus.NOT_FOUND, "not_found_in_community")

        member.accept(f.invite(member.email, person.id).body!!.token!!)
        val again = f.membershipOf(member.email)
        assertThat(again.membershipId).isNotEqualTo(id)
        assertThat(again.roles).isEmpty()
        assertThat(again.personId).isEqualTo(person.id)
    }

    @Test
    fun `a member can leave`() {
        val f = Fixture()
        val (member, _) = f.member()

        assertThat(member.post("${f.base}/membership/leave").statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        assertThat(member.communities()).isEmpty()
    }

    // The last administrator

    @Test
    fun `the last administrator can't step down, be suspended, be removed or leave`() {
        val f = Fixture()
        val id = f.adminMembership()

        val attempts =
            listOf(
                f.admin.post("${f.base}/memberships/$id/roles/administrator/end"),
                f.admin.post("${f.base}/memberships/$id/suspend"),
                f.admin.post("${f.base}/memberships/$id/remove"),
                f.admin.post("${f.base}/membership/leave"),
            )

        attempts.forEach { assertError(it, HttpStatus.CONFLICT, "last_administrator") }
        assertThat(f.admin.get("${f.base}/units").statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `an administrator can go once there's another, and a suspended one doesn't count`() {
        val f = Fixture()
        val (second, secondId) = f.member()
        f.assign(secondId, "administrator")

        f.admin.post("${f.base}/memberships/$secondId/suspend")
        assertError(f.admin.post("${f.base}/membership/leave"), HttpStatus.CONFLICT, "last_administrator")

        f.admin.post("${f.base}/memberships/$secondId/reactivate")
        assertThat(f.admin.post("${f.base}/membership/leave").statusCode).isEqualTo(HttpStatus.NO_CONTENT)

        assertError(second.post("${f.base}/membership/leave"), HttpStatus.CONFLICT, "last_administrator")
    }

    @Test
    fun `changes to membership are audited`() {
        val f = Fixture()
        val (_, id) = f.member()
        f.assign(id, "treasurer")
        f.admin.post("${f.base}/memberships/$id/roles/treasurer/end")
        f.admin.post("${f.base}/memberships/$id/suspend")
        f.admin.post("${f.base}/memberships/$id/reactivate")
        f.admin.post("${f.base}/memberships/$id/remove")

        val events = asCommunity(f.id) { auditEvents.findAll().filter { it.entityId == id } }

        assertThat(events.map { it.action })
            .containsExactlyInAnyOrder("role_assigned", "role_ended", "suspended", "reactivated", "removed")
        assertThat(events.single { it.action == "role_assigned" }.summary).isEqualTo("treasurer")
    }
}
