package sukses.skripsi.crashdetection;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChooseUserActivity extends AppCompatActivity {
    private ListView listUser;
    private Button btnNext,btnAdd;

    ArrayList<String> listItems = new ArrayList<String>();
    ArrayList<String> listKeys = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    private List<String> phoneNumber = new ArrayList<>();
    private List<String> testing = new ArrayList<>();
    private String id;

    private Boolean itemSelected = false;
    private int selectedPosition = 0;
    private long pId = 0;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = database.getReference("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_user);

        listUser = findViewById(R.id.lUser);
        btnAdd = findViewById(R.id.add);
        btnNext = findViewById(R.id.next);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice,
                listItems);
        listUser.setAdapter(adapter);
        listUser.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listUser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedPosition = i;
                itemSelected = true;
            }
        });

        addChildEventListener();
//
        DatabaseReference mUsersName;
        mUsersName = FirebaseDatabase.getInstance().getReference("users");
        Query qName = mUsersName.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        qName.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setId(dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

//        DatabaseReference mCurrentUser = FirebaseDatabase.getInstance().getReference("users");
//        setId(String.valueOf(mCurrentUser.child(FirebaseAuth.getInstance().getCurrentUser().getUid())));
//        qgetCurrentUser.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });


        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listUser.setItemChecked(selectedPosition,false);
                final String id = String.valueOf(listKeys.get(selectedPosition));

                DatabaseReference mUsersPhone;
                mUsersPhone = FirebaseDatabase.getInstance().getReference("users").child(id);
                mUsersPhone.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String phone = dataSnapshot.child("NoHp").getValue(String.class);
                        String name = dataSnapshot.child("Name").getValue(String.class);
                        Toast.makeText(ChooseUserActivity.this,"Add "+name+ " to your contact",Toast.LENGTH_LONG).show();

                        getPhoneNumber().add(phone);
                        //insert to db
                        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("contact").child(getId());
                        dbRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    pId = (dataSnapshot.getChildrenCount());
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

                        Map contact = new HashMap<>();
                        contact.put("NoHp",phone);
                        dbRef.child(String.valueOf(pId+1)).setValue(contact);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChooseUserActivity.this,MapsActivity.class);
//                intent.putExtra("phoneList", (Parcelable) getPhoneNumber());
                startActivity(intent);
            }
        });
    }
    private void addChildEventListener() {
        ChildEventListener childListener = new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                adapter.add(
                        (String) dataSnapshot.child("Name").getValue());

                listKeys.add(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();
                int index = listKeys.indexOf(key);

                if (index != -1) {
                    listItems.remove(index);
                    listKeys.remove(index);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        dbRef.addChildEventListener(childListener);

    }

    public void getItem(){

    }

    public List<String> getTesting() {
        return testing;
    }

    public void setTesting(List<String> testing) {
        this.testing = testing;
    }

    public List<String> getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(List<String> phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
