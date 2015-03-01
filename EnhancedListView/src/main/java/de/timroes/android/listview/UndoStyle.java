package de.timroes.android.listview;

/**
 * Defines the style in which <i>undos</i> should be displayed and handled in the list.
 * Pass this to {@link de.timroes.android.listview.EnhancedListView#setUndoStyle(UndoStyle)}
 * to change the default behavior from {@link #SINGLE_POPUP}.
 */
public enum UndoStyle {

    /**
     * Shows a popup window, that allows the user to undo the last
     * dismiss. If another element is deleted, the undo popup will undo that deletion.
     * The user is only able to undo the last deletion.
     */
    SINGLE_POPUP,

    /**
     * Shows a popup window, that allows the user to undo the last dismiss.
     * If another item is deleted, this will be added to the chain of undos. So pressing
     * undo will undo the last deletion, pressing it again will undo the deletion before that,
     * and so on. As soon as the popup vanished (e.g. because {@link de.timroes.android.listview.EnhancedListView#setUndoHideDelay(int) autoHideDelay}
     * is over) all saved undos will be discarded.
     */
    MULTILEVEL_POPUP,

    /**
     * Shows a popup window, that allows the user to undo the last dismisses.
     * If another item is deleted, while there is still an undo popup visible, the label
     * of the button changes to <i>Undo all</i> and a press on the button, will discard
     * all stored undos. As soon as the popup vanished (e.g. because {@link de.timroes.android.listview.EnhancedListView#setUndoHideDelay(int) autoHideDelay}
     * is over) all saved undos will be discarded.
     */
    COLLAPSED_POPUP

}