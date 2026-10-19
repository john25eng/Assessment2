package com.example.assessment2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etAuthor, etYear, etGenre, etSearch;
    private final List<Book> allBooks = new ArrayList<>();
    private final List<Book> shownBooks = new ArrayList<>();
    private BookAdapter adapter;
    private FirebaseFirestore db;
    private Book selectedBook = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        etYear = findViewById(R.id.etYear);
        etGenre = findViewById(R.id.etGenre);
        etSearch = findViewById(R.id.etSearch);

        MaterialButton btnAdd = findViewById(R.id.btnAdd);
        MaterialButton btnUpdate = findViewById(R.id.btnUpdate);
        MaterialButton btnDelete = findViewById(R.id.btnDelete);

        RecyclerView rv = findViewById(R.id.rvBooks);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(shownBooks, this::selectBook);
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addBook());
        btnUpdate.setOnClickListener(v -> updateBook());
        btnDelete.setOnClickListener(v -> deleteBook());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { applySearch(); }
        });

        listenForBooks();
    }

    // live updates from firestore
    private void listenForBooks() {
        db.collection("books").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) {
                Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show();
                return;
            }
            allBooks.clear();
            snap.getDocuments().forEach(doc -> {
                Book b = doc.toObject(Book.class);
                if (b != null) {
                    b.setId(doc.getId());
                    allBooks.add(b);
                }
            });
            applySearch();
        });
    }

    private void applySearch() {
        String q = etSearch.getText() == null ? "" : etSearch.getText().toString().trim().toLowerCase();
        shownBooks.clear();
        for (Book b : allBooks) {
            String title = b.getTitle() == null ? "" : b.getTitle().toLowerCase();
            String author = b.getAuthor() == null ? "" : b.getAuthor().toLowerCase();
            if (q.isEmpty() || title.contains(q) || author.contains(q)) {
                shownBooks.add(b);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void selectBook(Book book) {
        selectedBook = book;
        etTitle.setText(book.getTitle());
        etAuthor.setText(book.getAuthor());
        etYear.setText(String.valueOf(book.getYear()));
        etGenre.setText(book.getGenre());
    }

    private Book readForm() {
        String title = text(etTitle);
        String author = text(etAuthor);
        String yearStr = text(etYear);
        String genre = text(etGenre);

        if (title.isEmpty() || author.isEmpty() || yearStr.isEmpty() || genre.isEmpty()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
            return null;
        }
        return new Book(null, title, author, Integer.parseInt(yearStr), genre);
    }

    private void addBook() {
        Book b = readForm();
        if (b == null) return;
        db.collection("books").add(b)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Book added", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Add failed", Toast.LENGTH_SHORT).show());
    }

    private void updateBook() {
        if (selectedBook == null) {
            Toast.makeText(this, "Tap a book first", Toast.LENGTH_SHORT).show();
            return;
        }
        Book b = readForm();
        if (b == null) return;
        db.collection("books").document(selectedBook.getId()).set(b)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Book updated", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void deleteBook() {
        if (selectedBook == null) {
            Toast.makeText(this, "Tap a book first", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("books").document(selectedBook.getId()).delete()
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Book deleted", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
    }

    private void clearForm() {
        etTitle.setText("");
        etAuthor.setText("");
        etYear.setText("");
        etGenre.setText("");
        selectedBook = null;
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}