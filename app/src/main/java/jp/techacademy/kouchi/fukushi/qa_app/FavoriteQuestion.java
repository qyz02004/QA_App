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

/**
 * Created by kouchi on 2018/01/28.
 */

public class FavoriteQuestion {

    private QuestionDetailActivity mActivity;


    public FavoriteQuestion(QuestionDetailActivity activity) {
    }

}
