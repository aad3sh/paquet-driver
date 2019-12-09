package com.example.paquetdriver;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Main3Activity extends AppCompatActivity {

    EditText name;
    EditText desc;
    EditText quantity;
    Button b;
    TextView tv;
    FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        name = (EditText)findViewById(R.id.editText);
        desc = (EditText)findViewById(R.id.editText2);
        quantity = (EditText)findViewById(R.id.editText3);
        tv = (TextView)findViewById(R.id.textView);
        b = (Button)findViewById(R.id.button2);
        db = FirebaseFirestore.getInstance();

        firebaseAuth = FirebaseAuth.getInstance();

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                CollectionReference orders123 = db.collection(Objects.requireNonNull(firebaseAuth.getUid()));//TODO:get user id and put in place of orders
//                orders123.add(addOrder).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Toast.makeText(Main3Activity.this,"Order Confirmed", Toast.LENGTH_LONG).show();
//                        tv.setText(documentReference.getId());
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(Main3Activity.this, e.getMessage(), Toast.LENGTH_LONG).show();
//                    }
//                });
                CollectionReference root = db.collection("AppRoot");
                //TODO:get user id and put in place of orders
                DocumentReference dr = root.document("User");

                CollectionReference userIDCollection = dr.collection(Objects.requireNonNull(firebaseAuth.getUid()));
                dr = userIDCollection.document("Orders");
                Toast.makeText(Main3Activity.this, dr.getId().toString(), Toast.LENGTH_LONG).show();
//                dr.set(addOrder).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Toast.makeText(Main3Activity.this,"Order Confirmed", Toast.LENGTH_LONG).show();
//                        tv.setText(documentReference.getId());
//
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(Main3Activity.this, e.getMessage(), Toast.LENGTH_LONG).show();
//                    }
//                });
                Map<String, Object> order = new HashMap<>();
                order.put("name", name.getText().toString());
                order.put("desc", desc.getText().toString());
                order.put("quantity", quantity.getText().toString());

                dr.collection("AllOrders").add(order)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(Main3Activity.this,"Order Confirmed", Toast.LENGTH_LONG).show();
                        tv.setText(documentReference.getId());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Main3Activity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
