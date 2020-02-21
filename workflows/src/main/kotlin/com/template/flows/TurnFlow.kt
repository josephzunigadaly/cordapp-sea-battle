package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.states.GameState
import com.template.states.PositionState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TurnInitiator(
        val gameName:String,
        val coordinate:String
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

//        val gameState = serviceHub.vaultService.queryBy(GameState::class.java).states.single { it.state.data.name == gameName }
//        val game = gameState.state.data
//
//        // Initiator flow logic goes here.
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val command = Command(GameContract.Commands.SetPos(), listOf(ourIdentity).map(Party::owningKey))
//
//        // Build output game state
//        val outputGameState: GameState
//        outputGameState = if (game.p1 == ourIdentity){
//            game.copy(p1SetPos = true, turn = game.p2)
//        } else {
//            game.copy(p2SetPos = true, turn = game.p1)
//        }
//
//        // Empty grid
//        val map = mutableMapOf<String, PositionState>()
//        for (x in 'A'..'I'){
//            for (y in 0..9){
//                val pos = "${x}${y}"
//                map[pos] = PositionState(ourIdentity, gameName, pos, false)
//            }
//        }
//
//        // Read ship locations and add to map
//        val regex = Regex.fromLiteral("([A-Ia-i])([0-8])([NESWnesw])")
//
//        for ((ship, size) in listOf(
//                Pair(aircraftCarrier, 5),
//                Pair(battleship, 4),
//                Pair(cruiser, 3),
//                Pair(destroyer1, 2),
//                Pair(destroyer2, 2),
//                Pair(submarine1, 1),
//                Pair(submarine2, 1)
//        )) {
//            val matchResult = regex.matchEntire(ship)!!
//
//            val col = matchResult.groups[0]!!.value[0].toUpperCase()
//            val row = Integer.parseInt(matchResult.groups[1]!!.value)
//            val direction = matchResult.groups[2]!!.value.toUpperCase()
//
//            for (i in 0 until size){
//                val posName: String = when (direction){
//                    "N" -> "$col${row - i}"
//                    "E" -> "${(col + i)}$row"
//                    "S" -> "$col${row + i}"
//                    "W" -> "${(col - i)}$row"
//                    else -> throw IllegalArgumentException("Direction needs to be one of N, E, S, W")
//                }
//
//                // Check position already exists
//                requireThat { "Ship positioned outside grid" using (map.containsKey(posName)) }
//
//                // Update the map
//                map[posName] = PositionState(ourIdentity, gameName, posName, true)
//            }
//        }
//
//        // Build up the transaction
//        val transactionBuilder = TransactionBuilder(notary)
//                .addCommand(command)
//                .addInputState(gameState)
//                .addOutputState(outputGameState, GameContract.ID)
//        for (p in map.values){
//            transactionBuilder.addOutputState(p, GameContract.ID)
//        }
//        transactionBuilder.verify(serviceHub)
//        val transaction = serviceHub.signInitialTransaction(transactionBuilder)
//
//        return subFlow(FinalityFlow(transaction, emptyList()))
        TODO()
    }
}
