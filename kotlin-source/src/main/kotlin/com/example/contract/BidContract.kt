package com.example.contract


import com.example.state.BidState
import kotlinx.html.COMMAND
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.ServiceHub

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [BidState], which in turn encapsulates an [BidValue].
 *
 * For a new [Bid] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [Bid].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class BidContract : Contract {
    companion object {
        @JvmStatic
        val Bid_CONTRACT_ID = "com.example.contract.BidContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val i=0
        val command = tx.commands.requireSingleCommand<BidContract.Commands>()
        if(command.value is Commands.Create){
            requireThat {
                // Generic constraints around the IOU transaction.
                "No inputs should be consumed when Creating a Bid." using (tx.inputs.isEmpty())
                "Only one output state should be created." using (tx.outputs.size == 1)
                val out = tx.outputsOfType<BidState>().single()
                // IOU-specific constraints.
                "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
                "The Bid value must be non-negative." using (out.bidValue > 0)
            }
        }else if(command.value is Commands.Submit){
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<BidState>().single()
            // IOU-specific constraints.
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))
            "The Bid value must be non-negative." using (out.bidValue > 0)
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
        class Submit: Commands
    }
}
