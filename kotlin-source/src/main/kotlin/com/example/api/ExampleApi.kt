package com.example.api

import com.example.flow.BidFlow
import com.example.flow.BidFlow.Initiator
import com.example.flow.SubmitBidFlow
import com.example.flow.SubmitBidFlow.Acceptor
import com.example.state.BidState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Controller", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }


    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)



    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all Bid states that exist in the node's vault.
     */
    @GET
    @Path("bids")
    @Produces(MediaType.APPLICATION_JSON)
    fun getBids() = rpcOps.vaultQueryBy<BidState>().states

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("create-bid")
    fun createBid(@QueryParam("bidValue") bidValue: Int, @QueryParam("bidderName") bidderName: List<String>): Response {
        if (bidValue <= 0 ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouValue' must be non-negative.\n").build()
        }

        val bidders = ArrayList<Party>()

        println("BIdders size"+ bidderName.size)


        for (i in bidderName.indices) {
            println("The bidderName is "+bidderName[i])
            if(bidderName[i].equals("BidderA")) {
                println("Inside BidderA ")
                val biddername = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("BidderA", "New York", "US")) ?:
                        return Response.status(BAD_REQUEST).entity("Party named $bidderName[i] cannot be found.\n").build()
                bidders.add(biddername)
            }else if(bidderName[i].equals("BidderB")) {
                println("Inside BidderB ")
                val biddername = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("BidderB", "Paris", "FR")) ?:
                        return Response.status(BAD_REQUEST).entity("Party named $bidderName[i] cannot be found.\n").build()
                bidders.add(biddername)
            }else if(bidderName[i].equals("BidderC")) {
                println("Inside BidderC ")
                val biddername = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("BidderC", "Paris", "FR")) ?:
                        return Response.status(BAD_REQUEST).entity("Party named $bidderName[i] cannot be found.\n").build()
                bidders.add(biddername)
            }else {
                return Response.status(BAD_REQUEST).entity("Party named $bidderName[i] cannot be found.\n").build()
            }

        }


        return try {

            if(myLegalName==CordaX500Name("Admin", "London", "GB")) {
                val flowHandle = rpcOps.startTrackedFlow(::Initiator, bidValue, bidders)
                flowHandle.progress.subscribe { println(">> $it") }

                // The line below blocks and waits for the future to resolve.
                val result = flowHandle.returnValue.getOrThrow()

                Response.status(CREATED).entity("Transaction id ${result.id} Successfully created a Bid\n").build()

            }else {
                return Response.status(BAD_REQUEST).entity("Only Admin is authorized to create a Bid  \n").build()
            }

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }


    @PUT
    @Path("submit-bid")
    fun submitBid(@QueryParam("bid_value") bidValue: Int): Response {

        if (bidValue <= 0 ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'bidValue' must be non-negative.\n").build()
        }
        val admin = rpcOps.wellKnownPartyFromX500Name(CordaX500Name("Admin", "London", "GB")) ?:
                return Response.status(BAD_REQUEST).entity("Admin node cannot be found.\n").build()

        return try {
            val flowHandle = rpcOps.startTrackedFlow(::Acceptor, bidValue,admin)
            flowHandle.progress.subscribe { println(">> $it") }

            // The line below blocks and waits for the future to resolve.
            val result = flowHandle.returnValue.getOrThrow()

            Response.status(CREATED).entity("Transaction id ${result.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

}


