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
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap

data class Params(val gameName: String, val coordinate: String)

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
        object FINDING : ProgressTracker.Step("Asking other player for coordinate.")
        object TRANSFERRING : ProgressTracker.Step("Finalising transfer.")
    }
    override val progressTracker = ProgressTracker(FINDING, TRANSFERRING)

    @Suspendable
    override fun call() : SignedTransaction {

        progressTracker.currentStep = FINDING

        val gameState = serviceHub.vaultService.queryBy(GameState::class.java).states.single { it.state.data.name == gameName }
        val game = gameState.state.data

        val otherPlayer = if (game.p1 == ourIdentity) game.p2 else game.p1

        val initiateFlow = initiateFlow(otherPlayer)
        val transaction = initiateFlow.sendAndReceive<SignedTransaction>(Params(gameName, coordinate)).unwrap{it}

        progressTracker.currentStep = TRANSFERRING

        transaction.verify(serviceHub)
        requireThat { "Returned position is not the one we requested" using ((transaction.tx.outputStates[0] as PositionState).positionName == coordinate) }
        serviceHub.addSignature(transaction)

        return subFlow(FinalityFlow(transaction, initiateFlow))
    }
}

@InitiatedBy(TurnInitiator::class)
class TurnResponder(val counterpartySession: FlowSession) :FlowLogic<Unit>() {

    companion object {
        object FINDING : ProgressTracker.Step("Finding position in vault.")
        object TRANSFERRING : ProgressTracker.Step("Transferring position to other player.")
    }
    override val progressTracker = ProgressTracker(FINDING, TRANSFERRING)

    @Suspendable
    override fun call() {
        val receiveAll = receiveAll<Params>(Params::class.java, listOf(counterpartySession))
        val (gameName, coordinate) = receiveAll[0].unwrap { it }

        progressTracker.currentStep = FINDING

        val gameState = serviceHub.vaultService.queryBy(GameState::class.java).states.single { it.state.data.name == gameName }
        val game = gameState.state.data

        val otherPlayer = if (game.p1 == ourIdentity) game.p2 else game.p1

        val gamePositionStates = serviceHub.vaultService.queryBy(PositionState::class.java).states.filter { it.state.data.gameName == gameName }
        val ourPositions = gamePositionStates.filter { it.state.data.originallyOwnedBy == ourIdentity }
        val desiredPos = ourPositions.single {it.state.data.positionName == coordinate}

        progressTracker.currentStep = TRANSFERRING

        val newPosition = desiredPos.state.data.copy(owner = otherPlayer)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(GameContract.Commands.Turn(), listOf(ourIdentity, otherPlayer).map(Party::owningKey))

        val transactionBuilder = TransactionBuilder(notary)
                .addInputState(desiredPos)
                .addOutputState(newPosition, GameContract.ID)
                .addCommand(command)
        transactionBuilder.verify(serviceHub)
        val transaction = serviceHub.signInitialTransaction(transactionBuilder)

        counterpartySession.send(transaction)
    }

}