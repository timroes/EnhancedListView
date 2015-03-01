package de.timroes.android.listview;

/**
 * The callback interface used by {@link de.timroes.android.listview.EnhancedListView#setDismissCallback(OnDismissCallback)}
 * to inform its client about a successful dismissal of one or more list item positions.
 * Implement this to remove items from your adapter, that has been swiped from the list.
 */
public interface OnDismissCallback {

    /**
     * Called when the user has deleted an item from the list. The item has been deleted from
     * the {@code listView} at {@code position}. Delete this item from your adapter.
     * <p>
     * Don't return from this method, before your item has been deleted from the adapter, meaning
     * if you delete the item in another thread, you have to make sure, you don't return from
     * this method, before the item has been deleted. Since the way how you delete your item
     * depends on your data and adapter, the {@link de.timroes.android.listview.EnhancedListView}
     * cannot handle that synchronizing for you. If you return from this method before you removed
     * the view from the adapter, you will most likely get errors like exceptions and flashing
     * items in the list.
     * <p>
     * If the user should get the possibility to undo this deletion, return an implementation
     * of {@link de.timroes.android.listview.Undoable} from this method.
     * If you return {@code null} no undo will be possible. You are free to return an {@code Undoable}
     * for some items, and {@code null} for others, though it might be a horrible user experience.
     *
     * @param listView The {@link EnhancedListView} the item has been deleted from.
     * @param position The position of the item to delete from your adapter.
     * @return An {@link de.timroes.android.listview.Undoable}, if you want
     *      to give the user the possibility to undo the deletion.
     */
    Undoable onDismiss(EnhancedList listView, int position);

}