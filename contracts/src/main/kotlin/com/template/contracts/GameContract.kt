package com.template.contracts

import com.template.states.GameState
import com.template.states.PositionState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class GameContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        val ID: String = GameContract::class.qualifiedName!!
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.

        val command = tx.commands.requireSingleCommand<Commands>().value

        when (command){
            is Commands.New -> requireThat {
                "There should be no input state" using (tx.inputs.isEmpty())
                "There should be only one output state" using (tx.outputs.count() == 1)
                "The output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                val gState = tx.outputs[0].data as GameState
                "The name must have length > 0" using (gState.name.isNotEmpty())
                "The maxScore must be 18" using (gState.maxScore == 18)
                "P1 and P2 must be different" using (gState.p1 != gState.p2)
                "p1SetPos must be false" using (!gState.p1SetPos)
                "p2SetPos must be false" using (!gState.p2SetPos)
                "turn must be p1" using (gState.turn == gState.p1)
            }
            is Commands.P1SetPos -> requireThat {
                "There should be one input state" using (tx.inputs.count() == 1)
                "There should be 101 output states" using (tx.outputs.count() == 101)
                "The input state must be of type ${GameState::class.simpleName}" using (tx.inputs[0].state.data is GameState)
                "The first output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                "The last 100 output states must be of type ${PositionState::class.simpleName}" using (tx.outputs.asSequence().drop(1).all { it.data is PositionState })
                val gState = tx.inputs[0].state.data as GameState
                val positions = tx.outputs.asSequence().drop(1).map { it.data as PositionState }
                "The number of set positions must match maxScore" using (positions.filter { it.containsShip }.count() == gState.maxScore)
                "It is P1 turn" using (gState.turn == gState.p1)
                "All positions belong to P1" using (positions.all { it.owner == gState.p1 && it.originallyOwnedBy == gState.p1 })
                val outputGState = tx.outputs[0].data as GameState
                "The turn should move to P2" using (outputGState.turn == outputGState.p2)
                "Positions are set for P1" using (outputGState.p1SetPos)
            }
            is Commands.P2SetPos -> requireThat {
                "There should be one input state" using (tx.inputs.count() == 1)
                "There should be 101 output states" using (tx.outputs.count() == 101)
                "The input state must be of type ${GameState::class.simpleName}" using (tx.inputs[0].state.data is GameState)
                "The first output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                "The last 100 output states must be of type ${PositionState::class.simpleName}" using (tx.outputs.asSequence().drop(1).all { it.data is PositionState })
                val gState = tx.inputs[0].state.data as GameState
                val positions = tx.outputs.asSequence().drop(1).map { it.data as PositionState }
                "The number of set positions must match maxScore" using (positions.filter { it.containsShip }.count() == gState.maxScore)
                "It is P2 turn" using (gState.turn == gState.p2)
                "All positions belong to P2" using (positions.all { it.owner == gState.p2 && it.originallyOwnedBy == gState.p2 })
                val outputGState = tx.outputs[0].data as GameState
                "The turn should move to P1" using (outputGState.turn == outputGState.p1)
                "Positions are set for P2" using (outputGState.p2SetPos)
            }
            is Commands.SetPos -> requireThat {
                "There should be one input state" using (tx.inputs.count() == 1)
                "There should be 101 output states" using (tx.outputs.count() == 101)
                "The input state must be of type ${GameState::class.simpleName}" using (tx.inputs[0].state.data is GameState)
                "The first output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                "The last 100 output states must be of type ${PositionState::class.simpleName}" using (tx.outputs.asSequence().drop(1).all { it.data is PositionState })
                val gState = tx.inputs[0].state.data as GameState
                val positions = tx.outputs.asSequence().drop(1).map { it.data as PositionState }
                "The number of set positions must match maxScore" using (positions.filter { it.containsShip }.count() == gState.maxScore)
                "It is my turn" using (positions.all { it.owner == gState.turn && it.originallyOwnedBy == gState.turn })
                val outputGState = tx.outputs[0].data as GameState
                "The turn should move to the next player" using (outputGState.turn != gState.turn)
                "Positions are set for one player" using (outputGState.p1SetPos || outputGState.p2SetPos)
            }
            is Commands.P1Turn -> requireThat {
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
            }
            is Commands.P2Turn -> requireThat {
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)
                "" using (true)

            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class New : Commands
        class SetPos : Commands
        class P1SetPos : Commands
        class P2SetPos : Commands
        class P1Turn : Commands
        class P2Turn : Commands
    }
}