package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.model.Libros;
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private PendingIntent pendingIntent;

    private IntentFilter[] readFilters;

    private EditText inputView, titulo, autor, impreso, braille, multimedia;

    private ListView listView_libros;
    private NdefMessage messageToWrite;

    private IntentFilter[] writeFilters;

    private String[][] writeTechlist;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;


    private List<Libros> listLibros= new ArrayList<Libros>();
    ArrayAdapter<Libros> arrayAdapterLibros;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        inputView = (EditText) findViewById(R.id.input);
        titulo = (EditText) findViewById(R.id.editTextText);
        autor = (EditText) findViewById(R.id.editTextText2);
        impreso = (EditText) findViewById(R.id.editTextText3);
        braille = (EditText) findViewById(R.id.editTextText4);
        multimedia = (EditText) findViewById(R.id.editTextText5);
        listView_libros = findViewById(R.id.lv_datosLibros);

        inicializarFirebase();
        listarDatos();

        try {
            Intent intent = new Intent(this, getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity
                        (this, 0, intent, PendingIntent.FLAG_MUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity
                        (this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
            }
            IntentFilter javadudeFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            javadudeFilter.addDataScheme("http");
            javadudeFilter.addDataAuthority("javadude.com", null);
            IntentFilter textFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED, "text/pain");

            readFilters = new IntentFilter[]{javadudeFilter, textFilter};
            writeFilters = new IntentFilter[]{};
            writeTechlist = new String[][]{
                    {Ndef.class.getName()},
                    {NdefFormatable.class.getName()},
            };

        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        processNFC(getIntent());
    }

    private void enableWrite() {
        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, pendingIntent, writeFilters, writeTechlist);
    }

    private void disableRead() {
        NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableRead();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processNFC(intent);
    }

    private void processNFC(Intent intent) {
        if (messageToWrite != null) {
            writeTag(intent);
        } else {
            readTag(intent);
        }
    }

    private void writeTag(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            try {
                Ndef ndef = Ndef.get(tag);
                if (ndef == null) {
                    NdefFormatable ndefFormatable = NdefFormatable.get(tag);
                    if (ndefFormatable != null) {
                        ndefFormatable.connect();
                        ndefFormatable.format(messageToWrite);
                        ndefFormatable.close();
                        Toast.makeText(this, "Etiqueta formateada y escrita", Toast.LENGTH_SHORT).show();
                    } else {
                        //report the tag canot formatted
                    }
                } else {
                    ndef.connect();
                    ndef.writeNdefMessage(messageToWrite);
                    ndef.close();
                    Toast.makeText(this, "Etiqueta escrita", Toast.LENGTH_SHORT).show();
                }
            } catch (FormatException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                messageToWrite = null;
            }
        }
    }

    private void readTag(Intent intent) {
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        textView.setText("");
        if (messages != null) {
            for (Parcelable message : messages) {
                NdefMessage ndefMessage = (NdefMessage) message;
                for (NdefRecord record : ndefMessage.getRecords()) {
                    switch (record.getTnf()) {
                        case NdefRecord.TNF_WELL_KNOWN:
                            textView.append("Well KNOWN: ");
                            if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                                textView.append("Text: ");
                                textView.append(new String(record.getPayload()));
                                textView.append("/n");
                            } else if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                                textView.append("URL: ");
                                textView.append(new String(record.getPayload()));
                                textView.append("/n");
                            }
                    }
                }
            }
        }
    }

    public void onWriteUrl(View view) {
        String verificar = inputView.getText().toString();
        if (!isValidURL(verificar)) {
            // El texto no es una URL válida, muestra un mensaje de error
            inputView.setError("No se ingreso ningun URL");
        } else if (verificar.isEmpty()) {
            inputView.setError("No se ingreso ningun URL");
        } else {
            NdefRecord record = NdefRecord.createUri(inputView.getText().toString());
            messageToWrite = new NdefMessage(new NdefRecord[]{record});
            textView.setText("Pase sobre la etiqueta");
            inputView.setText(" ");
            enableWrite();
        }
    }

    public boolean isValidURL(String url) {
        String urlPattern = "^(http://www\\.|https://www\\.|http://|https://)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(/.*)?$";
        Pattern pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    private void inicializarFirebase() {
        FirebaseApp.initializeApp(this);
        firebaseDatabase=FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
        databaseReference=firebaseDatabase.getReference();
    }

    public void onRegistrarLibro(View view) {
        String a = titulo.getText().toString();
        String b = autor.getText().toString();
        String c = impreso.getText().toString();
        String d = braille.getText().toString();
        String e = multimedia.getText().toString();
        if (a.equals("") || b.equals("") || c.equals("") || d.equals("") || e.equals("")) {
            validacion();
        } else {
            Libros l = new Libros();
            l.setUid(UUID.randomUUID().toString());
            l.setTitulos(a);
            l.setAutores(b);
            l.setImpresos(c);
            l.setBrailles(d);
            l.setMultimedias(e);
            databaseReference.child("Libros").child(l.getUid()).setValue(l);
            Toast.makeText(this, "Agregado Con Exito", Toast.LENGTH_SHORT).show();
            LimpiarCajas();
        }
    }

    private void LimpiarCajas() {
        titulo.setText("");
        autor.setText("");
        impreso.setText("");
        braille.setText("");
        multimedia.setText("");
    }

    private void validacion() {
        String a = titulo.getText().toString();
        String b = autor.getText().toString();
        String c = impreso.getText().toString();
        String d = braille.getText().toString();
        String e = multimedia.getText().toString();
        if (a.equals("")) {
            titulo.setError("Required");
        } else if (b.equals("")) {
            autor.setError("Required");
        } else if (c.equals("")) {
            impreso.setError("Required");
        } else if (d.equals("")) {
            braille.setError("Required");
        } else if (e.equals("")) {
            multimedia.setError("Required");
        }
    }
    private void listarDatos() {
        databaseReference.child("Libros").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listLibros.clear();
                for (DataSnapshot objSnapshot : snapshot.getChildren()){
                    Libros l = objSnapshot.getValue(Libros.class);
                    listLibros.add(l);

                    arrayAdapterLibros=new ArrayAdapter<Libros>(MainActivity.this, android.R.layout.simple_list_item_1,listLibros);
                    listView_libros.setAdapter(arrayAdapterLibros);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}