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
            is Commands.NewGame -> requireThat {
                "There should be no input state" using (tx.inputs.isEmpty())
                "There should be only one output state" using (tx.outputs.count() == 1)
                "The output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                val outputGameState = tx.outputs[0].data as GameState
                "The name must have length > 0" using (outputGameState.name.isNotEmpty())
                "The maxScore must be 18" using (outputGameState.maxScore == 18)
                "P1 and P2 must be different" using (outputGameState.p1 != outputGameState.p2)
                "p1SetPos must be false" using (!outputGameState.p1SetPos)
                "p2SetPos must be false" using (!outputGameState.p2SetPos)
                "turn must be p1" using (outputGameState.turn == outputGameState.p1)
            }
            is Commands.UpdateGame -> requireThat {
                "There should be one input state" using (tx.inputs.count() == 1)
                "There should be only one output state" using (tx.outputs.count() == 1)
                "The input state must be of type ${GameState::class.simpleName}" using (tx.inputs[0].state.data is GameState)
                "The output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                val inputGameState = tx.inputs[0].state.data as GameState
                val outputGameState = tx.outputs[0].data as GameState
                "p1 must not change" using (inputGameState.p1 == outputGameState.p1)
                "p2 must not change" using (inputGameState.p2 == outputGameState.p2)
                "linearId must not change" using (inputGameState.linearId == outputGameState.linearId)
                "maxScore must not change" using (inputGameState.maxScore == outputGameState.maxScore)
                "name must not change" using (inputGameState.name == outputGameState.name)
                "turn is one of p1 or p2" using (outputGameState.turn in setOf(outputGameState.p1, outputGameState.p2))
            }
            is Commands.SetPos -> requireThat {
                "There should be one input state" using (tx.inputs.count() == 1)
                "There should be 101 output states" using (tx.outputs.count() == 101)
                "The input state must be of type ${GameState::class.simpleName}" using (tx.inputs[0].state.data is GameState)
                "The first output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                "The last 100 output states must be of type ${PositionState::class.simpleName}" using (tx.outputs.asSequence().drop(1).all { it.data is PositionState })
                val inputGameState = tx.inputs[0].state.data as GameState
                val outputPositions = tx.outputs.asSequence().drop(1).map { it.data as PositionState }
                "The number of set positions must match maxScore" using (outputPositions.filter { it.containsShip }.count() == inputGameState.maxScore)
                "It is my turn" using (outputPositions.all { it.owner == inputGameState.turn && it.originallyOwnedBy == inputGameState.turn })
                val outputGameState = tx.outputs[0].data as GameState
                "The turn should move to the next player" using (outputGameState.turn != inputGameState.turn)
                "The next player should be P1 or P2" using (outputGameState.turn in setOf(inputGameState.p1, inputGameState.p2))
                "Positions are set for one player" using (outputGameState.p1SetPos || outputGameState.p2SetPos)
            }
            is Commands.Turn -> requireThat {
                "There should be two input states" using (tx.inputs.count() == 2)
                "There should be two output states" using (tx.outputs.count() == 2)
                "The first input state must be of type ${GameState::class.simpleName}" using (tx.inputs[0].state.data is GameState)
                "The second input state must be of type ${PositionState::class.simpleName}" using (tx.inputs[1].state.data is PositionState)
                "The first output state must be of type ${GameState::class.simpleName}" using (tx.outputs[0].data is GameState)
                "The second output state must be of type ${PositionState::class.simpleName}" using (tx.outputs[1].data is PositionState)
                val inputGameState = tx.inputs[0].state.data as GameState
                val inputPosition = tx.inputs[1].state.data as PositionState
                val outputGameState = tx.outputs[0].data as GameState
                val outputPosition = tx.outputs[1].data as PositionState
                "It is my turn" using (inputGameState.turn == outputPosition.owner)
                "The turn should move to the next player" using (outputGameState.turn != inputGameState.turn)
                "The next player should be P1 or P2" using (outputGameState.turn in setOf(inputGameState.p1, inputGameState.p2))
                "Ownership of position is being transferred" using (outputPosition.originallyOwnedBy != outputPosition.owner)
                "Input Position is owned by the other player" using (inputPosition.owner != inputGameState.turn)
                "Input Position has not been transferred before" using (inputPosition.owner == inputPosition.originallyOwnedBy)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class NewGame : Commands
        class UpdateGame : Commands
        class SetPos : Commands
        class Turn : Commands
    }
}