package wannabit.io.cosmostaion.task.gRpcTask.simulate;

import static wannabit.io.cosmostaion.base.BaseConstant.CONST_PW_TX_ADD_LIQUIDITY;

import cosmos.tx.v1beta1.ServiceGrpc;
import cosmos.tx.v1beta1.ServiceOuterClass;
import wannabit.io.cosmostaion.base.BaseApplication;
import wannabit.io.cosmostaion.base.BaseChain;
import wannabit.io.cosmostaion.cosmos.Signer;
import wannabit.io.cosmostaion.dao.Account;
import wannabit.io.cosmostaion.model.type.Coin;
import wannabit.io.cosmostaion.model.type.Fee;
import wannabit.io.cosmostaion.network.ChannelBuilder;
import wannabit.io.cosmostaion.task.CommonTask;
import wannabit.io.cosmostaion.task.TaskListener;
import wannabit.io.cosmostaion.task.TaskResult;
import wannabit.io.cosmostaion.utils.WKey;
import wannabit.io.cosmostaion.utils.WLog;

public class SimulKavaLiquidityGrpcTask extends CommonTask {

    private int                 mTxType;
    private BaseChain           mBaseChain;
    private Account             mAccount;
    private String              mValidatorAddress;
    private Coin                mAmount;
    private String              mMemo;
    private Fee                 mFees;
    private String              mChainId;

    public SimulKavaLiquidityGrpcTask(BaseApplication app, TaskListener listener, int txType, BaseChain basechain, Account account, String validatorAddress, Coin amount, String memo, Fee fee, String chainId) {
        super(app, listener);
        this.mTxType = txType;
        this.mBaseChain = basechain;
        this.mAccount = account;
        this.mValidatorAddress = validatorAddress;
        this.mAmount = amount;
        this.mMemo = memo;
        this.mFees = fee;
        this.mChainId = chainId;
    }

    @Override
    protected TaskResult doInBackground(String... strings) {
        try {
            ServiceGrpc.ServiceBlockingStub txService = ServiceGrpc.newBlockingStub(ChannelBuilder.getChain(mBaseChain));
            ServiceOuterClass.SimulateRequest simulateTxRequest = null;
            if (mTxType == CONST_PW_TX_ADD_LIQUIDITY) {
                simulateTxRequest = Signer.getGrpcKavaAddLiquiditySimulateReq(WKey.onAuthResponse(mBaseChain, mAccount), mAccount.address, mValidatorAddress, mAmount, mFees, mMemo, WKey.getECKey(mApp, mAccount), mChainId, mAccount.customPath, mBaseChain);
            } else {
                simulateTxRequest = Signer.getGrpcKavaRemoveLiquiditySimulateReq(WKey.onAuthResponse(mBaseChain, mAccount), mAccount.address, mValidatorAddress, mAmount, mFees, mMemo, WKey.getECKey(mApp, mAccount), mChainId, mAccount.customPath, mBaseChain);
            }
            ServiceOuterClass.SimulateResponse response = txService.simulate(simulateTxRequest);

            mResult.resultData = response.getGasInfo();
            mResult.isSuccess = true;

        } catch (Exception e) {
            WLog.e( "SimulKavaLiquidityGrpcTask "+ e.getMessage());
            mResult.isSuccess = false;
            mResult.errorMsg = e.getMessage();
        }
        return mResult;
    }
}