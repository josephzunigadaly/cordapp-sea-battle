package com.template.states

import com.template.contracts.GContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(GContract::class)
data class Position(
        val owner: Party,
        val gameName: String,
        val positionName: String,
        val containsShip: Boolean,
        val originallyOwnedBy: Party = owner
) : ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner, originallyOwnedBy)
}
