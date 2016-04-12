package me.ccrama.redditslide.Adapters;

import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thing;
import net.dean.jraw.paginators.FullnamesPaginator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import me.ccrama.redditslide.Authentication;
import me.ccrama.redditslide.PostMatch;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.util.LogUtil;

/**
 * Created by ccrama on 9/17/2015.
 */
public class HistoryPosts extends GeneralPosts {
    private SwipeRefreshLayout refreshLayout;
    private ContributionAdapter adapter;
    public boolean loading;

    public HistoryPosts() {
    }

    public void bindAdapter(ContributionAdapter a, SwipeRefreshLayout layout) throws ExecutionException, InterruptedException {
        this.adapter = a;
        this.refreshLayout = layout;
        loadMore(a, true);
    }

    public void loadMore(ContributionAdapter adapter, boolean reset) {
        new LoadData(reset).execute();
    }

    FullnamesPaginator paginator;

    public class LoadData extends AsyncTask<String, Void, ArrayList<Contribution>> {
        final boolean reset;

        public LoadData(boolean reset) {
            this.reset = reset;
        }

        @Override
        public void onPostExecute(ArrayList<Contribution> submissions) {
            loading = false;

            if (submissions != null && !submissions.isEmpty()) {
                // new submissions found

                int start = 0;
                if (posts != null) {
                    start = posts.size() + 1;
                }

                ArrayList<Contribution> filteredSubmissions = new ArrayList<>();
                for (Contribution c : submissions) {
                    if (c instanceof Submission) {
                        if (!PostMatch.doesMatch((Submission) c)) {
                            filteredSubmissions.add(c);
                        }
                    } else {
                        filteredSubmissions.add(c);
                    }
                }
                Log.v(LogUtil.getTag(), "SIZE IS " + filteredSubmissions.size());

                if (reset || posts == null) {
                    posts = filteredSubmissions;
                    start = -1;
                } else {
                    posts.addAll(filteredSubmissions);
                }

                final int finalStart = start;
                // update online
                if (refreshLayout != null) {
                    refreshLayout.setRefreshing(false);
                }

                if (finalStart != -1) {
                    adapter.notifyItemRangeInserted(finalStart + 1, posts.size());
                } else {
                    adapter.notifyDataSetChanged();
                }

            } else if (submissions != null) {
                // end of submissions
                nomore = true;
                adapter.notifyDataSetChanged();

            } else if (!nomore) {
                // error
                adapter.setError(true);
            }
            refreshLayout.setRefreshing(false);
        }

        @Override
        protected ArrayList<Contribution> doInBackground(String... subredditPaginators) {
            ArrayList<Contribution> newData = new ArrayList<>();
            try {
                if (reset || paginator == null) {
                    ArrayList<String> ids = new ArrayList<>();
                    HashMap<Long, String> idsSorted = new HashMap<>();
                    Map<String, ?> values = Reddit.seen.getAll();
                    for (String value : values.keySet()) {
                        if (value.length() == 6 && values.get(value) instanceof Boolean){
                            ids.add("t3_" + value);
                        } else if(values.get(value) instanceof Long){
                            idsSorted.put((Long) values.get(value), "t3_" + value);
                        }
                    }

                    if(!idsSorted.isEmpty()) {
                        TreeMap<Long, String> result2 = new TreeMap<>(Collections.reverseOrder());
                        result2.putAll(idsSorted);
                        LogUtil.v("Size is " + result2.size());
                        ids.addAll(0, result2.values());
                    }

                    paginator = new FullnamesPaginator(Authentication.reddit, ids.toArray(new String[ids.size()]));
                    if (!paginator.hasNext()) {
                        nomore = true;
                        return new ArrayList<>();
                    }
                }
                for (Thing c : paginator.next()) {
                    if (c instanceof Submission) {
                        Submission s = (Submission) c;
                        newData.add(s);
                    }
                }

                return newData;
            } catch (Exception e) {
                return null;
            }
        }

    }

}
