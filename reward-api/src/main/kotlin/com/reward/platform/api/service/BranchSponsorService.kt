package com.reward.platform.api.service

import com.reward.platform.api.entity.BranchEntity
import com.reward.platform.api.entity.SponsorEntity
import com.reward.platform.api.entity.SponsorLocationEntity
import com.reward.platform.api.repository.BranchRepository
import com.reward.platform.api.repository.ProgramRepository
import com.reward.platform.api.repository.SponsorLocationRepository
import com.reward.platform.api.repository.SponsorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Maps operational branches to CHILD sponsors so BIT rows show the outlet, not only the program host. */
@Service
class BranchSponsorService(
    private val branchRepository: BranchRepository,
    private val programRepository: ProgramRepository,
    private val sponsorRepository: SponsorRepository,
    private val sponsorLocationRepository: SponsorLocationRepository
) {
    companion object {
        const val DEFAULT_BRANCH_CODE = "DEFAULT_MAIN"
        private const val DEFAULT_LOCATION_CODE = "ONLINE_DEFAULT"
    }

    @Transactional
    fun ensureOutletSponsor(tenantId: Long, programId: Long, branch: BranchEntity): SponsorEntity {
        branch.sponsorId?.let { id ->
            sponsorRepository.findById(id).orElse(null)?.takeIf { it.tenantId == tenantId }?.let { return it }
        }

        val host = sponsorRepository.findByTenantIdAndProgramIdOrderByName(tenantId, programId)
            .firstOrNull { it.sponsorType == "HOST" }
            ?: throw IllegalArgumentException("Program host sponsor is not configured")

        if (branch.code == DEFAULT_BRANCH_CODE && branch.parentBranchId == null) {
            return host
        }

        val sponsorCode = branch.code.trim().uppercase().replace(Regex("[^A-Z0-9_]+"), "_").take(120)
        val existing = sponsorRepository.findByTenantIdAndProgramIdAndSponsorCode(tenantId, programId, sponsorCode)
        if (existing != null) {
            linkBranch(branch, existing.id)
            return existing
        }

        val displayName = outletDisplayName(host.name, branch)
        val child = sponsorRepository.save(
            SponsorEntity(
                tenantId = tenantId,
                programId = programId,
                parentSponsorId = host.id,
                name = displayName,
                sponsorCode = sponsorCode,
                sponsorType = "CHILD",
                status = "ACTIVE"
            )
        )
        ensureDefaultLocation(tenantId, child, displayName)
        linkBranch(branch, child.id)
        return child
    }

    fun isOutletBranch(branch: BranchEntity): Boolean =
        branch.code != DEFAULT_BRANCH_CODE || branch.parentBranchId != null

    private fun linkBranch(branch: BranchEntity, sponsorId: Long) {
        if (branch.sponsorId == sponsorId) return
        branchRepository.save(branch.copy(sponsorId = sponsorId))
    }

    private fun ensureDefaultLocation(tenantId: Long, sponsor: SponsorEntity, label: String) {
        if (sponsorLocationRepository.findByTenantIdAndSponsorIdAndLocationCode(tenantId, sponsor.id, DEFAULT_LOCATION_CODE) != null) {
            return
        }
        sponsorLocationRepository.save(
            SponsorLocationEntity(
                tenantId = tenantId,
                sponsorId = sponsor.id,
                locationName = "$label Location",
                locationCode = DEFAULT_LOCATION_CODE,
                locationPin = "BR-${sponsor.id}",
                status = "ACTIVE",
                createdAt = Instant.now()
            )
        )
    }

    private fun outletDisplayName(hostName: String, branch: BranchEntity): String {
        val city = branch.city?.trim()?.takeIf { it.isNotEmpty() }
        if (city != null) {
            return "${hostName.trim()} ${city.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
        }
        val name = branch.name.trim()
        return if (name.isNotEmpty() && !name.equals(hostName, ignoreCase = true)) name else branch.code
    }
}
