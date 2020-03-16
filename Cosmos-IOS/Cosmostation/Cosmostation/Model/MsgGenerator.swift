//
//  MsgGenerator.swift
//  Cosmostation
//
//  Created by yongjoo on 09/04/2019.
//  Copyright © 2019 wannabit. All rights reserved.
//

import Foundation
import ed25519swift
import SwiftProtobuf
import CryptoSwift

class MsgGenerator {
    
    static func genDelegateMsg(_ fromAddress: String, _ toValAddress: String, _ amount: Coin, _ chain: ChainType) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        if (chain == ChainType.SUPPORT_CHAIN_COSMOS_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_TEST) {
            value.delegator_address = fromAddress
            value.validator_address = toValAddress
            let data = try? JSONEncoder().encode(amount)
            do {
                value.amount = try JSONDecoder().decode(AmountType.self, from:data!)
            } catch {
                print(error)
            }
            
            msg.type = COSMOS_MSG_TYPE_DELEGATE
            msg.value = value
            
        } else if (chain == ChainType.SUPPORT_CHAIN_IRIS_MAIN) {
            value.delegator_addr = fromAddress
            value.validator_addr = toValAddress
            value.delegation = amount
            
            msg.type = IRIS_MSG_TYPE_DELEGATE
            msg.value = value
        }
        return msg
    }
    
    static func genUndelegateMsg(_ fromAddress: String, _ toValAddress: String, _ amount: Coin, _ chain: ChainType) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        if (chain == ChainType.SUPPORT_CHAIN_COSMOS_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_TEST) {
            value.delegator_address = fromAddress
            value.validator_address = toValAddress
            let data = try? JSONEncoder().encode(amount)
            do {
                value.amount = try JSONDecoder().decode(AmountType.self, from:data!)
            } catch {
                print(error)
            }
            
            msg.type = COSMOS_MSG_TYPE_UNDELEGATE2
            msg.value = value
            
        } else if (chain == ChainType.SUPPORT_CHAIN_IRIS_MAIN) {
            value.delegator_addr = fromAddress
            value.validator_addr = toValAddress
            
            //TODO need cal bal to shares.
            value.shares_amount = amount.amount + ".0000000000"
            msg.type = IRIS_MSG_TYPE_UNDELEGATE;
            msg.value = value;
        }
        return msg
    }
    
    static func genGetRewardMsg(_ fromAddress: String, _ toValAddress: String, _ chain: ChainType) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        if (chain == ChainType.SUPPORT_CHAIN_COSMOS_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_TEST) {
            value.delegator_address = fromAddress
            value.validator_address = toValAddress
            
            msg.type = COSMOS_MSG_TYPE_WITHDRAW_DEL
            msg.value = value
            
        } else if (chain == ChainType.SUPPORT_CHAIN_IRIS_MAIN) {
            value.delegator_addr = fromAddress
            value.validator_addr = toValAddress
            
            msg.type = IRIS_MSG_TYPE_WITHDRAW
            msg.value = value
        }
        return msg
    }
    
    static func genIrisGetAllRewardMsg(_ fromAddress: String) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        
        value.delegator_addr = fromAddress
        msg.type = IRIS_MSG_TYPE_WITHDRAW_ALL
        msg.value = value
        
        return msg
    }
    
    static func genGetSendMsg(_ fromAddress: String, _ toAddress: String, _ amount: Array<Coin>, _ chain: ChainType) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        if (chain == ChainType.SUPPORT_CHAIN_COSMOS_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_TEST) {
            value.from_address = fromAddress
            value.to_address = toAddress
            let data = try? JSONEncoder().encode(amount)
            do {
                value.amount = try JSONDecoder().decode(AmountType.self, from:data!)
            } catch {
                print(error)
            }
            
            msg.type = COSMOS_MSG_TYPE_TRANSFER2
            msg.value = value
            
        } else if (chain == ChainType.SUPPORT_CHAIN_IRIS_MAIN) {
            let input = InOutPut.init(fromAddress, amount)
            var inputs: Array<InOutPut> = Array<InOutPut>()
            inputs.append(input)
            
            let output = InOutPut.init(toAddress, amount)
            var outputs: Array<InOutPut> = Array<InOutPut>()
            outputs.append(output)
            
            value.inputs = inputs
            value.outputs = outputs
            
            msg.type = IRIS_MSG_TYPE_TRANSFER
            msg.value = value
        }
        return msg
    }
    
    static func genGetRedelegateMsg(_ address: String, _ fromValAddress: String, _ toValAddress: String, _ amount: Coin, _ chain: ChainType) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        if (chain == ChainType.SUPPORT_CHAIN_COSMOS_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_TEST) {
            value.delegator_address = address
            value.validator_src_address = fromValAddress
            value.validator_dst_address = toValAddress
            let data = try? JSONEncoder().encode(amount)
            do {
                value.amount = try JSONDecoder().decode(AmountType.self, from:data!)
            } catch {
                print(error)
            }
            
            msg.type = COSMOS_MSG_TYPE_REDELEGATE2
            msg.value = value
            
        } else if (chain == ChainType.SUPPORT_CHAIN_IRIS_MAIN) {
            value.delegator_addr = address
            value.validator_src_addr = fromValAddress
            value.validator_dst_addr = toValAddress
            value.shares_amount = amount.amount + ".0000000000"
            
            msg.type = IRIS_MSG_TYPE_REDELEGATE;
            msg.value = value;
        }
        return msg
    }
    
    static func genIrisToSignRedeleMsg(_ address: String, _ fromValAddress: String, _ toValAddress: String, _ amount: Coin) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        value.delegator_addr = address
        value.validator_src_addr = fromValAddress
        value.validator_dst_addr = toValAddress
        value.shares = amount.amount + ".0000000000"
        msg.type = IRIS_MSG_TYPE_REDELEGATE;
        msg.value = value;
        return msg
    }
    
    static func genGetModifyRewardAddressMsg(_ requestAddress: String, _ newRewardAddress: String, _ chain: ChainType) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        if (chain == ChainType.SUPPORT_CHAIN_COSMOS_MAIN) {
            value.delegator_address = requestAddress
            value.withdraw_address = newRewardAddress
            
            msg.type = COSMOS_MSG_TYPE_WITHDRAW_MIDIFY
            msg.value = value
            
        } else if (chain == ChainType.SUPPORT_CHAIN_IRIS_MAIN) {
            value.delegator_addr = requestAddress
            value.withdraw_addr = newRewardAddress
            
            msg.type = IRIS_MSG_TYPE_WITHDRAW_MIDIFY
            msg.value = value
        }
        return msg
    }
    
    static func genGetVoteMsg(_ fromAddress: String, _ proposalId: String, _ opinion: String, _ chain: ChainType) -> Msg {
        var msg = Msg.init()
        var value = Msg.Value.init()
        if (chain == ChainType.SUPPORT_CHAIN_COSMOS_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_MAIN || chain == ChainType.SUPPORT_CHAIN_KAVA_TEST) {
            
        } else if (chain == ChainType.SUPPORT_CHAIN_IRIS_MAIN) {
            value.proposal_id = proposalId
            value.voter = fromAddress
            value.option = opinion
            
            msg.type = IRIS_MSG_TYPE_VOTE
            msg.value = value
            
        }
        return msg
    }
    
    
    
    static func genSignedTx(_ msgs: Array<Msg>, _ fee: Fee, _ memo: String, _ signatures: Array<Signature>) -> StdTx {
        let stdTx = StdTx.init()
        let value = StdTx.Value.init()
        
        value.msg = msgs
        value.fee = fee
        value.signatures = signatures
        value.memo = memo
        
        stdTx.type = COSMOS_AUTH_TYPE_STDTX
        stdTx.value = value
        
        return stdTx
    }
    
    static func getToSignMsg(_ chain: String, _ accountNum: String, _ sequenceNum: String, _ msgs: Array<Msg>, _ fee: Fee, _ memo: String) -> StdSignMsg {
        var stdSignedMsg = StdSignMsg.init()
        
        stdSignedMsg.chain_id = chain
        stdSignedMsg.account_number = accountNum
        stdSignedMsg.sequence = sequenceNum
        stdSignedMsg.msgs = msgs
        stdSignedMsg.fee = fee
        stdSignedMsg.memo = memo
        
        return stdSignedMsg
    }
    
    static func getIrisToSignMsg(_ chain: String, _ accountNum: String, _ sequenceNum: String, _ msgs: Array<Msg>, _ fee: Fee, _ memo: String) -> IrisStdSignMsg {
        var stdSignedMsg = IrisStdSignMsg.init()
        
        stdSignedMsg.chain_id = chain
        stdSignedMsg.account_number = accountNum
        stdSignedMsg.sequence = sequenceNum
        var msgValues: Array<Msg.Value> = Array<Msg.Value>()
        for msg in msgs {
            msgValues.append(msg.value)
        }
        stdSignedMsg.msgs = msgValues
        stdSignedMsg.fee = fee
        stdSignedMsg.memo = memo
        
        return stdSignedMsg
    }
    
    
    
    
    static func genIovSendTx(_ nonce:Int64, _ fromAddr:String, _ toAddr:String, _ sendCoins: Array<Coin>, _ fee:Fee,  _ memo:String, _ key:WKey.Ed25519Key) -> String {
        let interPart = WUtils.getQuotient(sendCoins[0].amount)
        let decimalPart = WUtils.getRemainder(sendCoins[0].amount).multiplying(byPowerOf10: 9)
        
        //Set send coin
        var sendCoin = Coin_Coin.init()
        if (interPart != NSDecimalNumber.zero) {
            sendCoin.whole = interPart.int64Value
        }
        if (decimalPart != NSDecimalNumber.zero) {
            sendCoin.fractional = decimalPart.int64Value
        }
        sendCoin.ticker = IOV_MAIN_DENOM
        
        //Set fee
        var sendFee = Coin_Coin.init()
        sendFee.fractional = WUtils.getQuotient(GAS_FEE_IOV_TRANSFER).multiplying(byPowerOf10: 9).int64Value
        sendFee.ticker = IOV_MAIN_DENOM
        
        //Set FeeInfo
        var feeInfo = Cash_FeeInfo.init()
        feeInfo.payer = WKey.getIovDatafromDpAddress(fromAddr)!
        feeInfo.fees = sendFee
        
        //Set MetaData
        var metaData = Weave_Metadata.init()
        metaData.schema = 1
        
        var sendMsg = Cash_SendMsg.init()
        sendMsg.source = WKey.getIovDatafromDpAddress(fromAddr)!
        sendMsg.destination = WKey.getIovDatafromDpAddress(toAddr)!
        sendMsg.amount = sendCoin
        sendMsg.metadata = metaData
        sendMsg.memo = memo
        
        var sendTx = Bnsd_Tx.init()
        sendTx.cashSendMsg = sendMsg
        sendTx.fees = feeInfo
        
        let inSig = getIovInSig(sendTx, nonce)
        print("inSig ", bytesConvertToHexstring(byte: inSig))
        let midSig = Digest.sha512(inSig)
        print("midSig ", bytesConvertToHexstring(byte: midSig))
        let genSig = Ed25519.sign(message: midSig, secretKey: key.key)
        print("genSig ", bytesConvertToHexstring(byte: genSig))
        return ""
    }
    
    static func getIovInSig(_ tx:Bnsd_Tx, _ nonce:Int64) -> [UInt8] {
        let chainB: [UInt8] = Array("iov-mainnet".utf8)
        let versionB : [UInt8] = [0, 0xCA, 0xFE, 0]
        let nonceB = byteArray(from: nonce)
        let chainSize:UInt8 = UInt8(chainB.count)
        let chainLenB = byteArray(from: chainSize)
        let txB = try? tx.serializedData().bytes
        return versionB + chainLenB + chainB + nonceB + txB!
    }
    
    static func byteArray<T>(from value: T) -> [UInt8] where T: FixedWidthInteger {
        withUnsafeBytes(of: value.bigEndian, Array.init)
    }
    
    static func bytesConvertToHexstring(byte : [UInt8]) -> String {
        var string = ""

        for val in byte {
            //getBytes(&byte, range: NSMakeRange(i, 1))
            string = string + String(format: "%02X", val)
        }

        return string
    }
    
}


//protocol UIntToBytesConvertable {
//    var toBytes: [Byte] { get }
//}
//
//extension UIntToBytesConvertable {
//    func toByteArr<T: Integer>(endian: T, count: Int) -> [Byte] {
//        var _endian = endian
//        let bytePtr = withUnsafePointer(to: &_endian) {
//            $0.withMemoryRebound(to: Byte.self, capacity: count) {
//                UnsafeBufferPointer(start: $0, count: count)
//            }
//        }
//        return [Byte](bytePtr)
//    }
//}
//
//extension UInt16: UIntToBytesConvertable {
//    var toBytes: [Byte] {
//        return toByteArr(endian: self.littleEndian,
//                         count: MemoryLayout<UInt16>.size)
//    }
//}
//
//extension UInt32: UIntToBytesConvertable {
//    var toBytes: [Byte] {
//        return toByteArr(endian: self.littleEndian,
//                         count: MemoryLayout<UInt32>.size)
//    }
//}
//
//extension UInt64: UIntToBytesConvertable {
//    var toBytes: [Byte] {
//        return toByteArr(endian: self.littleEndian,
//                         count: MemoryLayout<UInt64>.size)
//    }
//}
