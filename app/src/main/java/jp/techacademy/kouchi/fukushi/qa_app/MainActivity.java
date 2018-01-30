package jp.techacademy.kouchi.fukushi.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    static final int GENRE_NONE = 0;
    static final int GENRE_HOBBY = 1;
    static final int GENRE_LIFE = 2;
    static final int GENRE_HEALTH = 3;
    static final int GENRE_COMPUTER = 4;
    static final int GENRE_FAVORITE = -1;

    private Toolbar mToolbar;

    private int mGenre = GENRE_NONE;

    // --- ここから ---
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private DatabaseReference mFavoriteRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;
    private HashMap<String, Query> mFavoriteQueryMap;


    // 質問に追加・変化があった時に受け取る
    private ChildEventListener mQuestionEventListener = new ChildEventListener() {
        // 質問が追加された時に呼ばれるメソッド
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
            String title = (String) map.get("title");
            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");
            String genre = (String) map.get("genre");
            String imageString = (String) map.get("image");
            byte[] bytes;
            if (imageString != null) {
                bytes = Base64.decode(imageString, Base64.DEFAULT);
            } else {
                bytes = new byte[0];
            }

            ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
            HashMap answerMap = (HashMap) map.get("answers");
            if (answerMap != null) {
                for (Object key : answerMap.keySet()) {
                    HashMap temp = (HashMap) answerMap.get((String) key);
                    String answerBody = (String) temp.get("body");
                    String answerName = (String) temp.get("name");
                    String answerUid = (String) temp.get("uid");
                    Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                    answerArrayList.add(answer);
                }
            }

            Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), Integer.valueOf(genre), bytes, answerArrayList);
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();
        }

        //
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを探す
            for (Question question: mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            question.getAnswers().add(answer);
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
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

    // お気に入りに追加・変化があった時に受け取る
    private ChildEventListener mFavoriteEventListener = new ChildEventListener() {
        // 質問が追加された時に呼ばれるメソッド
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            // 選択したジャンルにリスナーを登録する
            String genre = (String)dataSnapshot.getValue();
            String key = (String)dataSnapshot.getKey();
            // クエリーでお気に入りとキーが同じ質問を検索する
            Query query = mDatabaseReference.child(Const.ContentsPATH).child(genre).orderByKey().equalTo(key);
            query.addChildEventListener(mQuestionEventListener);
            // クエリーマップに追加
            mFavoriteQueryMap.put(key,query);
        }

        //
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            // リスナーを削除する
            String key = (String)dataSnapshot.getKey();
            Query query = mFavoriteQueryMap.get(key);
            query.removeEventListener(mQuestionEventListener);

            // クエリーマップから削除
            mFavoriteQueryMap.remove(key);
            // お気に入りを削除された質問をリストから探して削除
            for ( int i = 0; i < mQuestionArrayList.size();i++ ) {
                if ( mQuestionArrayList.get(i).getQuestionUid().equals(key)) {
                    mQuestionArrayList.remove(i);
                    mAdapter.notifyDataSetChanged();
                    break;
                }
            }
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
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 // ジャンルを選択していない場合（mGenre == GENRE_NONE）はエラーを表示するだけ
                // または(mGenre == GENRE_FAVORITE)
                if ( mGenre == GENRE_NONE ) {
                    Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }
                // ジャンルを選択していない場合(mGenre == GENRE_FAVORITE)はエラーを表示するだけ
                if ( mGenre == GENRE_FAVORITE ) {
                    Snackbar.make(view, "お気に入り以外のジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }

                // ログイン済みのユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // ジャンルを渡して質問作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }
            }
        });

        // ナビゲーションドロワーの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_hobby) {
                    mToolbar.setTitle("趣味");
                    mGenre = GENRE_HOBBY;
                } else if (id == R.id.nav_life) {
                    mToolbar.setTitle("生活");
                    mGenre = GENRE_LIFE;
                } else if (id == R.id.nav_health) {
                    mToolbar.setTitle("健康");
                    mGenre = GENRE_HEALTH;
                } else if (id == R.id.nav_compter) {
                    mToolbar.setTitle("コンピューター");
                    mGenre = GENRE_COMPUTER;
                } else if (id == R.id.nav_favorite ) {
                    mToolbar.setTitle("お気に入り");
                    mGenre = GENRE_FAVORITE;
                }

                setGenre();
                return true;
            }
        });
         // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();
        mFavoriteQueryMap = new HashMap<String, Query>();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });
    }

    private void setGenre() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear();
        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);

        // ジャンル別のリスナーを削除
        if (mGenreRef != null) {
            mGenreRef.removeEventListener(mQuestionEventListener);
        }
        // お気に入りのリスナーを削除
        if ( mFavoriteRef != null ) {
            mFavoriteRef.removeEventListener(mFavoriteEventListener);
        }

        if ( mGenre == GENRE_FAVORITE ) {
            // お気に入りが選択された場合
            // ログイン済みのユーザーを取得する
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                // お気に入りにリスナーを登録する
                mFavoriteRef = mDatabaseReference.child(Const.FavoritesPATH).child(user.getUid());
                mFavoriteRef.addChildEventListener(mFavoriteEventListener);
            }
        } else{
            // お気に入り以外のジャンルが選択された場合
            // お気に入りのクエリーが登録されていたら全要素削除
            if ( !mFavoriteQueryMap.isEmpty() ) {
                for (String key : mFavoriteQueryMap.keySet()) {
                    Query query = mFavoriteQueryMap.get(key);
                    query.removeEventListener(mQuestionEventListener);
                }
                mFavoriteQueryMap.clear();
            }
            // 選択したジャンルにリスナーを登録する
            mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
            mGenreRef.addChildEventListener(mQuestionEventListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();
        MenuItem menuFavorite = menu.findItem(R.id.nav_favorite);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // お気に入りメニュー表示
            menuFavorite.setVisible( true );
        }
        else {
            // ログインするまではお気に入りメニューを隠しておく
            menuFavorite.setVisible( false );
            // お気に入りが画面だった場合
            if ( mGenre == GENRE_FAVORITE ) {
                mToolbar.setTitle(getString(R.string.app_name));
                // ジャンルなしにする
                mGenre = GENRE_NONE;
                setGenre();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}