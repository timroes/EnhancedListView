package de.timroes.android.listviewdemo;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

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


        mListView.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, R.id.text,
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

        mListView.enableSwipeToDismiss(EnhancedListView.UndoStyle.MULTILEVEL_POPUP, new EnhancedListView.OnDismissCallback() {
            @Override
            public EnhancedListView.Undoable onDismiss(EnhancedListView listView, final int position) {

                final HeaderViewListAdapter headerAdapter = (HeaderViewListAdapter) listView.getAdapter();
                final ArrayAdapter<String> adapter = (ArrayAdapter<String>) headerAdapter.getWrappedAdapter();

                //final String item = (String) mListView.getAdapter().getItem(position);
                final String item = adapter.getItem(position);
                adapter.remove(item);
                return new EnhancedListView.Undoable() {
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

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     * <p/>
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.</p>
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_delete) {
            mListView.delete(1);
            mListView.delete(2);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
