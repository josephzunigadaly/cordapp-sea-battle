package com.template.contracts

import com.template.states.CarState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class CarContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        val ID: String = CarContract::class.qualifiedName!!
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.

        val command = tx.commands.requireSingleCommand<Commands>().value

        when (command){
            is Commands.Issue -> requireThat {
                "There should be no input state" using (tx.inputs.isEmpty())
                "There should be only one output state" using (tx.outputs.count() == 1)
                "The output state must be of type CarState" using (tx.outputs[0].data is CarState)
                val carState = tx.outputs[0].data as CarState
                "The license plate number must be 7 characters long" using (carState.licensePlateNumber.length == 7)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
    }
}