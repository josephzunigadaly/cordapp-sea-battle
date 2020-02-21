package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GContract
import com.template.states.GState
import com.template.states.Position
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class SetPosInitiator(
        val gameName:String,
        val aircraftCarrier:String,
        val battleship:String,
        val cruiser:String,
        val destroyer1:String,
        val destroyer2:String,
        val submarine1:String,
        val submarine2:String
) : FlowLogic<SignedTransaction>() {

    companion object {
//        object COLLECTING : ProgressTracker.Step("Collecting signatures from counterparties.")
//        object VERIFYING : ProgressTracker.Step("Verifying collected signatures.")

//        @JvmStatic
//        fun tracker() = ProgressTracker(COLLECTING, VERIFYING)
    }
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction {

        val gameState = serviceHub.vaultService.queryBy(GState::class.java).states.single { it.state.data.name == gameName }
        val game = gameState.state.data

        // Initiator flow logic goes here.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(GContract.Commands.SetPos(), listOf(ourIdentity).map(Party::owningKey))

        // Build output game state
        val outputGState: GState
        outputGState = if (game.p1 == ourIdentity){
            game.copy(p1SetPos = true, turn = game.p2)
        } else {
            game.copy(p2SetPos = true, turn = game.p1)
        }

        // Empty grid
        val map = mutableMapOf<String, Position>()
        for (x in 'A'..'I'){
            for (y in 0..9){
                val pos = "${x}${y}"
                map[pos] = Position(ourIdentity, gameName, pos, false)
            }
        }

        // Read ship locations and add to map
        val regex = Regex.fromLiteral("([A-Ia-i])([0-8])([NESWnesw])")

        for ((ship, size) in listOf(
                Pair(aircraftCarrier, 5),
                Pair(battleship, 4),
                Pair(cruiser, 3),
                Pair(destroyer1, 2),
                Pair(destroyer2, 2),
                Pair(submarine1, 1),
                Pair(submarine2, 1)
        )) {
            val matchResult = regex.matchEntire(ship)!!

            val col = matchResult.groups[0]!!.value[0].toUpperCase()
            val row = Integer.parseInt(matchResult.groups[1]!!.value)
            val direction = matchResult.groups[2]!!.value.toUpperCase()

            for (i in 0 until size){
                val posName: String = when (direction){
                    "N" -> "$col${row - i}"
                    "E" -> "${(col + i)}$row"
                    "S" -> "$col${row + i}"
                    "W" -> "${(col - i)}$row"
                    else -> throw IllegalArgumentException("Direction needs to be one of N, E, S, W")
                }

                // Check position already exists
                requireThat { "Ship positioned outside grid" using (map.containsKey(posName)) }

                // Update the map
                map[posName] = Position(ourIdentity, gameName, posName, true)
            }
        }

        // Build up the transaction
        val transactionBuilder = TransactionBuilder(notary)
                .addCommand(command)
                .addInputState(gameState)
                .addOutputState(outputGState, GContract.ID)
        for (p in map.values){
            transactionBuilder.addOutputState(p, GContract.ID)
        }
        transactionBuilder.verify(serviceHub)
        val transaction = serviceHub.signInitialTransaction(transactionBuilder)

        return subFlow(FinalityFlow(transaction, emptyList()))
    }
}
