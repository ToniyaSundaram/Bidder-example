package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.BidContract
import com.example.contract.BidContract.Companion.Bid_CONTRACT_ID
import com.example.state.BidState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step


/**
 * This flow allows (the [bidAdmin] and the [Bidders]) to come to an agreement about the Bid Created
 * within an [BidState].
 *
 * In our Project, the [Bidders] always accepts a valid Bid.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object BidFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val bidValue: Int,
                    val bidders: List<Party>) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Generating transaction based on new Bid.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                   GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION
            // Generate an unsigned transaction.
            val bidState = BidState(bidValue, serviceHub.myInfo.legalIdentities.first(), bidders)

            val txCommand = Command(BidContract.Commands.Create(), bidState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary).withItems(StateAndContract(bidState, Bid_CONTRACT_ID), txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            //stage 4
            progressTracker.currentStep = GATHERING_SIGS
            val fullySignedTx:SignedTransaction
            var arraySize = bidders.size



            if(arraySize==1){
                val otherPartyFlow = initiateFlow(bidders[0])
                fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))
            }else if(arraySize==2){
                val otherPartyFlow = initiateFlow(bidders[0])
                val otherPartyFlow1 = initiateFlow(bidders[1])
                fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow,otherPartyFlow1), GATHERING_SIGS.childProgressTracker()))

            }else {
                val otherPartyFlow = initiateFlow(bidders[0])
                val otherPartyFlow1 = initiateFlow(bidders[1])
                val otherPartyFlow2 = initiateFlow(bidders[2])
                fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow,otherPartyFlow1,otherPartyFlow2), GATHERING_SIGS.childProgressTracker()))
            }


            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
      }

    }

    @InitiatedBy(Initiator::class)
    class Bidders(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a Bid Creation." using (output is BidState)
                    val bid = output as BidState
                    "I won't accept Bid with a value less than 100." using (bid.bidValue >= 100)

                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}
