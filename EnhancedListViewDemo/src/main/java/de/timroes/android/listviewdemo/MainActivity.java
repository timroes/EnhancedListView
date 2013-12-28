/*
 * Copyright 2013 Tim Roes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.timroes.android.listviewdemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import de.timroes.android.listview.EnhancedListView;
import de.timroes.android.listview.RearrangementListener;

public class MainActivity extends ActionBarActivity {

    private enum ControlGroup {
		SWIPE_TO_DISMISS
    }

    private static final String PREF_UNDO_STYLE = "de.timroes.android.listviewdemo.UNDO_STYLE";
    private static final String PREF_SWIPE_TO_DISMISS = "de.timroes.android.listviewdemo.SWIPE_TO_DISMISS";
    private static final String PREF_SWIPE_DIRECTION = "de.timroes.android.listviewdemo.SWIPE_DIRECTION";
    private static final String PREF_SWIPE_LAYOUT = "de.timroes.android.listviewdemo.SWIPE_LAYOUT";

    private EnhancedListAdapter mAdapter;
    private EnhancedListView mListView;
    private DrawerLayout mDrawerLayout;

    private Bundle mUndoStylePref;
    private Bundle mSwipeDirectionPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View view, float v) { }

            @Override
            public void onDrawerOpened(View view) {
                mListView.discardUndo();
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View view) {
                supportInvalidateOptionsMenu();
                applySettings();
            }

            @Override
            public void onDrawerStateChanged(int i) { }

        });

        mListView = (EnhancedListView)findViewById(R.id.list);

        mAdapter = new EnhancedListAdapter();
        mAdapter.resetItems();

        mListView.setAdapter(mAdapter);

        CheckBox swipeToDismiss = (CheckBox) findViewById(R.id.pref_swipetodismiss);
        swipeToDismiss.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getPreferences(MODE_PRIVATE).edit().putBoolean(PREF_SWIPE_TO_DISMISS, isChecked).commit();
                enableControlGroup(ControlGroup.SWIPE_TO_DISMISS, isChecked);
            }
        });
        swipeToDismiss.setChecked(getPreferences(MODE_PRIVATE).getBoolean(PREF_SWIPE_TO_DISMISS, false));

        CheckBox swipeLayout = (CheckBox) findViewById(R.id.pref_swipelayout);
        swipeLayout.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getPreferences(MODE_PRIVATE).edit().putBoolean(PREF_SWIPE_LAYOUT, isChecked).commit();
            }
        });
        swipeLayout.setChecked(getPreferences(MODE_PRIVATE).getBoolean(PREF_SWIPE_LAYOUT, false));

        mUndoStylePref = new Bundle();
        mUndoStylePref.putInt(DialogPicker.DIALOG_TITLE, R.string.pref_undo_style_title);
        mUndoStylePref.putInt(DialogPicker.DIALOG_ITEMS_ID, R.array.undo_style);
        mUndoStylePref.putString(DialogPicker.DIALOG_PREF_KEY, PREF_UNDO_STYLE);

        mSwipeDirectionPref = new Bundle();
        mSwipeDirectionPref.putInt(DialogPicker.DIALOG_TITLE, R.string.pref_swipe_direction_title);
        mSwipeDirectionPref.putInt(DialogPicker.DIALOG_ITEMS_ID, R.array.swipe_direction);
        mSwipeDirectionPref.putString(DialogPicker.DIALOG_PREF_KEY, PREF_SWIPE_DIRECTION);

        enableControlGroup(ControlGroup.SWIPE_TO_DISMISS, getPreferences(MODE_PRIVATE).getBoolean(PREF_SWIPE_TO_DISMISS, false));

        // Set the callback that handles dismisses.
        mListView.setDismissCallback(new de.timroes.android.listview.EnhancedListView.OnDismissCallback() {
            /**
             * This method will be called when the user swiped a way or deleted it via
             * {@link de.timroes.android.listview.EnhancedListView#delete(int)}.
             *
             * @param listView The {@link EnhancedListView} the item has been deleted from.
             * @param position The position of the item to delete from your adapter.
             * @return An {@link de.timroes.android.listview.EnhancedListView.Undoable}, if you want
             *      to give the user the possibility to undo the deletion.
             */
            @Override
            public EnhancedListView.Undoable onDismiss(EnhancedListView listView, final int position) {

                final String item = (String) mAdapter.getItem(position);
                mAdapter.remove(position);
                return new EnhancedListView.Undoable() {
                    @Override
                    public void undo() {
                        mAdapter.insert(position, item);
                    }
                };
            }
        });

        // Show toast message on click on list items.
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Clicked on item " + mAdapter.getItem(position), Toast.LENGTH_SHORT).show();
            }
        });

        mListView.setSwipingLayout(R.id.swiping_layout);

        mListView.setArrayList(mAdapter.getItems());
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.enableRearranging();

        applySettings();

    }

    /**
     * Enables or disables a group of widgets in the settings drawer.
     *
     * @param group The Group that should be disabled/enabled.
     * @param enabled Whether the group should be enabled or not.
     */
    private void enableControlGroup(ControlGroup group, boolean enabled) {
        switch(group) {
            case SWIPE_TO_DISMISS:
                findViewById(R.id.pref_swipedirection).setEnabled(enabled);
                findViewById(R.id.pref_swipelayout).setEnabled(enabled);
                break;
        }
    }

    /**
     * Applies the settings the user has made to the list view.
     */
    private void applySettings() {

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        // Set the UndoStyle, the user selected.
        EnhancedListView.UndoStyle style;
        switch(prefs.getInt(PREF_UNDO_STYLE, 0)) {
            default: style = EnhancedListView.UndoStyle.SINGLE_POPUP; break;
            case 1: style = EnhancedListView.UndoStyle.MULTILEVEL_POPUP; break;
            case 2: style = EnhancedListView.UndoStyle.COLLAPSED_POPUP; break;
        }
        mListView.setUndoStyle(style);

        // Enable or disable Swipe to Dismiss
        if(prefs.getBoolean(PREF_SWIPE_TO_DISMISS, false)) {
            mListView.enableSwipeToDismiss();

            // Set the swipe direction
            EnhancedListView.SwipeDirection direction;
            switch(prefs.getInt(PREF_SWIPE_DIRECTION, 0)) {
                default: direction = EnhancedListView.SwipeDirection.BOTH; break;
                case 1: direction = EnhancedListView.SwipeDirection.START; break;
                case 2: direction = EnhancedListView.SwipeDirection.END; break;
            }
            mListView.setSwipeDirection(direction);

            // Enable or disable swiping layout feature
            mListView.setSwipingLayout(prefs.getBoolean(PREF_SWIPE_LAYOUT, false)
                    ? R.id.swiping_layout : 0);

        } else {
            mListView.disableSwipeToDismiss();
        }

    }

    @Override
    protected void onStop() {
        if(mListView != null) {
            mListView.discardUndo();
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean drawer = mDrawerLayout.isDrawerVisible(Gravity.RIGHT);

        menu.findItem(R.id.action_settings).setVisible(!drawer);
        menu.findItem(R.id.action_done).setVisible(drawer);

        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                mDrawerLayout.openDrawer(Gravity.RIGHT);
                return true;
            case R.id.action_done:
                mDrawerLayout.closeDrawers();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void resetItems(View view) {
        mListView.discardUndo();
        mAdapter.resetItems();
        mDrawerLayout.closeDrawers();
    }

    public void selectUndoStyle(View view) {
        DialogPicker picker = new DialogPicker();
        picker.setArguments(mUndoStylePref);
        picker.show(getSupportFragmentManager(), "UNDO_STYLE_PICKER");
    }

    public void selectSwipeDirection(View view) {
        DialogPicker picker = new DialogPicker();
        picker.setArguments(mSwipeDirectionPref);
        picker.show(getSupportFragmentManager(), "SWIPE_DIR_PICKER");
    }

    private class EnhancedListAdapter extends BaseAdapter implements RearrangementListener {

        final int INVALID_ID = -1;
        private ArrayList<String> mItems = new ArrayList<String>();
        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public void onStartedRearranging(){}

        public void swapElements(int indexOne, int indexTwo){
            String temp1 = mItems.get(indexOne);
            String temp2 = mItems.get(indexTwo);

            mItems.remove(indexOne);
            mItems.add(indexOne, temp2);

            mItems.remove(indexTwo);
            mItems.add(indexTwo, temp1);
        }

        public void onFinishedRearranging(){}

        ArrayList getItems(){
            return mItems;
        }

        void resetItems() {
            mItems.clear();
            for(int i = 1; i <= 40; i++) {
                mItems.add("Item " + i);
            }

            mIdMap.clear();
            for (int i = 0; i < mItems.size(); ++i) {
                mIdMap.put(mItems.get(i), i);
            }

            notifyDataSetChanged();
        }

        public void remove(int position) {
            mItems.remove(position);
            notifyDataSetChanged();
        }

        public void insert(int position, String item) {
            mItems.add(position, item);
            notifyDataSetChanged();
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return mItems.size();
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
        * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            if (position < 0 || position >= mIdMap.size()) {
                return INVALID_ID;
            }
            Object item = getItem(position);
            return mIdMap.get(item);
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item, parent, false);
                // Clicking the delete icon, will read the position of the item stored in
                // the tag and delete it from the list. So we don't need to generate a new
                // onClickListener every time the content of this view changes.
                final View origView = convertView;
                convertView.findViewById(R.id.action_delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListView.delete(((ViewHolder)origView.getTag()).position);
                    }
                });

                holder = new ViewHolder();
                assert convertView != null;
                holder.mTextView = (TextView) convertView.findViewById(R.id.text);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.position = position;
            holder.mTextView.setText(mItems.get(position));

            return convertView;
        }

        private class ViewHolder {
            TextView mTextView;
            int position;
        }

    }

    private class DialogPicker extends DialogFragment {

        final static String DIALOG_TITLE = "dialog_title";
        final static String DIALOG_ITEMS_ID = "items_id";
        final static String DIALOG_PREF_KEY = "pref_key";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(args.getInt(DIALOG_TITLE));
            builder.setSingleChoiceItems(
                args.getInt(DIALOG_ITEMS_ID),
                getPreferences(MODE_PRIVATE).getInt(args.getString(DIALOG_PREF_KEY), 0),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                            prefs.edit().putInt(args.getString(DIALOG_PREF_KEY), which).commit();
                            dialog.dismiss();
                        }
                    }
            );

            return builder.create();
        }

    }
}
