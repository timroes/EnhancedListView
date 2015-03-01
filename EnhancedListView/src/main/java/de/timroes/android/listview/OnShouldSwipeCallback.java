package de.timroes.android.listview;

/**
 * The callback interface used by {@link de.timroes.android.listview.EnhancedListView#setShouldSwipeCallback(OnShouldSwipeCallback)}
 * to inform its client that a list item is going to be swiped and check whether is
 * should or not. Implement this to prevent some items from be swiped.
 */
public interface OnShouldSwipeCallback {

    /**
     * Called when the user is swiping an item from the list.
     * <p>
     * If the user should get the possibility to swipe the item, return true.
     * Otherwise, return false to disable swiping for this item.
     *
     * @param listView The {@link EnhancedListView} the item is wiping from.
     * @param position The position of the item to swipe in your adapter.
     * @return Whether the item should be swiped or not.
     */
    boolean onShouldSwipe(EnhancedList listView, int position);

}