package wannabit.io.cosmostaion.activities.txs.osmosis;

import static wannabit.io.cosmostaion.base.BaseConstant.CONST_PW_TX_OSMOSIS_SWAP;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

import cosmos.tx.v1beta1.ServiceOuterClass;
import wannabit.io.cosmostaion.R;
import wannabit.io.cosmostaion.activities.PasswordCheckActivity;
import wannabit.io.cosmostaion.base.BaseBroadCastActivity;
import wannabit.io.cosmostaion.base.BaseChain;
import wannabit.io.cosmostaion.base.BaseFragment;
import wannabit.io.cosmostaion.base.chains.ChainFactory;
import wannabit.io.cosmostaion.cosmos.MsgGenerator;
import wannabit.io.cosmostaion.cosmos.Signer;
import wannabit.io.cosmostaion.fragment.StepFeeSetFragment;
import wannabit.io.cosmostaion.fragment.StepMemoFragment;
import wannabit.io.cosmostaion.fragment.txs.osmosis.CoinSwapStep0Fragment;
import wannabit.io.cosmostaion.fragment.txs.osmosis.CoinSwapStep3Fragment;
import wannabit.io.cosmostaion.model.type.Msg;
import wannabit.io.cosmostaion.task.TaskResult;
import wannabit.io.cosmostaion.task.gRpcTask.broadcast.OsmosisSwapGrpcTask;
import wannabit.io.cosmostaion.utils.LedgerManager;
import wannabit.io.cosmostaion.utils.WKey;

public class SwapActivity extends BaseBroadCastActivity {

    private RelativeLayout mRootView;
    private Toolbar mToolbar;
    private TextView mTitle;
    private ImageView mIvStep;
    private TextView mTvStep;
    private ViewPager mViewPager;
    private CoinSwapPageAdapter mPageAdapter;

    public String mInputDenom;
    public String mOutputDenom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step);
        initView();
        loadData();
    }

    public void initView() {
        mRootView = findViewById(R.id.root_view);
        mToolbar = findViewById(R.id.tool_bar);
        mTitle = findViewById(R.id.toolbar_title);
        mIvStep = findViewById(R.id.send_step);
        mTvStep = findViewById(R.id.send_step_msg);
        mViewPager = findViewById(R.id.view_pager);
        mTitle.setText(getString(R.string.str_title_swap));

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mIvStep.setImageDrawable(ContextCompat.getDrawable(SwapActivity.this, R.drawable.step_4_img_1));
        mTvStep.setText(getString(R.string.str_swap_step_0));

        mPageAdapter = new CoinSwapPageAdapter(getSupportFragmentManager());
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mPageAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                if (i == 0) {
                    mIvStep.setImageDrawable(ContextCompat.getDrawable(SwapActivity.this, R.drawable.step_4_img_1));
                    mTvStep.setText(getString(R.string.str_swap_step_0));
                    mPageAdapter.mCurrentFragment.onRefreshTab();
                } else if (i == 1) {
                    mIvStep.setImageDrawable(ContextCompat.getDrawable(SwapActivity.this, R.drawable.step_4_img_2));
                    mTvStep.setText(getString(R.string.str_tx_step_memo));
                } else if (i == 2) {
                    mIvStep.setImageDrawable(ContextCompat.getDrawable(SwapActivity.this, R.drawable.step_4_img_3));
                    mTvStep.setText(getString(R.string.str_tx_step_fee));
                    mPageAdapter.mCurrentFragment.onRefreshTab();
                } else if (i == 3) {
                    mIvStep.setImageDrawable(ContextCompat.getDrawable(SwapActivity.this, R.drawable.step_4_img_4));
                    mTvStep.setText(getString(R.string.str_tx_step_confirm));
                    mPageAdapter.mCurrentFragment.onRefreshTab();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        mViewPager.setCurrentItem(0);

        mRootView.setOnClickListener(v -> onHideKeyboard());
    }

    public void loadData() {
        mAccount = getBaseDao().onSelectAccount(getBaseDao().getLastUser());
        mBaseChain = BaseChain.getChain(mAccount.baseChain);
        mChainConfig = ChainFactory.getChain(mBaseChain);
        mTxType = CONST_PW_TX_OSMOSIS_SWAP;
        mOsmosisPoolId = getIntent().getLongExtra("mPoolId", 0);
        mInputDenom = getIntent().getStringExtra("inputDenom");
        mOutputDenom = getIntent().getStringExtra("outputDenom");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        onHideKeyboard();
        if (mViewPager.getCurrentItem() > 0) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
        } else {
            super.onBackPressed();
        }
    }

    public void onNextStep() {
        if (mViewPager.getCurrentItem() < mPageAdapter.getCount() - 1) {
            onHideKeyboard();
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
        }
    }

    public void onBeforeStep() {
        if (mViewPager.getCurrentItem() > 0) {
            onHideKeyboard();
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
        } else {
            onBackPressed();
        }
    }

    public void onStartSwap() {
        if (mAccount.isLedger()) {
            onSwapByLedger();
        } else if (getBaseDao().isAutoPass()) {
            onBroadCastTx();
        } else {
            Intent intent = new Intent(SwapActivity.this, PasswordCheckActivity.class);
            activityResultLauncher.launch(intent);
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out);
        }
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            onShowWaitDialog();
            onBroadCastTx();
        }
    });

    private void onSwapByLedger() {
        runOnUiThread(() -> LedgerManager.getInstance().signAndBroadcast(SwapActivity.this, mAccount, new LedgerManager.LedgerSignListener() {
            @NonNull
            @Override
            public String getMessage() {
                ArrayList<Msg> delegateMsgs = MsgGenerator.genSwapMsgs(mAccount.address, String.valueOf(mOsmosisPoolId), mSwapInCoin, mSwapOutCoin);
                return WKey.onGetLedgerMessage(getBaseDao(), mChainConfig, mAccount, delegateMsgs, mTxFee, mTxMemo);
            }

            @NonNull
            @Override
            public ServiceOuterClass.BroadcastTxRequest makeBroadcastTxRequest(@NonNull byte[] currentPubKey) {
                return Signer.getGrpcLedgerSwapReq(WKey.onAuthResponse(mBaseChain, mAccount), mOsmosisSwapAmountInRoute, mSwapInCoin, mSwapOutCoin.amount, mTxFee, mTxMemo, LedgerManager.Companion.getInstance().getCurrentPubKey(), WKey.getLedgerSigData(currentPubKey));
            }

            @Override
            public void processResponse(@NonNull TaskResult mResult, @NonNull ServiceOuterClass.BroadcastTxResponse response) {
                onCommonIntentTx(SwapActivity.this, mResult);
            }
        }));
    }

    private void onBroadCastTx() {
        new OsmosisSwapGrpcTask(getBaseApplication(), result -> {
            onCommonIntentTx(SwapActivity.this, result);
        }, mAccount, mBaseChain, mOsmosisSwapAmountInRoute, mSwapInCoin, mSwapOutCoin, mTxMemo, mTxFee, getBaseDao().getChainIdGrpc()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class CoinSwapPageAdapter extends FragmentPagerAdapter {

        private ArrayList<BaseFragment> mFragments = new ArrayList<>();
        private BaseFragment mCurrentFragment;

        public CoinSwapPageAdapter(FragmentManager fm) {
            super(fm);
            mFragments.clear();
            mFragments.add(CoinSwapStep0Fragment.newInstance());
            mFragments.add(StepMemoFragment.newInstance());
            mFragments.add(StepFeeSetFragment.newInstance());
            mFragments.add(CoinSwapStep3Fragment.newInstance());
        }

        @Override
        public BaseFragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (getCurrentFragment() != object) {
                mCurrentFragment = ((BaseFragment) object);
            }
            super.setPrimaryItem(container, position, object);
        }

        public BaseFragment getCurrentFragment() {
            return mCurrentFragment;
        }

        public ArrayList<BaseFragment> getFragments() {
            return mFragments;
        }
    }
}
