package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.GameContract
import com.template.states.GameState
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
class NewGameInitiator(
        val name:String,
        val p2: Party
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
        // Initiator flow logic goes here.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(GameContract.Commands.New(), listOf(ourIdentity, p2).map(Party::owningKey))
        val gState = GameState(ourIdentity, p2, name)

        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(gState, GameContract.ID)
                .addCommand(command)
        transactionBuilder.verify(serviceHub)
        val transaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = listOf(initiateFlow(p2))
        val collectSignaturesFlow: SignedTransaction = subFlow(CollectSignaturesFlow(transaction, sessions))

        return subFlow(FinalityFlow(collectSignaturesFlow, sessions))
    }
}

@InitiatedBy(NewGameInitiator::class)
class NewGameResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction{
        // Responder flow logic goes here.
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession){
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "The output must be a ${GameState::class.simpleName}" using (output is GameState)
            }
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
