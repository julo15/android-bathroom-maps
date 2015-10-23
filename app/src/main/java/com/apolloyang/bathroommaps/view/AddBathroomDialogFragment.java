package com.apolloyang.bathroommaps.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.apolloyang.bathroommaps.R;
import com.apolloyang.bathroommaps.model.BathroomMapsAPI;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by julianlo on 10/12/15.
 */
public class AddBathroomDialogFragment extends DialogFragment {

    private Mode mMode;
    private String mId;
    private LatLng mPosition;
    private View mRootView;

    public enum Mode {
        ADDBATHROOM,
        ADDREVIEW,
    }

    public AddBathroomDialogFragment(Mode mode, LatLng position, String id) {
        mPosition = position;
        mMode = mode;
        mId = id;
    }

    public void setMode() {
        mRootView.findViewById(R.id.name_layout).setVisibility(mMode == Mode.ADDBATHROOM ? View.VISIBLE : View.GONE);
        mRootView.findViewById(R.id.category_layout).setVisibility(mMode == Mode.ADDBATHROOM ? View.VISIBLE : View.GONE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final DialogFragment dialogFragment = this;
        final Context context = getActivity();
        mRootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_addbathroom, null);
        setMode();

        return new AlertDialog.Builder(getActivity())
                .setView(mRootView)
                .setPositiveButton(R.string.action_addbathroom, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE: {
                                switch (mMode) {
                                    case ADDBATHROOM: {
                                        AddBathroomTask task = new AddBathroomTask(getActivity(), mPosition,
                                                ((EditText)mRootView.findViewById(R.id.name_edittext)).getText().toString(),
                                                ((EditText)mRootView.findViewById(R.id.category_edittext)).getText().toString());
                                        task.execute();
                                        break;
                                    }

                                    case ADDREVIEW: {
                                        AddReviewTask task = new AddReviewTask(getActivity(), mId,
                                                Integer.parseInt(((EditText)mRootView.findViewById(R.id.rating_edittext)).getText().toString()),
                                                ((EditText)mRootView.findViewById(R.id.review_edittext)).getText().toString());
                                        task.execute();
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogFragment.dismiss();
                    }
                })
                .create();
    }

    // Parameters
    // Progress
    // Result
    private static class AddBathroomTask extends AsyncTask<Void, Void, String> {

        private Context mContext;
        private LatLng mPosition;
        private String mName;
        private String mCategory;

        public AddBathroomTask(Context context, LatLng position, String name, String category) {
            mContext = context;
            mPosition = position;
            mName = name;
            mCategory = category;
        }

        @Override
        protected String doInBackground(Void... params) {
            BathroomMapsAPI api = new BathroomMapsAPI();
            try {
                return api.addBathroom(mPosition, mName, mCategory);
            } catch (Exception e) {
                System.err.println(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast toast = Toast.makeText(mContext, result, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    // Parameters
    // Progress
    // Result
    private static class AddReviewTask extends AsyncTask<Void, Void, String> {

        private Context mContext;
        private String mId;
        private int mRating;
        private String mText;

        public AddReviewTask(Context context, String id, int rating, String text) {
            mContext = context;
            mId = id;
            mRating = rating;
            mText = text;
        }

        @Override
        protected String doInBackground(Void... params) {
            BathroomMapsAPI api = new BathroomMapsAPI();
            try {
                return api.addReview(mId, mRating, mText);
            } catch (Exception e) {
                System.err.println(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast toast = Toast.makeText(mContext, result, Toast.LENGTH_LONG);
            toast.show();
        }
    }
}
