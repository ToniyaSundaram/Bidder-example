package com.example.model

import java.util.*

class BidVO {

    var bidId: UUID = UUID.randomUUID()
    var  businessname =""
    var businessaddress =""
    var industrytype=""
    var industrysubtype=""
    var description=""
    var  insurancedsign=""
    var docslist=""
    var otherremarks=""
    var  winner=""
    var winningbidamount=0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BidVO

        if (bidId != other.bidId) return false
        if (businessname != other.businessname) return false
        if (businessaddress != other.businessaddress) return false
        if (industrytype != other.industrytype) return false
        if (industrysubtype != other.industrysubtype) return false
        if (description != other.description) return false
        if (insurancedsign != other.insurancedsign) return false
        if (docslist != other.docslist) return false
        if (otherremarks != other.otherremarks) return false
        if (winner != other.winner) return false
        if (winningbidamount != other.winningbidamount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bidId.hashCode()
        result = 31 * result + businessname.hashCode()
        result = 31 * result + businessaddress.hashCode()
        result = 31 * result + industrytype.hashCode()
        result = 31 * result + industrysubtype.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + insurancedsign.hashCode()
        result = 31 * result + docslist.hashCode()
        result = 31 * result + otherremarks.hashCode()
        result = 31 * result + winner.hashCode()
        result = 31 * result + winningbidamount
        return result
    }

    override fun toString(): String {
        return "BidVO(bidId=$bidId, businessname='$businessname', businessaddress='$businessaddress', industrytype='$industrytype', industrysubtype='$industrysubtype', description='$description', insurancedsign='$insurancedsign', docslist='$docslist', otherremarks='$otherremarks', winner='$winner', winningbidamount=$winningbidamount)"
    }
}