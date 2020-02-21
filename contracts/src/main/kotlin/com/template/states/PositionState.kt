package com.template.states

import com.template.contracts.GameContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(GameContract::class)
data class PositionState(
        val owner: Party,
        val gameName: String,
        val positionName: String,
        val containsShip: Boolean,
        val originallyOwnedBy: Party = owner
) : ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(owner, originallyOwnedBy)
}
