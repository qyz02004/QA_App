package jp.techacademy.kouchi.fukushi.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class QuestionDetailActivity extends AppCompatActivity {

    private ListView mListView;
    Question mQuestion;
    private QuestionDetailListAdapter mAdapter;

    private DatabaseReference mAnswerRef;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mDatabaseFavoriteRef;
    private boolean mFavorite;

    // firebaseのお気に入りを監視するリスナー
    private ChildEventListener mFavoriteEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            String key = dataSnapshot.getKey();
            String QuestionUid = mQuestion.getQuestionUid();
            if (key.equals(QuestionUid)){
                mFavorite = true;
                FloatingActionButton favoriteButton = (FloatingActionButton) findViewById(R.id.fab2);
                favoriteButton.setImageResource(R.drawable.ic_star_black_24dp);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            String key = dataSnapshot.getKey();
            String QuestionUid = mQuestion.getQuestionUid();
            if (key.equals(QuestionUid)){
                mFavorite = false;
                FloatingActionButton favoriteButton = (FloatingActionButton) findViewById(R.id.fab2);
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

    // firebaseの質問の答えを監視するリスナー
    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for(Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");

        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ログイン済みのユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // Questionを渡して回答作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                }
            }
        });

        DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);

        // お気に入りボタン
        FloatingActionButton favoriteButton = (FloatingActionButton) findViewById(R.id.fab2);
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String QuestionUid = mQuestion.getQuestionUid();
                String genre = String.valueOf(mQuestion.getGenre());
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

    @Override
    protected void onResume(){
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FloatingActionButton favoriteButton = (FloatingActionButton) findViewById(R.id.fab2);
        if (user == null) {
            // ログインしていない
            // お気に入りボタンを隠す
            favoriteButton.hide();
            if ( mDatabaseFavoriteRef != null ) {
                // お気に入りリスナーを削除
                mDatabaseFavoriteRef.removeEventListener(mFavoriteEventListener);
            }
        } else {
            // ログイン済み
            // お気に入りボタンを表示
            favoriteButton.show();
            if ( mDatabaseFavoriteRef != null ) {
                // 前回登録したお気に入りリスナーを削除
                mDatabaseFavoriteRef.removeEventListener(mFavoriteEventListener);
            }
            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
            mDatabaseFavoriteRef = mDatabaseReference.child(Const.FavoritesPATH).child(user.getUid());
            // お気に入りリスナーを登録
            mDatabaseFavoriteRef.addChildEventListener(mFavoriteEventListener);
        }
    }
}