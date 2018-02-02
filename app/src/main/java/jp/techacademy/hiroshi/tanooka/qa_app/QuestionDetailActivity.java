package jp.techacademy.hiroshi.tanooka.qa_app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class QuestionDetailActivity extends AppCompatActivity{

    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;

    private DatabaseReference databaseReference;
    private DatabaseReference mAnswerRef;
    private DatabaseReference mFavoriteRef;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap)dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for(Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String)map.get("body");
            String name = (String)map.get("name");
            String uid = (String)map.get("uid");

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
    private ChildEventListener mFavListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            favButtonNotPressed = false;    // すでにお気に入りされている状態
            favButton.setImageResource(R.drawable.star_rate);
            favButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff9800")));
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

    FloatingActionButton favButton;
    boolean favButtonNotPressed = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question)extras.get("question");

        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView)findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
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

        databaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = databaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);

        // お気に入りボタンの実装
        favButton = (FloatingActionButton)findViewById(R.id.favorite);
        favButton.setVisibility(View.GONE);
        if (user != null) {
            // ログインしていれば、favoriteボタンを表示する
            favButton.setVisibility(View.VISIBLE);

            // お気に入りのFirebase保存先とリスナーの登録
            mFavoriteRef = databaseReference.child(Const.FavoritesPATH).child(user.getUid()).child(mQuestion.getQuestionUid());
            mFavoriteRef.addChildEventListener(mFavListener);

            clickFavButton();
        }
    }

    private void clickFavButton() {
        favButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (favButtonNotPressed == true) {
                    // お気に入りにする
                    favButton.setImageResource(R.drawable.star_rate);
                    favButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff9800")));
                    favButtonNotPressed = false;

                    // 質問のIDをFirebaseに保存する
                    HashMap<String, String> data = new HashMap<>();
                    String questionId = String.valueOf(mQuestion.getQuestionUid());
                    data.put("questionUid", questionId);

                    mFavoriteRef.setValue(data);
                } else {
                    // お気に入りを解除する
                    favButton.setImageResource(R.drawable.star_rate_black);
                    favButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#808080")));
                    favButtonNotPressed = true;

                    // Firebaseからお気に入り登録を削除する
                    mFavoriteRef.removeValue();
                }
            }
        });
    }
}
