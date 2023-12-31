import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zoho.people.utils.others.Util;

@SuppressWarnings("rawtypes") //No i18n
public class RecyclerHeadersDecoration extends RecyclerView.ItemDecoration {

	private final StickyRecyclerHeadersInterface mAdapter;
	private final LongSparseArray<View> mHeaderViews = new LongSparseArray<View>();
	private final SparseArray<Rect> mHeaderRects = new SparseArray<Rect>();


	public RecyclerHeadersDecoration(StickyRecyclerHeadersInterface adapter) {
		mAdapter = adapter;
	}

	/**
	 * Returns the first item currently in the recyclerview that's not obscured by a header.
	 * @param parent
	 * @return
	 */

	private int getOrientation(RecyclerView parent) {
		if (parent.getLayoutManager() instanceof LinearLayoutManager) {
			LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
			return layoutManager.getOrientation();
		} else {
			throw new IllegalStateException("Use linear layout manager"); // No i18n
		}
	}

	private View getNextView(RecyclerView parent) {
		View firstView = parent.getChildAt(0);
		// draw the first visible child's header at the top of the view
		int firstPosition = parent.getChildAdapterPosition(firstView);
		if (firstPosition == NO_POSITION) {
			return null;
		}
		View firstHeader = getHeaderView(parent, firstPosition);
		for (int i = 0; i < parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);
			RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
			if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
				if (child.getTop() - layoutParams.topMargin > firstHeader.getHeight()) {
					return child;
				}
			} else {
				if (child.getLeft() - layoutParams.leftMargin > firstHeader.getWidth()) {
					return child;
				}
			}
		}
		return null;
	}



	@Override
	public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.onDrawOver(canvas, parent, state);
		int orientation = getOrientation(parent);
		mHeaderRects.clear();

		if (parent.getChildCount() > 0 && mAdapter.getItemCount() > 0) {
			// draw the first visible child's header at the top of the view
			View firstView = parent.getChildAt(0);
			int firstPosition = parent.getChildAdapterPosition(firstView);
			if (firstPosition != NO_POSITION) {
				View firstHeader = getHeaderView(parent, firstPosition);
				View nextView = getNextView(parent);
				int translationX = Math.max(parent.getChildAt(0).getLeft() - firstHeader.getWidth(), 0);
				int translationY = Math.max(parent.getChildAt(0).getTop() - firstHeader.getHeight(), 0);
				int nextPosition = parent.getChildAdapterPosition(nextView);
				if (nextPosition != NO_POSITION && hasNewHeader(nextPosition)) {
					View secondHeader = getHeaderView(parent, nextPosition);
					//Translate the topmost header so the next header takes its place, if applicable
					if (orientation == LinearLayoutManager.VERTICAL &&
							nextView.getTop() - secondHeader.getHeight() - firstHeader.getHeight() < 0) {
						translationY += nextView.getTop() - secondHeader.getHeight() - firstHeader.getHeight();
					} else if (orientation == LinearLayoutManager.HORIZONTAL &&
							nextView.getLeft() - secondHeader.getWidth() - firstHeader.getWidth() < 0) {
						translationX += nextView.getLeft() - secondHeader.getWidth() - firstHeader.getWidth();
					}
				}
				canvas.save();
				canvas.translate(translationX, translationY);
				firstHeader.draw(canvas);
				canvas.restore();
				mHeaderRects.put(firstPosition, new Rect(translationX, translationY,
						translationX + firstHeader.getWidth(), translationY + firstHeader.getHeight()));
			}
			for (int i = 1; i < parent.getChildCount(); i++) {
				int position = parent.getChildAdapterPosition(parent.getChildAt(i));
				if (position == NO_POSITION) {
					continue;
				}
				if (hasNewHeader(position)) {
					// this header is different than the previous, it must be drawn in the correct place
					int translationX = 0;
					int translationY = 0;
					View header = getHeaderView(parent, position);
					if (orientation == LinearLayoutManager.VERTICAL) {
						translationY = parent.getChildAt(i).getTop() - header.getHeight();
					} else {
						translationX = parent.getChildAt(i).getLeft() - header.getWidth();
					}
					canvas.save();
					canvas.translate(translationX, translationY);
					header.draw(canvas);
					canvas.restore();
					mHeaderRects.put(position, new Rect(translationX, translationY,
							translationX + header.getWidth(), translationY + header.getHeight()));
				}
			}
		}
	}


	/**
	 * Gets the position of the header under the specified (x, y) coordinates.
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return position of header, or -1 if not found
	 */
	public int findHeaderPositionUnder(int x, int y) {
		for (int i = 0; i < mHeaderRects.size(); i++) {
			Rect rect = mHeaderRects.get(mHeaderRects.keyAt(i));
			if (rect.contains(x, y)) {
				return mHeaderRects.keyAt(i);
			}
		}
		return -1;
	}

	private int getFirstHeaderPosition() {
		if (mAdapter.getItemCount() > 0) {
			return 0;
		}
		return -1;
	}
	/**
	 * Gets the header view for the associated position.  If it doesn't exist yet, it will be
	 * created, measured, and laid out.
	 * @param parent
	 * @param position
	 * @return Header view
	 */

	@SuppressWarnings("unchecked") // No i18n
	public View getHeaderView(RecyclerView parent, int position) {
		if (position >= mAdapter.getItemCount()) {
			position = mAdapter.getItemCount() - 1;
		}
		if (position == NO_POSITION) {
			Util.throwException(new IndexOutOfBoundsException("Position shouldn't be NO_POSITION (-1)")); // no i18n
		}
		long headerId = mAdapter.getHeaderId(position);

		View header = mHeaderViews.get(headerId);
		if (header == null) {
			RecyclerView.ViewHolder viewHolder = mAdapter.onCreateHeaderViewHolder(parent);
			mAdapter.onBindHeaderViewHolder(viewHolder, position);
			header = viewHolder.itemView;
			if (header.getLayoutParams() == null) {
				header.setLayoutParams(new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}

			int widthSpec;
			int heightSpec;

			if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
				widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
				heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
			} else {
				widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.UNSPECIFIED);
				heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.EXACTLY);
			}

			int childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
					parent.getPaddingLeft() + parent.getPaddingRight(), header.getLayoutParams().width);
			int childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
					parent.getPaddingTop() + parent.getPaddingBottom(), header.getLayoutParams().height);
			header.measure(childWidth, childHeight);
			header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
			mHeaderViews.put(headerId, header);
		}
		return header;
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);
		int orientation = getOrientation(parent);
		int itemPosition = parent.getChildAdapterPosition(view);
		if (itemPosition == NO_POSITION) {
			return;
		}
		getItemOffsetsWithPosition(outRect, view, parent, state, itemPosition);
	}

	public void getItemOffsetsWithPosition(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state, int itemPosition) {
		int orientation = getOrientation(parent);

		if (hasNewHeader(itemPosition)) {
			View header = getHeaderView(parent, itemPosition);
			if (orientation == LinearLayoutManager.VERTICAL) {
				outRect.top = header.getHeight();
			} else {
				outRect.left = header.getWidth();
			}
		}
	}

	public boolean hasNewHeader(int position) {
		if (getFirstHeaderPosition() == position) {
			return true;
		} else if (position >= 1 && position < mAdapter.getItemCount()) {
			return mAdapter.getHeaderId(position) != mAdapter.getHeaderId(position - 1);
		} else {
			return false;
		}
	}
}
