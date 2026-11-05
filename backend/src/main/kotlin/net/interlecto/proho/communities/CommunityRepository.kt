package net.interlecto.proho.communities

import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface CommunityRepository : CrudRepository<Community, UUID>
