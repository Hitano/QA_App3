package jp.techacademy.hiroshi.tanooka.qa_app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.HashMap;

public class FavoriteActivity extends AppCompatActivity {

    HashMap mFavData;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mFavoriteRef;
    private DatabaseReference mQuestionRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionListAdapter mAdapter;

    private FirebaseUser user;

    private ChildEventListener mFavListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap)dataSnapshot.getValue();
            String qUid = (String)map.get("questionUid");
            mFavData.put(qUid, qUid);
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
    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap)dataSnapshot.getValue();
            String questionUid =  dataSnapshot.getKey();
            for (Object key : mFavData.keySet()) {
                if (questionUid.equals(mFavData.get(key).toString())) {
                    String title = (String)map.get("title");
                    String body = (String)map.get("body");
                    String name = (String)map.get("name");
                    String uid = (String)map.get("uid");
                    String imageString = (String)map.get("image");
                    byte[] bytes;
                    if (imageString != null) {
                        bytes = Base64.decode(imageString, Base64.DEFAULT);
                    } else {
                        bytes = new byte[0];
                    }

                    ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                    HashMap answerMap = (HashMap)map.get("answers");
                    if (answerMap != null) {
                        for (Object answerKey : answerMap.keySet()) {
                            HashMap temp = (HashMap)answerMap.get((String)answerKey);
                            String answerBody = (String)temp.get("body");
                            String answerName = (String)temp.get("name");
                            String answerUid = (String)temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String)answerKey);
                            answerArrayList.add(answer);
                        }
                    }

                    Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), 5, bytes, answerArrayList);
                    mQuestionArrayList.add(question);
                    mAdapter.notifyDataSetChanged();
                }
            }
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
        setContentView(R.layout.activity_favorite);
        Toolbar mToolbar = (Toolbar)findViewById(R.id.favToolbar);
        mToolbar.setTitle("お気に入り");
        setSupportActionBar(mToolbar);

        // ListViewの準備
        mListView = (ListView)findViewById(R.id.favListView);
        mAdapter = new QuestionListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        // ログインユーザーを取得
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // 現在のユーザーのお気に入りを取得する
        mFavData = new HashMap();
        mFavoriteRef = mDatabaseReference.child(Const.FavoritesPATH).child(user.getUid());
        mFavoriteRef.addChildEventListener(mFavListener);

        // 質問にリスナーをセットする
        for(int genre = 1; genre < 5; genre++) {
            mQuestionRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(genre));
            mQuestionRef.addChildEventListener(mEventListener);
        }

        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);

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
}
