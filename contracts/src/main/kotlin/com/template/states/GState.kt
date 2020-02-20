package com.template.states

import com.template.contracts.GContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(GContract::class)
data class GState(
        val p1: Party,
        val p2: Party,
        val name:String,
        val maxScore: Int = 18,
        val p1SetPos: Boolean = false,
        val p2SetPos: Boolean = false,
        val turn: Party = p1,
        val linearId: UniqueIdentifier = UniqueIdentifier()
) : ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(p1,p2)
}


