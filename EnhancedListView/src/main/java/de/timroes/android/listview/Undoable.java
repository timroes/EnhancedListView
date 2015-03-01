package de.timroes.android.listview;


/**
 * Extend this abstract class and return it from
 * {@link OnDismissCallback#onDismiss(EnhancedListView, int)}
 * to let the user undo the deletion you've done with your {@link OnDismissCallback}.
 * You have at least to implement the {@link #undo()} method, and can override {@link #discard()}
 * and {@link #getTitle()} to offer more functionality. See the README file for example implementations.
 */
public abstract class Undoable {

    /**
     * This method must undo the deletion you've done in
     * {@link OnDismissCallback#onDismiss(EnhancedListView, int)} and reinsert
     * the element into the adapter.
     * <p>
     * In the most implementations, you will only remove the list item from your adapter
     * in the {@code onDismiss} method and delete it from the database (or your permanent
     * storage) in {@link #discard()}. In that case you only need to reinsert the item
     * to the adapter.
     */
    public abstract void undo();

    /**
     * Returns the individual undo message for this undo. This will be displayed in the undo
     * window, beside the undo button. The default implementation returns {@code null},
     * what will lead in a default message to be displayed in the undo window.
     * Don't call the super method, when overriding this method.
     *
     * @return The title for a special string.
     */
    public String getTitle() {
        return null;
    }

    /**
     * Discard the undo, meaning the user has no longer the possibility to undo the deletion.
     * Implement this, to finally delete your stuff from permanent storages like databases
     * (whereas in {@link de.timroes.android.listview.OnDismissCallback#onDismiss(EnhancedListView, int)}
     * you should only remove it from the list adapter).
     */
    public void discard() { }

}