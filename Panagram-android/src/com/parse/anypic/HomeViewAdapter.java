package com.parse.anypic;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import java.util.List;

/*
 * The HomeViewAdapter is an extension of ParseQueryAdapter
 * that has a custom layout for Anypic photos in the home
 * list view.
 */

public class HomeViewAdapter extends ParseQueryAdapter<Photo> {

    public HomeViewAdapter(Context context) {
        super(context, new ParseQueryAdapter.QueryFactory<Photo>() {
            public ParseQuery<Photo> create() {

                ParseQuery<Photo> query = new ParseQuery<Photo>("Photo");
                query.whereExists("image");
                query.include("user");
                query.orderByDescending("createdAt");

                return query;
            }
        });
    }

    /**
     * This class is overridden to provide a custom view for each item in the
     * Home List View. It sets the user's profile picture, their user name,
     * and then displays the actual photo.
     *
     * See home_list_item.xml for the layout file
     *
     * @see com.parse.ParseQueryAdapter#getItemView(com.parse.ParseObject, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getItemView(final Photo photo, View v, ViewGroup parent) {

        if (v == null) {
            v = View.inflate(getContext(), R.layout.home_list_item, null);
        }

        super.getItemView(photo, v, parent);

        // Set up the user's profile picture
        ImageView fbPhotoView = (ImageView) v.findViewById(R.id.user_thumbnail);
        ParseUser user = photo.getUser();
        Picasso.with(getContext())
            .load("https://graph.facebook.com/" + user.getString("facebookId") + "/picture?type=square")
            .into(fbPhotoView);

        // Set up the username
        TextView usernameView = (TextView) v.findViewById(R.id.user_name);
        usernameView.setText((String) user.get("displayName"));

        // Set up the actual photo
        ImageView anypicPhotoView = (ImageView) v.findViewById(R.id.photo);
        ParseFile photoFile = photo.getImage();

        // TODO (future) - get image bitmap, then set the image view with setImageBitmap()
        // we can use the decodeBitmap tricks to reduce the size to save memory

        if (photoFile != null) {
            Picasso.with(getContext())
                .load(photoFile.getUrl())
                .placeholder(new ColorDrawable(Color.LTGRAY))
                .into(anypicPhotoView);
        } else { // Clear ParseImageView if an object doesn't have a photo
            anypicPhotoView.setImageResource(android.R.color.transparent);
        }

        final TextView likeCount = (TextView) v.findViewById(R.id.like_count);

        ParseQuery<Activity> likeQuery = new ParseQuery<Activity>("Activity");
        likeQuery.whereEqualTo("type", "like");
        likeQuery.include("fromUser");
        likeQuery.whereExists("photo");
        likeQuery.whereEqualTo("photo", photo);
        likeQuery.findInBackground(new FindCallback<Activity>() {

            @Override
            public void done(List<Activity> activities, ParseException e) {
                boolean isLiked = false;

                likeCount.setText(String.valueOf(activities.size()));
                for (Activity activity : activities) {
                    if (activity.getFromUser().getUsername()
                            .equals(ParseUser.getCurrentUser().getUsername())) {
                        isLiked = true;
                    }
                }

                if (isLiked) {
                    setLiked(likeCount);
                } else {
                    setUnliked(likeCount, photo);
                }
            }
        });


//        final ImageView iv=anypicPhotoView;
//        ViewTreeObserver vto = iv.getViewTreeObserver();
//        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            public boolean onPreDraw() {
//                Log.i(AnypicApplication.TAG, "*** Photo height: " + iv.getMeasuredHeight() + " width: " + iv.getMeasuredWidth());
//                return true;
//            }
//        });
        return v;
    }

    public void setUnliked(TextView v, final Photo photo) {
        v.setClickable(true);
        v.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like, 0, 0, 0);

        v.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.LTGRAY);
                } else {
                    v.setBackgroundColor(Color.TRANSPARENT);
                }
                return false;
            }
        });

        v.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                TextView likeView = (TextView) v;
                int like = Integer.valueOf((String) likeView.getText());
                likeView.setText(String.valueOf(++like));

                setLiked(likeView);

                Activity likeActivity = new Activity();
                likeActivity.setFromUser(ParseUser.getCurrentUser());
                likeActivity.setToUser(photo.getUser());
                likeActivity.setPhoto(photo);
                likeActivity.setType("like");
                likeActivity.saveEventually();
            }
        });
    }

    public void setLiked(TextView v) {
        v.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
        v.setClickable(false);
        v.setOnTouchListener(null);
    }

}
