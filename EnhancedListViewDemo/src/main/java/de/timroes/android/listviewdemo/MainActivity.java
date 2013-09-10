package de.timroes.android.listviewdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.timroes.android.listview.EnhancedListView;

public class MainActivity extends Activity {

    private EnhancedListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (EnhancedListView)findViewById(R.id.list);

        TextView tv = new TextView(this);
        tv.setText("Header View 1");
        mListView.addHeaderView(tv);
        tv = new TextView(this);
        tv.setText("Header View 2");
        mListView.addHeaderView(tv);


        mListView.setAdapter(new EnhancedListAdapter(this, R.layout.list_item, R.id.text,
                new ArrayList<String>(Arrays.asList(new String[]{
                        "Test 1",
                        "Test 2",
                        "Test 3", "Test 4", "Test 5", "Test 6"
                }))));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Touched item " + parent.getAdapter().getItem(position).toString(), Toast.LENGTH_SHORT).show();
            }
        });

        mListView.enableSwipeToDismiss(EnhancedListView.UndoStyle.MULTILEVEL_POPUP, new de.timroes.android.listview.EnhancedListView.OnDismissCallback() {
            @Override
            public de.timroes.android.listview.EnhancedListView.Undoable onDismiss(de.timroes.android.listview.EnhancedListView listView, final int position) {

                final HeaderViewListAdapter headerAdapter = (HeaderViewListAdapter) listView.getAdapter();
                final ArrayAdapter<String> adapter = (ArrayAdapter<String>) headerAdapter.getWrappedAdapter();

                //final String item = (String) mListView.getAdapter().getItem(position);
                final String item = adapter.getItem(position);
                adapter.remove(item);
                return new de.timroes.android.listview.EnhancedListView.Undoable() {
                    @Override
                    public void undo() {
                        adapter.insert(item, position);
                    }
                };
            }
        });

        //mListView.setSwipingLayout(R.id.text);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_delete) {
            mListView.delete(1);
            mListView.delete(2);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class EnhancedListAdapter extends ArrayAdapter<String> {

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         */
        public EnhancedListAdapter(Context context, int resource) {
            super(context, resource);
        }

        /**
         * Constructor
         *
         * @param context            The current context.
         * @param resource           The resource ID for a layout file containing a layout to use when
         *                           instantiating views.
         * @param textViewResourceId The id of the TextView within the layout resource to be populated
         */
        public EnhancedListAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects  The objects to represent in the ListView.
         */
        public EnhancedListAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        /**
         * Constructor
         *
         * @param context            The current context.
         * @param resource           The resource ID for a layout file containing a layout to use when
         *                           instantiating views.
         * @param textViewResourceId The id of the TextView within the layout resource to be populated
         * @param objects            The objects to represent in the ListView.
         */
        public EnhancedListAdapter(Context context, int resource, int textViewResourceId, String[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        /**
         * Constructor
         *
         * @param context  The current context.
         * @param resource The resource ID for a layout file containing a TextView to use when
         *                 instantiating views.
         * @param objects  The objects to represent in the ListView.
         */
        public EnhancedListAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        /**
         * Constructor
         *
         * @param context            The current context.
         * @param resource           The resource ID for a layout file containing a layout to use when
         *                           instantiating views.
         * @param textViewResourceId The id of the TextView within the layout resource to be populated
         * @param objects            The objects to represent in the ListView.
         */
        public EnhancedListAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item, parent, false);
            }

            TextView tv = (TextView) convertView.findViewById(R.id.text);
            tv.setText(getItem(position));

            Button btn = (Button)convertView.findViewById(R.id.btn);
            final Context ctx = convertView.getContext();
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(ctx, "Clicked on button", Toast.LENGTH_SHORT).show();
                }
            });

            return super.getView(position, convertView, parent);
        }
    }
}
