package wannabit.io.cosmostaion.fragment.txs.neutron.dao

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import wannabit.io.cosmostaion.R
import wannabit.io.cosmostaion.activities.PasswordCheckActivity
import wannabit.io.cosmostaion.activities.txs.neutron.dao.DaoProposalActivity
import wannabit.io.cosmostaion.base.BaseBroadCastActivity
import wannabit.io.cosmostaion.base.BaseConstant
import wannabit.io.cosmostaion.base.BaseFragment
import wannabit.io.cosmostaion.cosmos.Signer
import wannabit.io.cosmostaion.databinding.FragmentDaoVoteStep3Binding
import wannabit.io.cosmostaion.model.viewModel.neutron.DaoViewModel
import wannabit.io.cosmostaion.network.req.neutron.*
import wannabit.io.cosmostaion.utils.WDp
import wannabit.io.cosmostaion.utils.WKey
import wannabit.io.cosmostaion.utils.getTxResultIntent

@AndroidEntryPoint
class DaoVoteStep3Fragment : BaseFragment() {

    private var _binding: FragmentDaoVoteStep3Binding? = null
    private val binding get() = _binding!!

    private val daoViewModel: DaoViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDaoVoteStep3Binding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
    }

    override fun onRefreshTab() {
        binding.apply {
            getSActivity()?.let {
                WDp.setDpCoin(requireContext(), baseDao, baseActivity.mChainConfig, it.mTxFee.amount[0], feeSymbol, feeAmount)
                if (it.mTxType == BaseConstant.CONST_PW_TX_DAO_SINGLE_PROPOSAL) {
                    myOpinion.text = "# ${it.mProposalData.id} - ${it.mOpinion}"
                } else {
                    myOpinion.text = "# ${it.mProposalData.id} - ${it.mOptionId}"
                }
                memo.text = it.mTxMemo
            }
        }
    }

    private fun onClick() {
        binding.apply {
            btnBefore.setOnClickListener { getSActivity()?.onBeforeStep() }
            btnConfirm.setOnClickListener { onStartVote() }
        }
    }

    private fun onStartVote() {
        if (baseDao.isAutoPass) {
            onBroadCastTx()
        } else {
            Intent(requireContext(), PasswordCheckActivity::class.java).apply {
                activityResultLauncher.launch(this)
                baseActivity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
            }
        }
    }

    var activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == BaseBroadCastActivity.RESULT_OK) {
            getSActivity()?.onShowWaitDialog()
            onBroadCastTx()
        }
    }

    private fun onBroadCastTx() {
        getSActivity()?.let {
            var req: Any? = null
            if (it.mTxType == BaseConstant.CONST_PW_TX_DAO_SINGLE_PROPOSAL) {
                req = VoteReq(Vote(it.mProposalData.id?.toInt(), it.mOpinion))
            } else if (it.mTxType == BaseConstant.CONST_PW_TX_DAO_MULTI_PROPOSAL) {
                req = MultiVoteReq(MultiVote(it.mProposalData.id?.toInt(), WeightVote(it.mOptionId)))
            }

            val broadcastTxRequest = Signer.getGrpcContractReq(
                WKey.onAuthResponse(it.mBaseChain, it.mAccount), req, it.mAccount.address, it.mProposalModule.address, it.mAmount, it.mTxFee, it.mTxMemo, WKey.getECKey(baseApplication, it.mAccount), baseDao.chainIdGrpc, it.mAccount.customPath, it.mBaseChain, it.mTxType
            )
            daoViewModel.broadCastTx(it.mBaseChain, broadcastTxRequest)
        }

        daoViewModel.txResponse.observe(requireActivity()) {
            getTxResultIntent(requireContext(), it)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun getSActivity(): DaoProposalActivity? {
        return baseActivity as? DaoProposalActivity
    }
}