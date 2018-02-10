package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object BidSchema

/**
 * An IOUState schema.
 */
object BidSchemaV1 : MappedSchema(
        schemaFamily = BidSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBid::class.java)) {
    @Entity
    @Table(name = "Bid_states")
    class PersistentBid(
            @Column(name = "admin")
            var adminName: String,

            @Column(name = "bidderrs")
            var bidderName: String,

            @Column(name = "bidvalue")
            var bidvalue: Int,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState()
}