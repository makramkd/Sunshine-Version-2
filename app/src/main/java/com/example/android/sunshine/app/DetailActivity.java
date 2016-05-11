package com.example.android.sunshine.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public static class DetailFragment extends Fragment {
        public static final String DETAIL_FRAGMENT = DetailFragment.class.getSimpleName();

        private TextView view;

        public DetailFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            // get intent and extras
            Intent intent = getActivity().getIntent();
            String forecast = intent.getStringExtra(Intent.EXTRA_TEXT);
            view = (TextView) rootView.findViewById(R.id.detail_forecast);
            view.setText(forecast);

            return rootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);

            getActivity().getMenuInflater().inflate(R.menu.detailfragment, menu);

            MenuItem item = menu.findItem(R.id.action_share_forecast);

            ShareActionProvider provider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

            if (provider != null) {
                provider.setShareIntent(createShareForecastIntent());
            } else {
                Log.d(DETAIL_FRAGMENT, "provider is null");
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_share_forecast:
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        private Intent createShareForecastIntent() {
            Intent intent = new Intent(Intent.ACTION_SEND);
            String forecast = view.getText().toString();
            intent.putExtra(Intent.EXTRA_TEXT, forecast + "#SunshineApp");
            intent.setType("text/plain");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            }
            return intent;
        }
    }
}
