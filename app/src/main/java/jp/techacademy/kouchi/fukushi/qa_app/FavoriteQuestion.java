package jp.techacademy.kouchi.fukushi.qa_app;

import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

/**
 * Created by kouchi on 2018/01/28.
 */

public class FavoriteQuestion {

    private QuestionDetailActivity mActivity;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mDatabaseFavoriteRef;
    private boolean mFavorite;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            String key = dataSnapshot.getKey();
            String QuestionUid = mActivity.mQuestion.getQuestionUid();
            if (key.equals(QuestionUid)){
                 mFavorite = true;
                 FloatingActionButton favoriteButton = (FloatingActionButton) mActivity.findViewById(R.id.fab2);
                 favoriteButton.setImageResource(R.drawable.ic_star_black_24dp);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            String key = dataSnapshot.getKey();
            String QuestionUid = mActivity.mQuestion.getQuestionUid();
            if (key.equals(QuestionUid)){
                mFavorite = false;
                FloatingActionButton favoriteButton = (FloatingActionButton) mActivity.findViewById(R.id.fab2);
                favoriteButton.setImageResource(R.drawable.ic_star_white_24dp);
            }

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    public FavoriteQuestion(QuestionDetailActivity activity) {
        mActivity = activity;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FloatingActionButton favoriteButton = (FloatingActionButton) activity.findViewById(R.id.fab2);
        if (user == null) {
            favoriteButton.hide();
        } else {
            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
            mDatabaseFavoriteRef = mDatabaseReference.child(Const.FavoritesPATH).child(user.getUid());
            mDatabaseFavoriteRef.addChildEventListener(mEventListener);
            favoriteButton.show();
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String QuestionUid = mActivity.mQuestion.getQuestionUid();
                    String genre = String.valueOf(mActivity.mQuestion.getGenre());
                    if ( mFavorite == false ){
                        // お気に入り解除→登録
                        mDatabaseFavoriteRef.child(QuestionUid).setValue(genre);
                    } else {
                        // お気に入り登録→解除
                        mDatabaseFavoriteRef.child(QuestionUid).removeValue();
                    }
                }
            });
        }
    }

}
