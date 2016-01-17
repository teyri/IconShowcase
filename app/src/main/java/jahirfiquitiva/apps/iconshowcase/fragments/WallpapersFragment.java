package jahirfiquitiva.apps.iconshowcase.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import jahirfiquitiva.apps.iconshowcase.R;
import jahirfiquitiva.apps.iconshowcase.activities.ShowcaseActivity;
import jahirfiquitiva.apps.iconshowcase.activities.ViewerActivity;
import jahirfiquitiva.apps.iconshowcase.adapters.WallpapersAdapter;
import jahirfiquitiva.apps.iconshowcase.models.WallpaperItem;
import jahirfiquitiva.apps.iconshowcase.models.WallpapersList;
import jahirfiquitiva.apps.iconshowcase.tasks.ApplyWallpaper;
import jahirfiquitiva.apps.iconshowcase.utilities.JSONParser;
import jahirfiquitiva.apps.iconshowcase.utilities.Preferences;
import jahirfiquitiva.apps.iconshowcase.utilities.ThemeUtils;
import jahirfiquitiva.apps.iconshowcase.utilities.Utils;
import jahirfiquitiva.apps.iconshowcase.views.GridSpacingItemDecoration;

public class WallpapersFragment extends Fragment {

    private static ProgressBar mProgress;
    public static WallpapersAdapter mAdapter;
    private static ImageView noConnection;
    private static RecyclerView mRecyclerView;
    private static RecyclerFastScroller fastScroller;
    public static SwipeRefreshLayout mSwipeRefreshLayout;
    public static Activity context;
    private static ViewGroup layout;
    private static GridSpacingItemDecoration gridSpacing;

    private static boolean worked;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        setHasOptionsMenu(true);

        context = getActivity();

        if (layout != null) {
            ViewGroup parent = (ViewGroup) layout.getParent();
            if (parent != null) {
                parent.removeView(layout);
            }
        }
        try {
            layout = (ViewGroup) inflater.inflate(R.layout.wallpapers_section, container, false);
        } catch (InflateException e) {

        }

        if (!ShowcaseActivity.wallsPicker) {
            showWallsAdviceDialog(getActivity());
        }

        int light = ContextCompat.getColor(context, android.R.color.white);
        int dark = ContextCompat.getColor(context, R.color.card_dark_background);

        noConnection = (ImageView) layout.findViewById(R.id.no_connected_icon);
        noConnection.setImageDrawable(new IconicsDrawable(context)
                .icon(GoogleMaterial.Icon.gmd_cloud_off)
                .color(ThemeUtils.darkTheme ? light : dark)
                .sizeDp(144));

        noConnection.setVisibility(View.GONE);
        mProgress = (ProgressBar) layout.findViewById(R.id.progress);

        mRecyclerView = (RecyclerView) layout.findViewById(R.id.wallsGrid);

        fastScroller = (RecyclerFastScroller) layout.findViewById(R.id.rvFastScroller);
        fastScroller.attachRecyclerView(mRecyclerView);

        mSwipeRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swipeRefreshLayout);

        setupRecyclerView(false, 0);
        mRecyclerView.setVisibility(View.GONE);

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(ThemeUtils.darkTheme ? dark : light);

        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.accent,
                R.color.accent,
                R.color.accent);
        mSwipeRefreshLayout.setEnabled(false);

        setupLayout();

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getActivity();
    }

    private static void setupLayout() {

        if (WallpapersList.getWallpapersList() != null && WallpapersList.getWallpapersList().size() > 0) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter = new WallpapersAdapter(context,
                            new WallpapersAdapter.ClickListener() {
                                @Override
                                public void onClick(WallpapersAdapter.WallsHolder view,
                                                    int position, boolean longClick) {
                                    if ((longClick && !ShowcaseActivity.wallsPicker) || ShowcaseActivity.wallsPicker) {
                                        pickWallpaper(position, WallpapersList.getWallpapersList(), ShowcaseActivity.wallsPicker);
                                    } else {
                                        openViewer(context, view, position, WallpapersList.getWallpapersList());
                                    }
                                }

                            });

                    mAdapter.setData(WallpapersList.getWallpapersList());

                    mRecyclerView.setAdapter(mAdapter);

                    if (Utils.hasNetwork(context)) {
                        showStuff();
                    } else {
                        hideStuff();
                    }
                }
            });
        } else {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mAdapter != null) {
                        hideStuff();
                    }
                    if (layout != null) {
                        Timer timer = new Timer();
                        noConnection.setVisibility(View.GONE);
                        showProgressBar();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideStuff();
                                    }
                                });
                            }
                        }, 4000);
                    }
                }
            });
        }
    }

    private static void showStuff() {
        hideProgressBar();
        noConnection.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        fastScroller.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private static void hideStuff() {
        hideProgressBar();
        noConnection.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
        fastScroller.setVisibility(View.GONE);
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private static void showProgressBar() {
        if (mProgress != null) {
            if (mProgress.getVisibility() != View.VISIBLE) {
                mProgress.setVisibility(View.VISIBLE);
            }
        }
    }

    private static void hideProgressBar() {
        if (mProgress != null) {
            if (mProgress.getVisibility() != View.GONE) {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    private static void setupRecyclerView(boolean updating, int newColumns) {
        Preferences mPrefs = new Preferences(context);
        if (updating && gridSpacing != null) {
            mPrefs.setWallsColumnsNumber(newColumns);
            mRecyclerView.removeItemDecoration(gridSpacing);
        }

        int columnsNumber = mPrefs.getWallsColumnsNumber();
        if (context.getResources().getConfiguration().orientation == 2) {
            columnsNumber += 2;
        }

        mRecyclerView.setLayoutManager(new GridLayoutManager(context,
                columnsNumber));
        gridSpacing = new GridSpacingItemDecoration(columnsNumber,
                context.getResources().getDimensionPixelSize(R.dimen.lists_padding),
                true);
        mRecyclerView.addItemDecoration(gridSpacing);
        mRecyclerView.setHasFixedSize(true);
        if (mRecyclerView.getVisibility() != View.VISIBLE) {
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        //mRecyclerView.updateViewLayout(mRecyclerView, mRecyclerView.getLayoutParams());
        if (fastScroller.getVisibility() != View.VISIBLE) {
            fastScroller.setVisibility(View.VISIBLE);
        }
    }

    public static void updateRecyclerView(int newColumns) {
        mRecyclerView.setVisibility(View.GONE);
        fastScroller.setVisibility(View.GONE);
        showProgressBar();
        setupRecyclerView(true, newColumns);
        hideProgressBar();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.walls_menu, menu);
    }

    public static void refreshWalls(Activity context) {
        if (Utils.hasNetwork(context)) {
            Utils.showSimpleSnackbar(layout,
                    context.getResources().getString(R.string.refreshing_walls), 1);
        } else {
            Utils.showSimpleSnackbar(layout,
                    context.getResources().getString(R.string.no_conn_title), 1);
        }
        mSwipeRefreshLayout.setEnabled(true);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    public static void openViewer(Context context, WallpapersAdapter.WallsHolder wallsHolder,
                                  int index, final ArrayList<WallpaperItem> list) {
        final Intent intent = new Intent(context, ViewerActivity.class);
        WallpaperItem wallItem = list.get(index);
        intent.putExtra("wallName", wallItem.getWallName());
        intent.putExtra("authorName", wallItem.getWallAuthor());
        intent.putExtra("wallUrl", wallItem.getWallURL());
        intent.putExtra("transitionName", ViewCompat.getTransitionName(wallsHolder.wall));
        Bitmap bitmap = drawableToBitmap(wallsHolder.wall.getDrawable());
        try {
            String filename = "temp.png";
            FileOutputStream stream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            intent.putExtra("image", filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                (Activity) context, wallsHolder.wall, ViewCompat.getTransitionName(wallsHolder.wall));
        context.startActivity(intent, options.toBundle());

    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // DownloadJSON AsyncTask
    public static class DownloadJSON extends AsyncTask<Void, Void, Void> {

        ShowcaseActivity.WallsListInterface wi;
        private static ArrayList<String> names = new ArrayList<>();
        private static ArrayList<String> authors = new ArrayList<>();
        private static ArrayList<String> urls = new ArrayList<>();

        private Context taskContext;

        private WeakReference<Activity> wrActivity;

        static long startTime, endTime;

        public DownloadJSON(ShowcaseActivity.WallsListInterface wi, AppCompatActivity activity) {
            this.wi = wi;
            this.wrActivity = new WeakReference<Activity>(activity);
        }

        public DownloadJSON(ShowcaseActivity.WallsListInterface wi, Context context) {
            this.wi = wi;
            this.taskContext = context;
        }

        @Override
        protected void onPreExecute() {
            startTime = System.currentTimeMillis();

            if (wrActivity != null) {
                final Activity a = wrActivity.get();
                if (a != null) {
                    this.taskContext = a.getApplicationContext();
                }
            }

            names.clear();
            authors.clear();
            urls.clear();
        }

        @Override
        protected Void doInBackground(Void... params) {

            JSONObject json = JSONParser.getJSONFromURL(Utils.getStringFromResources(taskContext, R.string.json_file_url));

            if (json != null) {
                try {
                    // Locate the array name in JSON
                    JSONArray jsonarray = json.getJSONArray("wallpapers");

                    for (int i = 0; i < jsonarray.length(); i++) {
                        json = jsonarray.getJSONObject(i);
                        // Retrieve JSON Objects
                        names.add(json.getString("name"));
                        authors.add(json.getString("author"));
                        urls.add(json.getString("url"));
                    }

                    WallpapersList.createWallpapersList(names, authors, urls);

                    worked = true;

                } catch (JSONException e) {
                    worked = false;
                    e.printStackTrace();
                }
            } else {
                worked = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void args) {

            endTime = System.currentTimeMillis();
            Utils.showLog("Walls Task completed in: " + String.valueOf((endTime - startTime) / 1000) + " secs.");

            if (layout != null) {
                setupLayout();
            }

            if (wi != null)
                wi.checkWallsListCreation(worked);

        }
    }

    private static void pickWallpaper(int position, final ArrayList<WallpaperItem> list,
                                      final boolean isWallsPicker) {
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .content(R.string.downloading_wallpaper)
                .progress(true, 0)
                .cancelable(false)
                .show();

        WallpaperItem wallItem = list.get(position);

        Glide.with(context)
                .load(wallItem.getWallURL())
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        if (resource != null) {
                            new ApplyWallpaper(context, dialog, resource, isWallsPicker, layout, null).execute();
                        }
                    }
                });
    }

    private void showWallsAdviceDialog(Context context) {
        final Preferences mPrefs = new Preferences(context);
        if (!mPrefs.getWallsDialogDismissed()) {
            new MaterialDialog.Builder(context)
                    .title(R.string.advice)
                    .content(R.string.walls_advice)
                    .positiveText(R.string.close)
                    .neutralText(R.string.dontshow)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            mPrefs.setWallsDialogDismissed(false);
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            mPrefs.setWallsDialogDismissed(true);
                        }
                    })
                    .show();
        }
    }

}