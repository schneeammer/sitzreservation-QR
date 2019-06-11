
/*
 * MIT License
 *
 * Copyright (c) 2019 Mauricio Giordano <giordano@inevent.us>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.snowbunting.tvm;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.time.Year;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    public final int REQ_CAMERA = 0x1;

    private ZXingScannerView mScannerView;
    private ListView mListView;
    private LinearLayout mRoomView;
    private LinearLayout mMainView;
    private ArrayAdapter<String> mArrayAdapter;
    private Menu mMenu;

    private int menu = R.id.navigation_camera;
    private boolean flash = false;
    private List<Integer> checkedItems = new ArrayList<>();

    Set<String> checked_in_seats;



    private HashMap<String, ToggleButton> seat_buttons = new HashMap<>();

    private ListView.MultiChoiceModeListener multiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
            if (checkedItems.contains(position)) {
                if (!checked) {
                    checkedItems.remove((Integer) position);
                }
            } else if (checked) {
                checkedItems.add(position);
            }

            if (checkedItems.size() > 1) {
                actionMode.setTitle(checkedItems.size() + " " + getString(R.string.items_selected_pl));
            } else {
                actionMode.setTitle(checkedItems.size() + " " + getString(R.string.items_selected_sing));
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getMenuInflater().inflate(R.menu.action_mode, menu);
            setSimpleListMultipleChoice();

            if (checkedItems != null) {
                // Restore state if any
                List<Integer> clone = new ArrayList<>(checkedItems);
                checkedItems.clear();

                for (Integer pos : clone) {
                    mListView.setItemChecked(pos, true);
                }
            } else {
                checkedItems = new ArrayList<>();
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.action_remove) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Remove")
                    .setMessage(R.string.confirm_remove)
                    .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int position) {
                            for (Integer i : checkedItems) {
                                String qrCode = mArrayAdapter.getItem(i);
                                removeHistory(qrCode);
                            }

                            actionMode.finish();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
                return true;

            } else if (menuItem.getItemId() == R.id.action_select_all) {
                for (int i = 0; i < mListView.getCount(); i++) {
                    mListView.setItemChecked(i, true);
                }

                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            setSimpleList();
            if (checkedItems != null) checkedItems.clear();
        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (menu == item.getItemId()) return false;
            menu = item.getItemId();
            switch (item.getItemId()) {
                case R.id.navigation_camera:
                    setCameraPage();
                    return true;
                case R.id.navigation_room:
                    //Intent a = new Intent(MainActivity.this,RoomActivity.class);
                    //startActivity(a);
                    //break;
                    setRoomPage();
                    return true;
                case R.id.navigation_history:
                    setHistoryPage();
                    return true;
            }
            return false;
        }
    };

    private void setCameraPage() {
        mMainView.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
        mRoomView.setVisibility(View.GONE);
        mScannerView.startCamera();
        setFlash(flash);

        if (mMenu != null) {
            mMenu.findItem(R.id.action_flash_on).setVisible(!flash);
            mMenu.findItem(R.id.action_flash_off).setVisible(flash);

            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                mMenu.findItem(R.id.action_flash_on).setVisible(false);
                mMenu.findItem(R.id.action_flash_off).setVisible(false);
            }
        }
    }

    private void  setRoomPage() {
        mMainView.setVisibility(View.GONE);
        mListView.setVisibility(View.GONE);
        mRoomView.setVisibility(View.VISIBLE);
        mScannerView.stopCamera();
        setSimpleList();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showAlertDialog((String) adapterView.getItemAtPosition(i));
            }
        });

        if (mMenu != null) {
            mMenu.findItem(R.id.action_flash_on).setVisible(false);
            mMenu.findItem(R.id.action_flash_off).setVisible(false);
        }



    }

    private void  setHistoryPage() {
        mMainView.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
        mRoomView.setVisibility(View.GONE);
        mScannerView.stopCamera();
        setSimpleList();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showAlertDialog((String) adapterView.getItemAtPosition(i));
            }
        });

        if (mMenu != null) {
            mMenu.findItem(R.id.action_flash_on).setVisible(false);
            mMenu.findItem(R.id.action_flash_off).setVisible(false);
        }
    }

    private void mounted() {
        if (menu == R.id.navigation_camera) {
            setCameraPage();
            mScannerView.setAspectTolerance(0.5f);

        } else if (menu == R.id.navigation_history) {
            setHistoryPage();
        } else {
            setRoomPage();
        }
    }

    private void setSimpleList() {
        int index = mListView.getFirstVisiblePosition();
        View v = mListView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());

        mArrayAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<>(getHistory()));
        mListView.setAdapter(mArrayAdapter);

        mListView.setSelectionFromTop(index, top);
    }

    private void setSimpleListMultipleChoice() {
        int index = mListView.getFirstVisiblePosition();
        View v = mListView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());

        mArrayAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_list_item_multiple_choice, android.R.id.text1, new ArrayList<>(getHistory()));
        mListView.setAdapter(mArrayAdapter);

        mListView.setSelectionFromTop(index, top);
    }

    private Set<String> getHistory() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return new HashSet<>(prefs.getStringSet("history", new HashSet<String>()));
    }

    private void removeHistory(String qrCode) {
        Set<String> set = getHistory();
        set.remove(qrCode);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("history", set);
        editor.apply();
    }

    private void pushHistory(String qrCode) {
        Set<String> set = getHistory();
        set.add(qrCode);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("history", set);
        editor.apply();
    }

    private void setClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.copy_clipboard, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.failed_copy_text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        checked_in_seats = new HashSet<>(prefs.getStringSet("seats_checked_in", new HashSet<String>()));

        mScannerView = findViewById(R.id.scannerView);
        mListView = findViewById(R.id.listView);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(multiChoiceModeListener);
        mRoomView = findViewById(R.id.roomView);
        mMainView = findViewById(R.id.mainView);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        if (savedInstanceState != null) {
            flash = savedInstanceState.getBoolean("flash", false);
            menu = savedInstanceState.getInt("menu", R.id.navigation_room);
            checkedItems = savedInstanceState.getIntegerArrayList("checkedItems");

            if (checkedItems == null) {
                checkedItems = new ArrayList<>();
            }
        }

        // If permission not given
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQ_CAMERA);

        } else {
            mounted();
        }


        mRoomView = findViewById(R.id.roomView);

        List<Integer> emptyIdx = Arrays.asList(0, 1, 10, 11, 20, 21,
                240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251,
                252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263,
                264, 265, 266, 267, 268, 269,
                240+30, 241+30, 250+30, 251+30, 260+30, 261+30, 270+30, 271+30,
                280+30, 281+30, 290+30, 291+30, 300+30, 301+30, 302+30, 303+30, 304+30, 305+30,
                310+30, 311+30, 312+30, 313+30, 314+30, 315+30, 320+30, 321+30,
                322+30, 323+30, 324+30, 325+30);

        String [] colLabels = {"E", "D", "C", "B", "A"};

        LinearLayout.LayoutParams paramRowLayout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams paramButton = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);


        LinearLayout grid = findViewById(R.id.seatGrid);

        for (int row = 0; row < 33; row++) {

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(paramRowLayout);
            for (int col = 0; col < 10; col++) {

                int seat_nr = 2*row + (1 - col%2) - 6*(col < 2?1:0) + 1;

                SquareLayout layout = new SquareLayout(this);

                LinearLayout.LayoutParams paramSquareLayout = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                paramSquareLayout.weight = 1;
                paramSquareLayout.setMargins(
                        3 + (8+(int) getResources().getDimension(R.dimen.seat_padding))*((col+1) % 2),
                        3,
                        3 + (8+(int) getResources().getDimension(R.dimen.seat_padding)) *(col % 2),
                        3
                );
                layout.setLayoutParams(paramSquareLayout);

                if (emptyIdx.contains(10*row+col)){
                    LinearLayout placeholder = new LinearLayout(this);

                    placeholder.setLayoutParams(paramButton);
                    layout.addView(placeholder);
                } else {

                    ToggleButton button = new ToggleButton(this);

                    String seat = String.format(Locale.US, "%s%02d", colLabels[col/2], seat_nr);
                    button.setText(seat);
                    button.setTextOff(seat);
                    button.setTextOn(seat);
                    int x = (int) getResources().getDimension(R.dimen.seat_padding);

                    //noinspection SuspiciousNameCombination
                    button.setPadding(x, x, x, x);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        // only APK 26+

                        button.setAutoSizeTextTypeUniformWithConfiguration(8, 45, 5, TypedValue.COMPLEX_UNIT_SP);

                        // Bugged out for Nokia1
                        // button.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                    } else {
                        button.setTextSize(8);
                    }

                    button.setLayoutParams(paramButton);
                    layout.addView(button);

                    seat_buttons.put(seat, button);
                }
                rowLayout.addView(layout);
            }
            grid.addView(rowLayout);
        }




        //activate Button
        Button next_button = findViewById(R.id.continueScanning);
        next_button.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Button next_button = findViewById(R.id.continueScanning);
                next_button.setVisibility(View.GONE);
                mScannerView.resumeCameraPreview(MainActivity.this);
            }
        });
        Button check_in_button = findViewById(R.id.checkInButton);
        check_in_button.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                for (String seat: seat_buttons.keySet()) {
                    if (seat_buttons.get(seat).isChecked()) {
                        checked_in_seats.add(seat);
                        seat_buttons.get(seat).setChecked(false);
                    }
                }
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putStringSet("seats_checked_in", checked_in_seats);
                editor.apply();

                updateSeats();

            }
        });

        updateSeats();

        TextView info = findViewById(R.id.infoMenu);

        info.setText(R.string.info_init);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("flash", flash);
        outState.putInt("menu", menu);
        outState.putIntegerArrayList("checkedItems", (ArrayList<Integer>) checkedItems);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mounted();
            } else {
                Toast.makeText(this, "You must provide camera permission!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        if ((mListView.getVisibility() == View.GONE) && (mRoomView.getVisibility() == View.GONE)) {
            mScannerView.startCamera();
        }
        updateSeats();

    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result result) {
        pushHistory(result.getText());
        /*showAlertDialog(result.getText(), true);*/

        /* Display text in the textView below the camera */

        parseQR(result);
    }

    private void good(String billnr, String seats, String menu) {
        TextView infoSeats = findViewById(R.id.infoSeats);
        TextView infoBillNr = findViewById(R.id.infoBillNr);
        TextView infoMenu = findViewById(R.id.infoMenu);

        infoBillNr.setText(billnr);
        infoSeats.setText(seats);
        infoMenu.setText(menu);

        infoBillNr.setBackgroundResource(R.color.good);
        infoBillNr.setTextColor(getResources().getColor(R.color.goodText));
        infoSeats.setBackgroundResource(R.color.good);
        infoSeats.setTextColor(getResources().getColor(R.color.goodText));
        infoMenu.setBackgroundResource(R.color.good);
        infoMenu.setTextColor(getResources().getColor(R.color.goodText));

    }

    private void bad(String billnr, String seats, String menu) {
        TextView infoSeats = findViewById(R.id.infoSeats);
        TextView infoBillNr = findViewById(R.id.infoBillNr);
        TextView infoMenu = findViewById(R.id.infoMenu);

        infoBillNr.setText(billnr);
        infoSeats.setText(seats);
        infoMenu.setText(menu);

        infoBillNr.setBackgroundResource(R.color.bad);
        infoBillNr.setTextColor(getResources().getColor(R.color.badText));
        infoSeats.setBackgroundResource(R.color.bad);
        infoSeats.setTextColor(getResources().getColor(R.color.badText));
        infoMenu.setBackgroundResource(R.color.bad);
        infoMenu.setTextColor(getResources().getColor(R.color.badText));

    }


    private boolean validateChecksum(String message, String check) throws NoSuchAlgorithmException {
        /*
         * def verify(s, certificate):
         * # public key
         * n = 1276109729173033093
         * d =  197116842892907279
         *
         * hasher = hashlib.sha256()
         * hasher.update(s.encode('utf-8'))
         * expected = int.from_bytes(hasher.digest(), 'big') % n
         *
         * received = pow(int(base64.decode(certificate)), d, n)
         *
         * print('Expected vs. received: {} - {}'.format(expected, received))
         * return expected == received
         */
        BigInteger n = new BigInteger("1276109729173033093");
        BigInteger d = new BigInteger("197116842892907279");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(
                message.getBytes(StandardCharsets.UTF_8));
        BigInteger expected = new BigInteger( 1, encodedhash ).mod(n);

        byte [] decoded;
        try {
            decoded = Base64.getDecoder().decode(check);
        } catch (IllegalArgumentException e) {
            return false;
        }

        BigInteger received = new BigInteger(1, decoded).modPow(d,n);

        return received.equals(expected);
    }

    private void parseQR(Result result) {

        String data = result.getText();

        try {
            String regex = "^(\\s*TVM\\s*(\\d+)\\s*Nr.\\s*(.+?)\\s*Seat:\\s*(.+?)\\s*Menu:\\s*(.+?)\\s*)Checksum:\\s*(.+?)\\s*$";
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(data);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String event_key = prefs.getString("event_key", getString(R.string.default_key));
            if (matcher.matches()) {

                String menu = (Integer.parseInt(matcher.group(5)) == 1) ? getString(R.string.menu_sing) : getString(R.string.menu_sing);

                // Check the year
                String today = prefs.getString("year", "");
                try {
                    Integer.parseInt(today);
                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        today = String.format("%s", Year.now().getValue());
                    } else {
                        // Backport for 25-
                        today = String.format("%s", Calendar.getInstance().get(Calendar.YEAR));
                    }
                }

                if (!matcher.group(2).equals(today)) {
                    bad(getString(R.string.wrong_year), matcher.group(4),
                            String.format("+ %s %s (%s)", matcher.group(5), menu, matcher.group(2)));

                // Check if it is for the proper event.
                } else if (!matcher.group(3).split("-")[0].equals(event_key)) {  //FIXME
                    bad(getString(R.string.key) + matcher.group(3).split("-")[0] +
                            getString(R.string.not) +
                            prefs.getString("event_key", getString(R.string.default_key)),
                            matcher.group(4),
                            String.format("+ %s %s (%s)", matcher.group(5), menu, matcher.group(3)));

                // Validate Checksum   TODO: Here we should validate the checksum `matcher.group(6)`
                } else if (!validateChecksum(matcher.group(1), matcher.group(6))) {
                    bad(getString(R.string.wrong_checksum), matcher.group(4),
                            String.format("+ %s %s (%s)", matcher.group(5), menu, matcher.group(3))
                    );
                } else {
                    boolean _is_valid = true;

                    // Adding claimed seats to database
                    Set<String> new_seats = new HashSet<>();
                    for (String s: matcher.group(4).split("\\s*,\\s*")) {
                        if (checked_in_seats.contains(s)) {
                            bad(getString(R.string.already_checked_in), matcher.group(4),
                                    String.format("+ %s %s (%s)", matcher.group(5), menu, matcher.group(3))
                            );
                            _is_valid = false;
                        } else {
                            new_seats.add(s);
                        }
                    }

                    if (_is_valid) {
                        checked_in_seats.addAll(new_seats);

                        good(matcher.group(3), matcher.group(4),
                                String.format("+ %s %s", matcher.group(5), menu)
                        );
                        updateSeats();
                    }
                }

            }
            else {
                bad(getString(R.string.unknown_qr), "", data);
            }

        }
        catch(Exception e) {
            bad(getString(R.string.invalid), "", data);
        }

        Button next_button = findViewById(R.id.continueScanning);
        next_button.setVisibility(View.VISIBLE);



    }

    private void updateSeats() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        checked_in_seats = prefs.getStringSet("seats_checked_in", new HashSet<String>());
        Set<String> claimed_seats = prefs.getStringSet("seats_claimed", new HashSet<String>());

        for (String s: seat_buttons.keySet()) {
            ToggleButton button = seat_buttons.get(s);
            if (checked_in_seats.contains(s)) {
                button.setEnabled(false);
            } else {
                button.setEnabled(true);
            }

            if(claimed_seats.contains(s)) {
                button.setBackgroundResource(R.drawable.seat_button_free);
            } else {
                button.setBackgroundResource(R.drawable.seat_button_claimed);
            }

        }

        TextView nr = findViewById(R.id.textNumberFree);
        nr.setText(String.format("%s", 294 - checked_in_seats.size()));
    }

    private void showAlertDialog(final String text) {
        final SpannableString message = new SpannableString(text);
        Linkify.addLinks(message, Linkify.ALL);

        pushHistory(text);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title)
                .setMessage(message)
                .setPositiveButton(R.string.copy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setClipboard(text);
                    }
                })
                .setNeutralButton(R.string.share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                        sendIntent.setType("text/plain");
                        startActivity(sendIntent);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        // mScannerView.resumeCameraPreview(MainActivity.this);
                    }
                })
                .show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ((TextView) Objects.requireNonNull(dialog.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            // Backport for 18-
            TextView tmp =  dialog.findViewById(android.R.id.message);
            if (tmp != null) {
                tmp.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.menu, menu);
        menu.findItem(R.id.action_flash_off).setVisible(false);
        mMenu = menu;

        if (this.menu == R.id.navigation_camera) {
            mMenu.findItem(R.id.action_flash_on).setVisible(!flash);
            mMenu.findItem(R.id.action_flash_off).setVisible(flash);

            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                mMenu.findItem(R.id.action_flash_on).setVisible(false);
                mMenu.findItem(R.id.action_flash_off).setVisible(false);
            }

        } else {
            mMenu.findItem(R.id.action_flash_on).setVisible(false);
            mMenu.findItem(R.id.action_flash_off).setVisible(false);

        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_info) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.info)
                .setMessage(R.string.info_text)
                .setNegativeButton(R.string.okay, null)
                .show();

        } else if (item.getItemId() == R.id.menu_instructions) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.instructions)
                    .setMessage(R.string.instructions_text)
                    .setNegativeButton(R.string.okay, null)
                    .show();

        } else if (item.getItemId() == R.id.action_flash_on) {
            mMenu.findItem(R.id.action_flash_off).setVisible(true);
            mMenu.findItem(R.id.action_flash_on).setVisible(false);
            setFlash(true);


        } else if (item.getItemId() == R.id.action_flash_off) {
            mMenu.findItem(R.id.action_flash_on).setVisible(true);
            mMenu.findItem(R.id.action_flash_off).setVisible(false);
            setFlash(false);

        } else if (item.getItemId() == R.id.menu_settings) {

            Intent i = new Intent(this, PreferencesActivity.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    private void setFlash(final boolean flash) {
        if (mScannerView == null) return;
        if (flash == this.flash) return;
        this.flash = flash;
        try {
            mScannerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScannerView.setFlash(flash);
                }
            }, 1500);
        } catch (Exception ignore) { }
    }

}
