package com.example.state

import com.example.schema.BidSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording Bidder agreements between Admin and Bidders.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the value of the Bid.
 * @param Admin is the person who to create a Bid.
 * @param bidders is the list of Bidders who have access to the created Bid.
 */
data class BidState(val bidValue: Int,
                    val bidAdmin: Party,
                    val bidders: List<Party>,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(bidAdmin)+bidders

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BidSchemaV1  -> BidSchemaV1.PersistentIOU(
                    this.bidAdmin.name.toString(),
                    this.bidders.toString(),
                    this.bidValue,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BidSchemaV1)
}
