package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CarContract
import com.template.states.CarState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
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
class CarIssueInitiator(
        val owningBank: Party,
        val holdingDealer: Party,
        val manufacturer: Party,
        val vin:String,
        val licensePlateNumber: String,
        val make: String,
        val model: String,
        val dealershipLocation: String
) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction {
        // Initiator flow logic goes here.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(CarContract.Commands.Issue(), listOf(owningBank, holdingDealer, manufacturer).map(Party::owningKey))
        val carState = CarState(owningBank, holdingDealer, manufacturer, vin, licensePlateNumber, make, model, dealershipLocation, UniqueIdentifier())

        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(carState, CarContract.ID)
                .addCommand(command)
        transactionBuilder.verify(serviceHub)
        val transaction = serviceHub.signInitialTransaction(transactionBuilder)

        val sessions = (carState.participants - ourIdentity)
                .map { it as Party }
                .map { initiateFlow(it) }
        val collectSignaturesFlow = subFlow(CollectSignaturesFlow(transaction, sessions))

        return subFlow(FinalityFlow(collectSignaturesFlow, sessions))
    }
}

@InitiatedBy(CarIssueInitiator::class)
class CarIssueResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call() : SignedTransaction{
        // Responder flow logic goes here.
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession){
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "The output must be a CarState" using (output is CarState)
            }
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
