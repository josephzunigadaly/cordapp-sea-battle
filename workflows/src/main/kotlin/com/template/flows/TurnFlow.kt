package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.states.GameState
import com.template.states.PositionState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@CordaSerializable
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

        val gameState = serviceHub.vaultService.queryBy(GameState::class.java, criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states.single { it.state.data.name == gameName }
        val game = gameState.state.data

        val otherPlayer = if (game.p1 == ourIdentity) game.p2 else game.p1

        val initiateFlow = initiateFlow(otherPlayer)

        val desiredPos = initiateFlow.sendAndReceive<StateAndRef<PositionState>>(Params(gameName, coordinate)).unwrap{it}

        progressTracker.currentStep = TRANSFERRING

        requireThat { "Returned position is not the one we requested" using (desiredPos.state.data.positionName == coordinate) }

        return subFlow(TurnInitiator2(desiredPos, gameState, game, otherPlayer))
    }
}

@InitiatingFlow
class TurnInitiator2(
        val desiredPos: StateAndRef<PositionState>,
        val gameState: StateAndRef<GameState>,
        val game: GameState,
        val otherPlayer: Party
) :FlowLogic<SignedTransaction>(){
    @Suspendable
    override fun call() : SignedTransaction {

        // Crate transaction to record move
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(GameContract.Commands.Turn(), listOf(ourIdentity, otherPlayer).map(Party::owningKey))

        val newPosition = desiredPos.state.data.copy(owner = ourIdentity)

        val transactionBuilder = TransactionBuilder(notary)
                .addCommand(command)
                .addInputState(gameState)
                .addOutputState(game.copy(turn = otherPlayer))
                .addInputState(desiredPos)
                .addOutputState(newPosition, GameContract.ID)
        transactionBuilder.verify(serviceHub)
        val transaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(initiateFlow(otherPlayer))
        val collectSignaturesFlow = subFlow(CollectSignaturesFlow(transaction, sessions))
        return subFlow(FinalityFlow(collectSignaturesFlow, sessions))
    }
}

@InitiatedBy(TurnInitiator2::class)
class TurnResponder2(val counterpartySession: FlowSession) :FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {        // Recieve collect signatures flow
        val txWeJustSigned = subFlow(object : SignTransactionFlow(counterpartySession){
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            }
        })
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
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
    override fun call(): Unit {
        val receiveAll = receiveAll<Params>(Params::class.java, listOf(counterpartySession))
        val (gameName, coordinate) = receiveAll[0].unwrap { it }

        progressTracker.currentStep = FINDING

        val gameState = serviceHub.vaultService.queryBy(GameState::class.java).states.single { it.state.data.name == gameName }
        val game = gameState.state.data

        val otherPlayer = if (game.p1 == ourIdentity) game.p2 else game.p1

        val gamePositionStates = serviceHub.vaultService.queryBy(PositionState::class.java).states.filter { it.state.data.gameName == gameName }
        val ourPositions = gamePositionStates.filter { it.state.data.originallyOwnedBy == ourIdentity }
        val desiredPos: StateAndRef<PositionState> = ourPositions.single {it.state.data.positionName == coordinate}

        progressTracker.currentStep = TRANSFERRING

        // Send position to the other player
        counterpartySession.send(desiredPos)
    }
}

