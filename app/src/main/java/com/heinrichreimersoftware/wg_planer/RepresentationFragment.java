package com.heinrichreimersoftware.wg_planer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.heinrichreimersoftware.wg_planer.adapters.ListLoader;
import com.heinrichreimersoftware.wg_planer.adapters.RepresentationAdapter;
import com.heinrichreimersoftware.wg_planer.authentication.AccountGeneral;
import com.heinrichreimersoftware.wg_planer.data.RepresentationsContentHelper;
import com.heinrichreimersoftware.wg_planer.data.RepresentationsContract;
import com.heinrichreimersoftware.wg_planer.data.UserContentHelper;
import com.heinrichreimersoftware.wg_planer.structure.Representation;
import com.heinrichreimersoftware.wg_planer.structure.User;
import com.heinrichreimersoftware.wg_planer.sync.SyncStatusManager;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.FadeInAnimator;

public class RepresentationFragment extends Fragment {

    public static final String ARG_DATE = "section_number_representation";
    @Bind(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @Bind(R.id.list)
    RecyclerView list;
    @Bind(R.id.emptyView)
    TextView emptyView;
    private SyncStatusManager syncStatusManager;
    private ListLoader<Representation, RepresentationAdapter> loader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(MainActivity.TAG, "onCreateView()");
        View rootView = inflater.inflate(R.layout.fragment_representation, container, false);
        ButterKnife.bind(this, rootView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);

        list.setItemAnimator(new FadeInAnimator());

        RepresentationAdapter adapter = new RepresentationAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyView();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateEmptyView();
            }
        });
        list.setAdapter(adapter);
        updateEmptyView();

        emptyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeRefreshLayout.setRefreshing(true);
                requestSync();
            }
        });

		/* Swipe refresh */
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestSync();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.swipe_refresh_0, R.color.swipe_refresh_1, R.color.swipe_refresh_2);
        swipeRefreshLayout.setRefreshing(false);

        list.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int topRowVerticalPosition = (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                swipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }
        });

        loader = new ListLoader<>(adapter, new ListLoader.Task<Representation>() {
            @Override
            public List<Representation> load() {
                Calendar date = new GregorianCalendar();
                date.setTimeInMillis(getArguments().getLong(ARG_DATE));

                User user = UserContentHelper.getUser(getActivity());
                if (user != null) {
                    String schoolClass = user.getSchoolClass();
                    if (schoolClass != null && !schoolClass.equals("")) {
                        return RepresentationsContentHelper.getRepresentations(getActivity(), date, schoolClass);
                    }
                    return RepresentationsContentHelper.getRepresentations(getActivity(), date);
                }
                return RepresentationsContentHelper.getRepresentations(getActivity(), date);
            }
        });
        loader.load();

        syncStatusManager = new SyncStatusManager(getActivity(), RepresentationsContract.AUTHORITY);
        syncStatusManager.addOnSyncStatusChangedListener(new SyncStatusManager.OnSyncStatusChangedListener() {
            @Override
            public void onStatusChanged(int state, String affectedAuthority) {
                if (state == SyncStatusManager.STATE_NO_LONGER_SYNCING) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                            loader.load();
                        }
                    });
                }
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (loader != null) {
            loader.load();
        }
    }

    @Override
    public void onDestroyView() {
        if (loader != null) {
            loader.cancel();
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncStatusManager.start();
    }

    @Override
    public void onPause() {
        syncStatusManager.stop();
        super.onPause();
    }

    private void updateEmptyView() {
        if (list.getAdapter().getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setAlpha(0);
            emptyView.animate().alpha(1).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            list.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            list.setAlpha(0);
            list.animate().alpha(1).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
        }
    }

    private void requestSync() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        AccountManager accountManager = AccountManager.get(getActivity());
        if (accountManager != null) {
            for (Account account : accountManager.getAccountsByType(AccountGeneral.ACCOUNT_TYPE)) {
                ContentResolver.requestSync(account, RepresentationsContract.AUTHORITY, bundle);
            }
        }
    }
}
