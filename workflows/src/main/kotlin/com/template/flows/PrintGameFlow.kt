package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.GameState
import com.template.states.PositionState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class PrintGameInitiator(val gameName:String) : FlowLogic<Unit>() {

    override val progressTracker: ProgressTracker? = null

    @Suspendable
    override fun call() : Unit {

        val gameState = serviceHub.vaultService.queryBy(GameState::class.java).states.single { it.state.data.name == gameName }
        val game = gameState.state.data

        println("Game ${game.name}")
        println("-----------------")

        if (game.p1 == ourIdentity){
            println("We are player 1")
        } else{
            println("We are player 2")
        }

        if (game.turn == ourIdentity){
            println("It is our turn")
        } else {
            println("It is the other players turn")
        }

        if (game.p1SetPos) {
            println("Player 1 has set their positions")
        } else {
            println("Player 1 has not set their positions")
            if (game.p1 == ourIdentity){
                println("Run command ... to set position")
            }
        }

        if (game.p2SetPos) {
            println("Player 2 has set their positions")
        } else {
            println("Player 2 has not set their positions")
            if (game.p2 == ourIdentity){
                println("Run command ... to set position")
            }
        }

        if (!game.p1SetPos || !game.p2SetPos){
            // Exit early if either position is not set
            return
        }

        val gamePositionStates = serviceHub.vaultService.queryBy(PositionState::class.java).states.filter { it.state.data.gameName == gameName }
        val positions = gamePositionStates.map { it.state.data }
        val ourPositions = positions.filter { it.originallyOwnedBy == ourIdentity }
        val theirPositions = positions.filter { it.originallyOwnedBy != ourIdentity }

        // Print our grid
        println("Our grid")
        for (y in -1..9) {
            val stringBuilder = StringBuilder()
            if (y == -1) {
                stringBuilder.append("  ")
            } else {
                stringBuilder.append("$y ")
            }
            for (x in 'A'..'I') {
                if (y == -1) {
                    stringBuilder.append("$x")
                } else {
                    val posName = "${x}${y}"
                    val position = ourPositions.single { it.positionName == posName }
                    if (position.owner != ourIdentity){
                        // The other side have claimed this
                        // Did it contain anything?
                        if (position.containsShip) {
                            print('X')
                        } else{
                            print('/')
                        }
                    } else {
                        // Still ours
                        if (position.containsShip) {
                            print("*")
                        } else {
                            print(" ")
                        }
                    }
                }
            }
            println(stringBuilder.toString())
        }

        // Print their grid
        println("Their grid")
        for (y in -1..9) {
            val stringBuilder = StringBuilder()
            if (y == -1) {
                stringBuilder.append("  ")
            } else {
                stringBuilder.append("$y ")
            }
            for (x in 'A'..'I') {
                if (y == -1) {
                    stringBuilder.append("$x")
                } else {
                    val posName = "${x}${y}"
                    val position = theirPositions.singleOrNull { it.positionName == posName }
                    if (position == null){
                        // We have not tried this position
                        print(" ")
                    } else if (position.owner == ourIdentity){
                        // We have claimed this
                        // Did it contain anything?
                        if (position.containsShip) {
                            print('X')
                        } else {
                            print('/')
                        }
                    }
                }
            }
            println(stringBuilder.toString())
        }
    }
}
