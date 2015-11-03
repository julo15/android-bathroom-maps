package com.apolloyang.bathroommaps.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.apolloyang.bathroommaps.R;
import com.apolloyang.bathroommaps.model.BathroomMapsAPI;

/**
 * Created by julianlo on 10/22/15.
 */
public class ReviewsDialogFragment extends DialogFragment {

    private View mRootView;
    private BathroomMapsAPI.Bathroom mBathroom;
    private AddBathroomDialogFragment.Listener mListener;

    public ReviewsDialogFragment(BathroomMapsAPI.Bathroom bathroom, AddBathroomDialogFragment.Listener listener) {
        mBathroom = bathroom;
        mListener = listener;

        // HACK: Basically using setRetainInstance to prevent the crash caused by this fragment not
        // having a default constructor. Because we don't also have the workaround to clear out the
        // dismiss message, the end result is that the dialog disappears on rotation. This is fine,
        // since we don't currently have the right MainActivity implementation to easily support
        // re-passing in the Bathrom and Listener.
        setRetainInstance(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final DialogFragment dialogFragment = this;
        final Context context = getActivity();
        mRootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_reviews, null);

        ListView listView = (ListView)mRootView.findViewById(R.id.reviews_listview);
        listView.setAdapter(new ReviewAdapter(getActivity(), mBathroom));

        mRootView.findViewById(R.id.addreview_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddBathroomDialogFragment reviewDialogFragment = new AddBathroomDialogFragment(AddBathroomDialogFragment.Mode.ADDREVIEW, null, mBathroom.getId(), mListener);
                reviewDialogFragment.show(dialogFragment.getFragmentManager(), "addbathroom" /* TODO: Fix */);
                dialogFragment.dismiss();
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setView(mRootView)
                .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogFragment.dismiss();
                    }
                })
                .create();
    }

    private static class ReviewAdapter extends ArrayAdapter<BathroomMapsAPI.Review> {
        private Context mContext;

        public ReviewAdapter(Context context, BathroomMapsAPI.Bathroom bathroom) {
            super(context, R.layout.row_review, bathroom.getReviews());
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row_review, parent, false);
            RatingBar ratingBar = (RatingBar)rowView.findViewById(R.id.rating_ratingbar);
            TextView reviewTextView = (TextView)rowView.findViewById(R.id.review_textview);

            BathroomMapsAPI.Review review = getItem(position);
            ratingBar.setRating(review.getRating());
            reviewTextView.setText(review.getText());

            return rowView;
        }
    }
}
