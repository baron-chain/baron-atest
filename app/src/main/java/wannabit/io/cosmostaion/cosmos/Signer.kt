package wannabit.io.cosmostaion.cosmos

import com.cosmos.auth.v1beta1.AuthProto
import com.cosmos.auth.v1beta1.QueryProto.QueryAccountResponse
import com.cosmos.bank.v1beta1.TxProto.MsgSend
import com.cosmos.base.v1beta1.CoinProto
import com.cosmos.crypto.secp256k1.KeysProto.PubKey
import com.cosmos.distribution.v1beta1.DistributionProto.DelegationDelegatorReward
import com.cosmos.distribution.v1beta1.TxProto.MsgSetWithdrawAddress
import com.cosmos.distribution.v1beta1.TxProto.MsgWithdrawDelegatorReward
import com.cosmos.gov.v1beta1.TxProto
import com.cosmos.staking.v1beta1.TxProto.MsgBeginRedelegate
import com.cosmos.staking.v1beta1.TxProto.MsgCancelUnbondingDelegation
import com.cosmos.staking.v1beta1.TxProto.MsgDelegate
import com.cosmos.staking.v1beta1.TxProto.MsgUndelegate
import com.cosmos.tx.signing.v1beta1.SigningProto
import com.cosmos.tx.v1beta1.ServiceProto.BroadcastMode
import com.cosmos.tx.v1beta1.ServiceProto.BroadcastTxRequest
import com.cosmos.tx.v1beta1.ServiceProto.SimulateRequest
import com.cosmos.tx.v1beta1.TxProto.AuthInfo
import com.cosmos.tx.v1beta1.TxProto.Fee
import com.cosmos.tx.v1beta1.TxProto.ModeInfo
import com.cosmos.tx.v1beta1.TxProto.SignDoc
import com.cosmos.tx.v1beta1.TxProto.SignerInfo
import com.cosmos.tx.v1beta1.TxProto.Tx
import com.cosmos.tx.v1beta1.TxProto.TxBody
import com.cosmos.tx.v1beta1.TxProto.TxRaw
import com.cosmos.vesting.v1beta1.VestingProto
import com.cosmwasm.wasm.v1.TxProto.MsgExecuteContract
import com.ethermint.crypto.v1.ethsecp256k1.KeysProto
import com.ethermint.types.v1.AccountProto
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.ibc.applications.transfer.v1.TxProto.MsgTransfer
import com.kava.bep3.v1beta1.TxProto.MsgCreateAtomicSwap
import com.kava.cdp.v1beta1.TxProto.MsgCreateCDP
import com.kava.cdp.v1beta1.TxProto.MsgDeposit
import com.kava.cdp.v1beta1.TxProto.MsgDrawDebt
import com.kava.cdp.v1beta1.TxProto.MsgRepayDebt
import com.kava.cdp.v1beta1.TxProto.MsgWithdraw
import com.kava.hard.v1beta1.TxProto.MsgBorrow
import com.kava.hard.v1beta1.TxProto.MsgRepay
import com.kava.incentive.v1beta1.QueryProto.QueryRewardsResponse
import com.kava.incentive.v1beta1.TxProto.MsgClaimDelegatorReward
import com.kava.incentive.v1beta1.TxProto.MsgClaimEarnReward
import com.kava.incentive.v1beta1.TxProto.MsgClaimHardReward
import com.kava.incentive.v1beta1.TxProto.MsgClaimSwapReward
import com.kava.incentive.v1beta1.TxProto.MsgClaimUSDXMintingReward
import com.kava.incentive.v1beta1.TxProto.Selection
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import wannabit.io.cosmostaion.chain.CosmosLine
import wannabit.io.cosmostaion.chain.cosmosClass.ChainInjective
import wannabit.io.cosmostaion.common.ByteUtils.integerToBytes
import wannabit.io.cosmostaion.common.delegatorRewardDenoms
import wannabit.io.cosmostaion.common.earnRewardDenoms
import wannabit.io.cosmostaion.common.hardRewardDenoms
import wannabit.io.cosmostaion.common.hasUsdxMinting
import wannabit.io.cosmostaion.common.swapRewardDenoms
import java.math.BigInteger
import java.math.RoundingMode

object Signer {

    fun genSendBroadcast(
        auth: QueryAccountResponse?,
        msgSend: MsgSend?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, sendMsg(msgSend), fee, memo, selectedChain)
    }

    fun genSendSimulate(
        auth: QueryAccountResponse?,
        msgSend: MsgSend?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, sendMsg(msgSend), fee, memo)
    }

    private fun sendMsg(msgSend: MsgSend?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(msgSend?.toByteString()).build()
        )
        return msgAnys
    }

    fun genIbcSendBroadcast(
        auth: QueryAccountResponse?,
        msgTransfer: MsgTransfer?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, ibcSendMsg(msgTransfer), fee, memo, selectedChain)
    }

    fun genIbcSendSimulate(
        auth: QueryAccountResponse?,
        msgTransfer: MsgTransfer?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, ibcSendMsg(msgTransfer), fee, memo)
    }

    private fun ibcSendMsg(msgTransfer: MsgTransfer?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/ibc.applications.transfer.v1.MsgTransfer")
                .setValue(msgTransfer?.toByteString()).build()
        )
        return msgAnys
    }

    fun genWasmBroadcast(
        auth: QueryAccountResponse?,
        msgWasms: MutableList<MsgExecuteContract?>?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, wasmMsg(msgWasms), fee, memo, selectedChain)
    }

    fun genWasmSimulate(
        auth: QueryAccountResponse?,
        msgWasms: MutableList<MsgExecuteContract?>?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, wasmMsg(msgWasms), fee, memo)
    }

    private fun wasmMsg(msgWasms: MutableList<MsgExecuteContract?>?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgWasms?.forEach { msgWasm ->
            msgAnys.add(
                Any.newBuilder().setTypeUrl("/cosmwasm.wasm.v1.MsgExecuteContract")
                    .setValue(msgWasm?.toByteString()).build()
            )
        }
        return msgAnys
    }

    fun genDelegateBroadcast(
        auth: QueryAccountResponse?,
        msgDelegate: MsgDelegate?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, delegateMsg(msgDelegate), fee, memo, selectedChain)
    }

    fun genDelegateSimulate(
        auth: QueryAccountResponse?,
        msgDelegate: MsgDelegate?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, delegateMsg(msgDelegate), fee, memo)
    }

    private fun delegateMsg(msgDelegate: MsgDelegate?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/cosmos.staking.v1beta1.MsgDelegate")
                .setValue(msgDelegate?.toByteString()).build()
        )
        return msgAnys
    }

    fun genUnDelegateBroadcast(
        auth: QueryAccountResponse?,
        msgUnDelegate: MsgUndelegate?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, unDelegateMsg(msgUnDelegate), fee, memo, selectedChain)
    }

    fun genUnDelegateSimulate(
        auth: QueryAccountResponse?,
        msgUnDelegate: MsgUndelegate?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, unDelegateMsg(msgUnDelegate), fee, memo)
    }

    private fun unDelegateMsg(msgUndelegate: MsgUndelegate?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/cosmos.staking.v1beta1.MsgUndelegate")
                .setValue(msgUndelegate?.toByteString()).build()
        )
        return msgAnys
    }

    fun genReDelegateBroadcast(
        auth: QueryAccountResponse?,
        msgReDelegate: MsgBeginRedelegate?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, reDelegateMsg(msgReDelegate), fee, memo, selectedChain)
    }

    fun genReDelegateSimulate(
        auth: QueryAccountResponse?,
        msgReDelegate: MsgBeginRedelegate?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, reDelegateMsg(msgReDelegate), fee, memo)
    }

    private fun reDelegateMsg(msgReDelegate: MsgBeginRedelegate?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/cosmos.staking.v1beta1.MsgBeginRedelegate")
                .setValue(msgReDelegate?.toByteString()).build()
        )
        return msgAnys
    }

    fun genCancelUnbondingBroadcast(
        auth: QueryAccountResponse?,
        msgCancelUnbondingDelegation: MsgCancelUnbondingDelegation?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, cancelUnbondingMsg(msgCancelUnbondingDelegation), fee, memo, selectedChain)
    }

    fun genCancelUnbondingSimulate(
        auth: QueryAccountResponse?,
        msgCancelUnbondingDelegation: MsgCancelUnbondingDelegation?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, cancelUnbondingMsg(msgCancelUnbondingDelegation), fee, memo)
    }

    private fun cancelUnbondingMsg(msgCancelUnbondingDelegation: MsgCancelUnbondingDelegation?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/cosmos.staking.v1beta1.MsgCancelUnbondingDelegation")
                .setValue(msgCancelUnbondingDelegation?.toByteString()).build()
        )
        return msgAnys
    }

    fun genClaimRewardsBroadcast(
        auth: QueryAccountResponse?,
        rewards: MutableList<DelegationDelegatorReward?>,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, claimStakingRewardMsg(auth, rewards), fee, memo, selectedChain)
    }

    fun genClaimRewardsSimulate(
        auth: QueryAccountResponse?,
        rewards: MutableList<DelegationDelegatorReward?>,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, claimStakingRewardMsg(auth, rewards), fee, memo)
    }

    private fun claimStakingRewardMsg(auth: QueryAccountResponse?, rewards: MutableList<DelegationDelegatorReward?>): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        rewards.forEach { reward ->
            val claimMsg = MsgWithdrawDelegatorReward.newBuilder()
                .setDelegatorAddress(parseAuthGrpc(auth).first)
                .setValidatorAddress(reward?.validatorAddress).build()
            val anyMsg = Any.newBuilder().setTypeUrl("/cosmos.distribution.v1beta1.MsgWithdrawDelegatorReward")
                .setValue(claimMsg.toByteString()).build()
            msgAnys.add(anyMsg)
        }
        return msgAnys
    }

    fun genCompoundingBroadcast(
        auth: QueryAccountResponse?,
        rewards: MutableList<DelegationDelegatorReward?>,
        stakingDenom: String?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, compoundingMsg(auth, rewards, stakingDenom), fee, memo, selectedChain)
    }

    fun genCompoundingSimulate(
        auth: QueryAccountResponse?,
        rewards: MutableList<DelegationDelegatorReward?>,
        stakingDenom: String?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, compoundingMsg(auth, rewards, stakingDenom), fee, memo)
    }

    private fun compoundingMsg(auth: QueryAccountResponse?, rewards: MutableList<DelegationDelegatorReward?>, stakingDenom: String?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        rewards.forEach { reward ->
            val claimMsg = MsgWithdrawDelegatorReward.newBuilder()
                .setDelegatorAddress(parseAuthGrpc(auth).first)
                .setValidatorAddress(reward?.validatorAddress).build()
            val anyMsg = Any.newBuilder().setTypeUrl("/cosmos.distribution.v1beta1.MsgWithdrawDelegatorReward")
                .setValue(claimMsg.toByteString()).build()
            msgAnys.add(anyMsg)

            val rewardCoin = reward?.rewardList?.firstOrNull { it.denom == stakingDenom }
            val delegateCoin = CoinProto.Coin.newBuilder().setDenom(rewardCoin?.denom).setAmount(rewardCoin?.amount?.toBigDecimal()?.movePointLeft(18)?.setScale(0, RoundingMode.DOWN)?.toPlainString())
            val delegateMsg = MsgDelegate.newBuilder().setDelegatorAddress(parseAuthGrpc(auth).first).setValidatorAddress(reward?.validatorAddress).setAmount(delegateCoin).build()
            val deleAnyMsg = Any.newBuilder().setTypeUrl("/cosmos.staking.v1beta1.MsgDelegate")
                .setValue(delegateMsg.toByteString()).build()
            msgAnys.add(deleAnyMsg)
        }
        return msgAnys
    }

    fun genChangeRewardAddressBroadcast(
        auth: QueryAccountResponse?,
        msgSetWithdrawAddress: MsgSetWithdrawAddress?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, changeRewardAddress(msgSetWithdrawAddress), fee, memo, selectedChain)
    }

    fun genChangeRewardAddressSimulate(
        auth: QueryAccountResponse?,
        msgSetWithdrawAddress: MsgSetWithdrawAddress?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, changeRewardAddress(msgSetWithdrawAddress), fee, memo)
    }

    private fun changeRewardAddress(msgSetWithdrawAddress: MsgSetWithdrawAddress?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/cosmos.distribution.v1beta1.MsgSetWithdrawAddress")
                .setValue(msgSetWithdrawAddress?.toByteString()).build()
        )
        return msgAnys
    }

    fun genVoteBroadcast(
        auth: QueryAccountResponse?,
        msgVotes: MutableList<TxProto.MsgVote?>?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, voteMsg(msgVotes), fee, memo, selectedChain)
    }

    fun genVoteSimulate(
        auth: QueryAccountResponse?,
        msgVotes: MutableList<TxProto.MsgVote?>?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, voteMsg(msgVotes), fee, memo)
    }

    private fun voteMsg(msgVotes: MutableList<TxProto.MsgVote?>?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgVotes?.forEach { msgVote ->
            val anyMsg = Any.newBuilder().setTypeUrl("/cosmos.gov.v1beta1.MsgVote")
                .setValue(msgVote?.toByteString()).build()
            msgAnys.add(anyMsg)
        }
        return msgAnys
    }

    fun genClaimIncentiveBroadcast(
        auth: QueryAccountResponse?,
        incentive: QueryRewardsResponse,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, claimIncentiveMsg(auth, incentive), fee, memo, selectedChain)
    }

    fun genClaimIncentiveSimulate(
        auth: QueryAccountResponse?,
        incentive: QueryRewardsResponse,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, claimIncentiveMsg(auth, incentive), fee, memo)
    }

    private fun claimIncentiveMsg(auth: QueryAccountResponse?, incentive: QueryRewardsResponse): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()

        if (incentive.hasUsdxMinting()) {
            val mintMsg = MsgClaimUSDXMintingReward.newBuilder()
                .setSender(parseAuthGrpc(auth).first)
                .setMultiplierName("large")
                .build()
            val anyMsg = Any.newBuilder().setTypeUrl("/kava.incentive.v1beta1.MsgClaimUSDXMintingReward")
                .setValue(mintMsg.toByteString()).build()
            msgAnys.add(anyMsg)
        }

        if (incentive.hardRewardDenoms().size > 0) {
            val denomToClaims: MutableList<Selection> = mutableListOf()
            incentive.hardRewardDenoms().forEach { denom ->
                denomToClaims.add(Selection.newBuilder().setDenom(denom).setMultiplierName("large").build())
            }
            val hardMsg = MsgClaimHardReward.newBuilder()
                .setSender(parseAuthGrpc(auth).first)
                .addAllDenomsToClaim(denomToClaims)
                .build()
            val anyMsg = Any.newBuilder().setTypeUrl("/kava.incentive.v1beta1.MsgClaimHardReward")
                .setValue(hardMsg.toByteString()).build()
            msgAnys.add(anyMsg)
        }

        if (incentive.delegatorRewardDenoms().size > 0) {
            val denomToClaims: MutableList<Selection> = mutableListOf()
            incentive.delegatorRewardDenoms().forEach { denom ->
                denomToClaims.add(Selection.newBuilder().setDenom(denom).setMultiplierName("large").build())
            }
            val delegateMsg = MsgClaimDelegatorReward.newBuilder()
                .setSender(parseAuthGrpc(auth).first)
                .addAllDenomsToClaim(denomToClaims)
                .build()
            val anyMsg = Any.newBuilder().setTypeUrl("/kava.incentive.v1beta1.MsgClaimDelegatorReward")
                .setValue(delegateMsg.toByteString()).build()
            msgAnys.add(anyMsg)
        }

        if (incentive.swapRewardDenoms().size > 0) {
            val denomToClaims: MutableList<Selection> = mutableListOf()
            incentive.swapRewardDenoms().forEach { denom ->
                denomToClaims.add(Selection.newBuilder().setDenom(denom).setMultiplierName("large").build())
            }
            val swapMsg = MsgClaimSwapReward.newBuilder()
                .setSender(parseAuthGrpc(auth).first)
                .addAllDenomsToClaim(denomToClaims)
                .build()
            val anyMsg = Any.newBuilder().setTypeUrl("/kava.incentive.v1beta1.MsgClaimSwapReward")
                .setValue(swapMsg.toByteString()).build()
            msgAnys.add(anyMsg)
        }

        if (incentive.earnRewardDenoms().size > 0) {
            val denomToClaims: MutableList<Selection> = mutableListOf()
            incentive.earnRewardDenoms().forEach { denom ->
                denomToClaims.add(Selection.newBuilder().setDenom(denom).setMultiplierName("large").build())
            }
            val earnMsg = MsgClaimEarnReward.newBuilder()
                .setSender(parseAuthGrpc(auth).first)
                .addAllDenomsToClaim(denomToClaims)
                .build()
            val anyMsg = Any.newBuilder().setTypeUrl("/kava.incentive.v1beta1.MsgClaimEarnReward")
                .setValue(earnMsg.toByteString()).build()
            msgAnys.add(anyMsg)
        }
        return msgAnys
    }

    fun genMintCreateBroadcast(
        auth: QueryAccountResponse?,
        msgCreateMint: MsgCreateCDP?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, mintCreateMsg(msgCreateMint), fee, memo, selectedChain)
    }

    fun genMintCreateSimulate(
        auth: QueryAccountResponse?,
        msgCreateMint: MsgCreateCDP?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, mintCreateMsg(msgCreateMint), fee, memo)
    }

    private fun mintCreateMsg(msgCreateMint: MsgCreateCDP?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.cdp.v1beta1.MsgCreateCDP")
                .setValue(msgCreateMint?.toByteString()).build()
        )
        return msgAnys
    }

    fun genMintDepositBroadcast(
        auth: QueryAccountResponse?,
        msgDeposit: MsgDeposit?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, mintDepositMsg(msgDeposit), fee, memo, selectedChain)
    }

    fun genMintDepositSimulate(
        auth: QueryAccountResponse?,
        msgDeposit: MsgDeposit?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, mintDepositMsg(msgDeposit), fee, memo)
    }

    private fun mintDepositMsg(msgDeposit: MsgDeposit?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.cdp.v1beta1.MsgDeposit")
                .setValue(msgDeposit?.toByteString()).build()
        )
        return msgAnys
    }

    fun genMintWithdrawBroadcast(
        auth: QueryAccountResponse?,
        msgWithdraw: MsgWithdraw?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, mintWithdrawMsg(msgWithdraw), fee, memo, selectedChain)
    }

    fun genMintWithdrawSimulate(
        auth: QueryAccountResponse?,
        msgWithdraw: MsgWithdraw?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, mintWithdrawMsg(msgWithdraw), fee, memo)
    }

    private fun mintWithdrawMsg(msgWithdraw: MsgWithdraw?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.cdp.v1beta1.MsgWithdraw")
                .setValue(msgWithdraw?.toByteString()).build()
        )
        return msgAnys
    }

    fun genMintBorrowBroadcast(
        auth: QueryAccountResponse?,
        msgDrawDebt: MsgDrawDebt?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, mintBorrowMsg(msgDrawDebt), fee, memo, selectedChain)
    }

    fun genMintBorrowSimulate(
        auth: QueryAccountResponse?,
        msgDrawDebt: MsgDrawDebt?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, mintBorrowMsg(msgDrawDebt), fee, memo)
    }

    private fun mintBorrowMsg(msgDrawDebt: MsgDrawDebt?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.cdp.v1beta1.MsgDrawDebt")
                .setValue(msgDrawDebt?.toByteString()).build()
        )
        return msgAnys
    }

    fun genMintRepayBroadcast(
        auth: QueryAccountResponse?,
        msgRepayDebt: MsgRepayDebt?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, mintRepayMsg(msgRepayDebt), fee, memo, selectedChain)
    }

    fun genMintRepaySimulate(
        auth: QueryAccountResponse?,
        msgRepayDebt: MsgRepayDebt?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, mintRepayMsg(msgRepayDebt), fee, memo)
    }

    private fun mintRepayMsg(msgRepayDebt: MsgRepayDebt?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.cdp.v1beta1.MsgRepayDebt")
                .setValue(msgRepayDebt?.toByteString()).build()
        )
        return msgAnys
    }

    fun genLendDepositBroadcast(
        auth: QueryAccountResponse?,
        msgDeposit: com.kava.hard.v1beta1.TxProto.MsgDeposit?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, lendDepositMsg(msgDeposit), fee, memo, selectedChain)
    }

    fun genLendDepositSimulate(
        auth: QueryAccountResponse?,
        msgDeposit: com.kava.hard.v1beta1.TxProto.MsgDeposit?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, lendDepositMsg(msgDeposit), fee, memo)
    }

    private fun lendDepositMsg(msgDeposit: com.kava.hard.v1beta1.TxProto.MsgDeposit?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.hard.v1beta1.MsgDeposit")
                .setValue(msgDeposit?.toByteString()).build()
        )
        return msgAnys
    }

    fun genLendWithdrawBroadcast(
        auth: QueryAccountResponse?,
        msgWithdraw: com.kava.hard.v1beta1.TxProto.MsgWithdraw?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, lendWithdrawMsg(msgWithdraw), fee, memo, selectedChain)
    }

    fun genLendWithdrawSimulate(
        auth: QueryAccountResponse?,
        msgWithdraw: com.kava.hard.v1beta1.TxProto.MsgWithdraw?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, lendWithdrawMsg(msgWithdraw), fee, memo)
    }

    private fun lendWithdrawMsg(msgWithdraw: com.kava.hard.v1beta1.TxProto.MsgWithdraw?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.hard.v1beta1.MsgWithdraw")
                .setValue(msgWithdraw?.toByteString()).build()
        )
        return msgAnys
    }

    fun genLendBorrowBroadcast(
        auth: QueryAccountResponse?,
        msgBorrow: MsgBorrow?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, lendBorrowMsg(msgBorrow), fee, memo, selectedChain)
    }

    fun genLendBorrowSimulate(
        auth: QueryAccountResponse?,
        msgBorrow: MsgBorrow?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, lendBorrowMsg(msgBorrow), fee, memo)
    }

    private fun lendBorrowMsg(msgBorrow: MsgBorrow?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.hard.v1beta1.MsgBorrow")
                .setValue(msgBorrow?.toByteString()).build()
        )
        return msgAnys
    }

    fun genLendRepayBroadcast(
        auth: QueryAccountResponse?,
        msgRepay: MsgRepay?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, lendRepayMsg(msgRepay), fee, memo, selectedChain)
    }

    fun genLendRepaySimulate(
        auth: QueryAccountResponse?,
        msgRepay: MsgRepay?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, lendRepayMsg(msgRepay), fee, memo)
    }

    private fun lendRepayMsg(msgRepay: MsgRepay?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.hard.v1beta1.MsgRepay")
                .setValue(msgRepay?.toByteString()).build()
        )
        return msgAnys
    }

    fun genPoolDepositBroadcast(
        auth: QueryAccountResponse?,
        msgDeposit: com.kava.swap.v1beta1.TxProto.MsgDeposit?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, poolDepositMsg(msgDeposit), fee, memo, selectedChain)
    }

    fun genPoolDepositSimulate(
        auth: QueryAccountResponse?,
        msgDeposit: com.kava.swap.v1beta1.TxProto.MsgDeposit?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, poolDepositMsg(msgDeposit), fee, memo)
    }

    private fun poolDepositMsg(msgDeposit: com.kava.swap.v1beta1.TxProto.MsgDeposit?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.swap.v1beta1.MsgDeposit")
                .setValue(msgDeposit?.toByteString()).build()
        )
        return msgAnys
    }

    fun genPoolWithdrawBroadcast(
        auth: QueryAccountResponse?,
        msgWithdraw: com.kava.swap.v1beta1.TxProto.MsgWithdraw?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, poolWithdrawMsg(msgWithdraw), fee, memo, selectedChain)
    }

    fun genPoolWithdrawSimulate(
        auth: QueryAccountResponse?,
        msgWithdraw: com.kava.swap.v1beta1.TxProto.MsgWithdraw?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        return signSimulTx(auth, poolWithdrawMsg(msgWithdraw), fee, memo)
    }

    private fun poolWithdrawMsg(msgWithdraw: com.kava.swap.v1beta1.TxProto.MsgWithdraw?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.swap.v1beta1.MsgWithdraw")
                .setValue(msgWithdraw?.toByteString()).build()
        )
        return msgAnys
    }

    fun genCreateSwapBroadcast(
        auth: QueryAccountResponse?,
        msgCreateAtomicSwap: MsgCreateAtomicSwap?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        return signBroadcastTx(auth, createSwapMsg(msgCreateAtomicSwap), fee, memo, selectedChain)
    }

    private fun createSwapMsg(msgCreateAtomicSwap: MsgCreateAtomicSwap?): MutableList<Any> {
        val msgAnys: MutableList<Any> = mutableListOf()
        msgAnys.add(
            Any.newBuilder().setTypeUrl("/kava.bep3.v1beta1.MsgCreateAtomicSwap")
                .setValue(msgCreateAtomicSwap?.toByteString()).build()
        )
        return msgAnys
    }

    private fun generateGrpcPubKeyFromPriv(line: CosmosLine?, privateKey: String): Any {
        val ecKey = ECKey.fromPrivate(BigInteger(privateKey, 16))
        return if (line?.evmCompatible == true) {
            val pubKey = KeysProto.PubKey.newBuilder().setKey(ByteString.copyFrom(ecKey.pubKey)).build()
            Any.newBuilder().setTypeUrl("/ethermint.crypto.v1.ethsecp256k1.PubKey")
                .setValue(pubKey.toByteString()).build()
        } else if (line is ChainInjective){
            val pubKey = com.injective.crypto.v1beta1.ethsecp256k1.KeysProto.PubKey.newBuilder().setKey(ByteString.copyFrom(ecKey.pubKey)).build()
            Any.newBuilder().setTypeUrl("/injective.crypto.v1beta1.ethsecp256k1.PubKey")
                .setValue(pubKey.toByteString()).build()
        } else {
            val pubKey = PubKey.newBuilder().setKey(ByteString.copyFrom(ecKey.pubKey)).build()
            Any.newBuilder().setTypeUrl("/cosmos.crypto.secp256k1.PubKey")
                .setValue(pubKey.toByteString()).build()
        }
    }

    private fun grpcByteSignature(selectedChain: CosmosLine?, toSignByte: ByteArray?): ByteArray {
        val sigData = ByteArray(64)
        if (selectedChain?.evmCompatible == true || selectedChain is ChainInjective) {
            val sig = Sign.signMessage(toSignByte, ECKeyPair.create(selectedChain.privateKey))
            System.arraycopy(sig.r, 0, sigData, 0, 32)
            System.arraycopy(sig.s, 0, sigData, 32, 32)
            return sigData

        } else {
            val sha256Hash = Sha256Hash.hash(toSignByte)
            ECKey.fromPrivate(selectedChain?.privateKey)?.sign(Sha256Hash.wrap(sha256Hash))?.let {
                System.arraycopy(integerToBytes(it.r, 32), 0, sigData, 0, 32)
                System.arraycopy(integerToBytes(it.s, 32), 0, sigData, 32, 32)
                return sigData
            }
            return sigData
        }
    }

    private fun parseAuthGrpc(auth: QueryAccountResponse?): Triple<String, Long, Long> {
        val rawAccount = auth?.account
        // desmos needed

        rawAccount?.let {
            if (it.typeUrl.contains(AuthProto.BaseAccount.getDescriptor().fullName)) {
                AuthProto.BaseAccount.parseFrom(rawAccount.value)?.let { account ->
                    return Triple(account.address, account.accountNumber, account.sequence)
                }
            } else if (rawAccount.typeUrl.contains(VestingProto.PeriodicVestingAccount.getDescriptor().fullName)) {
                VestingProto.PeriodicVestingAccount.parseFrom(rawAccount.value).baseVestingAccount.baseAccount?.let { account ->
                    return Triple(account.address, account.accountNumber, account.sequence)
                }

            } else if (rawAccount.typeUrl.contains(VestingProto.ContinuousVestingAccount.getDescriptor().fullName)) {
                VestingProto.ContinuousVestingAccount.parseFrom(rawAccount.value).baseVestingAccount.baseAccount?.let { account ->
                    return Triple(account.address, account.accountNumber, account.sequence)
                }

            } else if (rawAccount.typeUrl.contains(VestingProto.DelayedVestingAccount.getDescriptor().fullName)) {
                VestingProto.DelayedVestingAccount.parseFrom(rawAccount.value).baseVestingAccount.baseAccount?.let { account ->
                    return Triple(account.address, account.accountNumber, account.sequence)
                }

            } else if (rawAccount.typeUrl.contains(com.stride.vesting.VestingProto.StridePeriodicVestingAccount.getDescriptor().fullName)) {
                com.stride.vesting.VestingProto.StridePeriodicVestingAccount.parseFrom(rawAccount.value).baseVestingAccount.baseAccount?.let { account ->
                    return Triple(account.address, account.accountNumber, account.sequence)
                }

            } else if (rawAccount.typeUrl.contains(AccountProto.EthAccount.getDescriptor().fullName)) {
                AccountProto.EthAccount.parseFrom(rawAccount.value).baseAccount?.let { account ->
                    return Triple(account.address, account.accountNumber, account.sequence)
                }

            } else if (rawAccount.typeUrl.contains(com.injective.types.v1beta1.AccountProto.EthAccount.getDescriptor().fullName)) {
                com.injective.types.v1beta1.AccountProto.EthAccount.parseFrom(rawAccount.value).baseAccount?.let { account ->
                    return Triple(account.address, account.accountNumber, account.sequence)
                }

            } else {
                return Triple("", -1, -1)
            }
        }
        return Triple("", -1, -1)
    }

    private fun signBroadcastTx(
        auth: QueryAccountResponse?,
        msgAnys: List<Any>?,
        fee: Fee?,
        memo: String,
        selectedChain: CosmosLine?
    ): BroadcastTxRequest? {
        val txBody = grpcTxBody(msgAnys, memo)
        val signerInfo = grpcSignerInfo(auth, selectedChain)
        val authInfo = grpcAuthInfo(signerInfo, fee)
        val broadcastTx = grpcBroadcastTx(auth, txBody, authInfo, selectedChain)
        return BroadcastTxRequest.newBuilder().setModeValue(BroadcastMode.BROADCAST_MODE_SYNC.number).setTxBytes(broadcastTx?.toByteString()).build()
    }

    private fun signSimulTx(
        auth: QueryAccountResponse?,
        msgAnys: List<Any>?,
        fee: Fee?,
        memo: String
    ): SimulateRequest? {
        val txBody = grpcTxBody(msgAnys, memo)
        val signerInfo = grpcSimulInfo(auth)
        val authInfo = grpcAuthInfo(signerInfo, fee)
        val simulateTx = grpcSimulTx(txBody, authInfo)
        return SimulateRequest.newBuilder().setTxBytes(simulateTx?.toByteString()).build()
    }

    private fun grpcTxBody(msgsAny: List<Any>?, memo: String): TxBody? {
        val builder = TxBody.newBuilder()
        msgsAny?.forEach { msg ->
            builder.addMessages(msg)
        }
        return builder.setMemo(memo).build()
    }

    private fun grpcSignerInfo(auth: QueryAccountResponse?, selectedChain: CosmosLine?): SignerInfo? {
        ECKey.fromPrivate(selectedChain?.privateKey)?.let {
            val pubKey = generateGrpcPubKeyFromPriv(selectedChain, it.privateKeyAsHex)
            val singleMode =
                ModeInfo.Single.newBuilder().setMode(SigningProto.SignMode.SIGN_MODE_DIRECT).build()
            val modeInfo = ModeInfo.newBuilder().setSingle(singleMode).build()
            return SignerInfo.newBuilder().setPublicKey(pubKey).setModeInfo(modeInfo)
                .setSequence(parseAuthGrpc(auth).third).build()
        }
        return null
    }

    private fun grpcSimulInfo(auth: QueryAccountResponse?): SignerInfo {
        val singleMode =
            ModeInfo.Single.newBuilder().setMode(SigningProto.SignMode.SIGN_MODE_DIRECT).build()
        val modeInfo = ModeInfo.newBuilder().setSingle(singleMode).build()
        var pubKey: Any? = null
        auth?.let {
            pubKey = if (it.account.typeUrl.contains("/ethermint")) {
                Any.newBuilder().setTypeUrl("/ethermint.crypto.v1.ethsecp256k1.PubKey").build()
            } else if (it.account.typeUrl.contains("/injective")) {
                Any.newBuilder().setTypeUrl("/injective.crypto.v1beta1.ethsecp256k1.PubKey").build()
            } else {
                Any.newBuilder().setTypeUrl("/cosmos.crypto.secp256k1.PubKey").build()
            }
        }
        return SignerInfo.newBuilder().setPublicKey(pubKey).setModeInfo(modeInfo)
            .setSequence(parseAuthGrpc(auth).third).build()
    }

    private fun grpcAuthInfo(signerInfo: SignerInfo?, fee: Fee?): AuthInfo? {
        fee?.getAmount(0)?.let {
            val feeCoin =
                CoinProto.Coin.newBuilder().setAmount(it.amount).setDenom(it.denom).build()
            val txFee = Fee.newBuilder().addAmount(feeCoin).setGasLimit(fee.gasLimit).build()
            return AuthInfo.newBuilder().setFee(txFee).addSignerInfos(signerInfo).build()
        }
        return null
    }

    private fun grpcBroadcastTx(
        auth: QueryAccountResponse?,
        txBody: TxBody?,
        authInfo: AuthInfo?,
        selectedChain: CosmosLine?
    ): TxRaw? {
        val signDoc = SignDoc.newBuilder().setBodyBytes(txBody?.toByteString())
            .setAuthInfoBytes(authInfo?.toByteString()).setChainId(selectedChain?.chainId)
            .setAccountNumber(
                parseAuthGrpc(auth).second
            ).build()
        val sigByte = grpcByteSignature(selectedChain, signDoc.toByteArray())
        return TxRaw.newBuilder().setBodyBytes(txBody?.toByteString())
            .setAuthInfoBytes(authInfo?.toByteString()).addSignatures(ByteString.copyFrom(sigByte))
            .build()
    }

    private fun grpcSimulTx(txBody: TxBody?, authInfo: AuthInfo?): Tx? {
        val sigByte = ByteArray(64)
        return Tx.newBuilder().setAuthInfo(authInfo).setBody(txBody)
            .addSignatures(ByteString.copyFrom(sigByte)).build()
    }
}