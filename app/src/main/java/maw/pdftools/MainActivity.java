package maw.pdftools;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    final static private int PDF_REQ_CODE = 45;
    final static private int SEL_FOLDER_CODE = 4545;

    private final ArrayList<PDFItemHolder> pdfItems = new ArrayList<>();
    private ArrayAdapter<PDFItemHolder> pdfItemsAdapter;
    private PDDocument newDoc;

    @Override
    protected void onStart() {
        super.onStart();
        PDFBoxResourceLoader.init(getApplicationContext());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView pdfList = findViewById(R.id.pdf_list);
        pdfItemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pdfItems);
        pdfList.setAdapter(pdfItemsAdapter);

        Button addPdfButton = findViewById(R.id.add_pdf_button);
        addPdfButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            startActivityForResult(intent, PDF_REQ_CODE);
        });

        Button createPdfButton = findViewById(R.id.create_pdf_button);
        createPdfButton.setOnClickListener(v -> {
            EditText editText = findViewById(R.id.select_pdf_text);
            String text = editText.getText().toString();
            if (text.length() == 0) {
                //just merge all given PDFs..
                newDoc = new PDDocument();
                for (int i = 0; i < pdfItemsAdapter.getCount(); i++) {
                    PDFItemHolder holder = pdfItemsAdapter.getItem(i);
                    PDDocument document = holder.getDocument();
                    for (int j = 0; j < holder.getPageNum(); j++) {
                        newDoc.addPage(document.getPage(j));
                    }
                }
            } else {
                try {
                    String[] textSplit = text.toLowerCase().split(" ");
                    List<PDFPart> parts = new ArrayList<>();
                    for (String s : textSplit) {
                        List<PDFPart> someParts = getPDFParts(s);
                        parts.addAll(someParts);
                    }

                    newDoc = new PDDocument();
                    for (PDFPart part: parts) {
                        newDoc.addPage(part.doc.getPage((part.page)));
                    }

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            if (!checkWritePermission()) {
                requestWritePermission();
            }

            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), SEL_FOLDER_CODE);

        });
    }

    private static final Pattern ALL_DOC = Pattern.compile("([a-z]{1})"),
                                    SINGLE_PAGE = Pattern.compile("([a-z]{1})([0-9]+)"),
                                    PAGE_RANGE = Pattern.compile("([a-z]{1})([0-9]+)-([0-9]+)");

    private List<PDFPart> getPDFParts(String s) throws PDFPartsParseException {
        Matcher matcher = PAGE_RANGE.matcher(s);

        PDFItemHolder holder;
        int start, end;

        if (matcher.find()) {
            holder = getHolderForLetter(matcher.group(1).charAt(0));
            start = Integer.parseInt(matcher.group(2)) - 1;
            end = Integer.parseInt(matcher.group(3)) - 1;
        } else {
            matcher = SINGLE_PAGE.matcher(s);
            if (matcher.find()) {
                holder = getHolderForLetter(matcher.group(1).charAt(0));
                start = Integer.parseInt(matcher.group(2)) - 1;
                end = start + 1;
            } else {
                matcher = ALL_DOC.matcher(s);
                if (matcher.find()) {
                    holder = getHolderForLetter(matcher.group(1).charAt(0));
                    start = 0;
                    end = holder.getPageNum() - 1;
                } else {
                    throw new PDFPartsParseException("Ungültiger Ausdruck: '" + s + "'");
                }
            }
        }
        List<PDFPart> parts = new ArrayList<>();
        for (int i = 0; start < end; start++, i++) {
            parts.add(new PDFPart(holder.getDocument(), start));
        }
        return parts;
    }

    private PDFItemHolder getHolderForLetter(char c) throws PDFPartsParseException {
        // 'a' == 97
        int n = ((int)c)-97;
        if (n < 0 || n > pdfItemsAdapter.getCount() - 1) {
            throw new PDFPartsParseException("'" + c + "' ist kein gültiges Dokument");
        }
        return pdfItemsAdapter.getItem(n);
    }

    class PDFPartsParseException extends Exception {
        public PDFPartsParseException(String msg) {
            super(msg);
        }
    }

    class PDFPart {
        PDDocument doc;
        int page;

        public PDFPart(PDDocument document, int page) {
            this.doc = document;
            this.page = page;
        }
    }

    class PDFItemHolder {
        private String name;
        private PDDocument pdDocument;
        private int pageNum;
        public PDFItemHolder(Uri uri, PDDocument pdDocument) {
            this.pdDocument = pdDocument;

            String[] nameSplit = uri.toString().split("/");

            if (nameSplit.length > 0) {
                name = nameSplit[nameSplit.length-1];
            } else {
                name = uri.toString();
            }

            pageNum = pdDocument.getNumberOfPages();
            name += " (" + pageNum + " pages)";
        }

        public int getPageNum() {
            return pageNum;
        }

        public PDDocument getDocument() {
            return pdDocument;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PDF_REQ_CODE:
                if (resultCode == -1) {
                    if (checkPermission()) {
                        Uri fileUri = data.getData();

                        try {
                            PDDocument document = PDDocument.load(getApplicationContext().getContentResolver().openInputStream(fileUri));
                            pdfItemsAdapter.add(new PDFItemHolder(fileUri, document));
                            pdfItemsAdapter.notifyDataSetChanged();

                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                    } else {
                        requestPermission();
                    }

                }

                break;
            case SEL_FOLDER_CODE:
                String path = UriHelper.getRealPath(this, data.getData());
                File file = null;
                boolean first = true;
                int cnt = 1;
                do {
                    if (first) {
                        file = new File(path, "extract.pdf");
                        first = false;
                    } else {
                        file = new File(path, "extract_" + cnt + ".pdf");
                    }
                } while (file.exists());

                try {
                    newDoc.save(file);
                    Toast.makeText(MainActivity.this, "Dokument erfolgreich gespeichert!", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Read External Storage permission allows us to read files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PDF_REQ_CODE);
        }
    }

    private boolean checkWritePermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestWritePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to write files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PDF_REQ_CODE);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}