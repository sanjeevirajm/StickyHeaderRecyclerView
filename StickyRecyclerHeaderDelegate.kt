/* $Id$ */
package com.zoho.people.utils.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface StickyRecyclerHeaderDelegate<T : RecyclerView.ViewHolder?> {
    /**
     * Get the ID of the header associated with this item.  For example, if your headers group
     * items by their first letter, you could return the character representation of the first letter.
     * Return a value < 0 if the view should not have a header (like, a header view or footer view)
     * @param position
     * @return
     */
    fun getHeaderId(position: Int): Long

    /**
     * Creates a new ViewHolder for a header.  This works the same way onCreateViewHolder in
     * Recycler.Adapter, ViewHolders can be reused for different views.  This is usually a good place
     * to inflate the layout for the header.
     * @param parent
     * @return
     */
    fun onCreateHeaderViewHolder(parent: ViewGroup?): T

    /**
     * Binds an existing ViewHolder to the specified adapter position.
     * @param holder
     * @param position
     */
    fun onBindHeaderViewHolder(holder: T, position: Int)
    fun getItemCount(): Int
}
