package twetailer.console.golf;

import java.util.Calendar;

import twetailer.dto.Command;
import twetailer.dto.Demand;
import twetailer.dto.Entity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

/**
 * Controller of the Demand edition pane
 *
 * @author Dom Derrien
 */
public class DemandEdit extends Activity {

    public final static int MODE_CREATE = 0;
    public final static int MODE_EDIT = 1;
    public final static int PICK_ADDRESS = 2;

    private Long key = null;
    private Calendar dueDate = Calendar.getInstance();

    private final static int DATE_PICKER_DIALOG = 0;
    private final static int TIME_PICKER_DIALOG = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Attach the view
        setContentView(R.layout.demand);

        // Get the parameters
        Bundle extras = getIntent().getExtras();
        dueDate = Calendar.getInstance();
        Long locationKey = null;
        // String rangeUnit = "km";
        Double range = 25.0D;
        if (extras != null) {
            if (extras.get(Entity.KEY) != null) {
                key = extras.getLong(Entity.KEY);
            }
            if (extras.get(Command.LOCATION_KEY) != null) {
                locationKey = extras.getLong(Command.LOCATION_KEY);
            }
            if (extras.get(Demand.RANGE) != null) {
                range = extras.getDouble(Demand.RANGE);
                // rangeUnit = extras.getString(CommandDef.RANGE_UNIT);
            }
            if (extras.get(Command.DUE_DATE) != null) {
                dueDate.setTimeInMillis(extras.getLong(Command.DUE_DATE));
            }
            if (extras.get(Command.QUANTITY) != null) {
                Long quantity = extras.getLong(Command.QUANTITY);
                ((EditText) findViewById(R.id.demand_pane_quantity)).setText(quantity.toString());
            }
            if (extras.get(Command.CRITERIA) != null) {
                String criteria = extras.getString(Command.CRITERIA);
                ((EditText) findViewById(R.id.demand_pane_criteria)).setText(criteria);
            }
            if (extras.get(Command.CC) != null) {
                String cc = extras.getString(Command.CC);
                ((EditText) findViewById(R.id.demand_pane_cc)).setText(cc);
            }
        }
        ((TextView) findViewById(R.id.demand_pane_date)).setText(DateFormat.getMediumDateFormat(this).format(dueDate.getTime()));
        ((TextView) findViewById(R.id.demand_pane_time)).setText(DateFormat.getTimeFormat(this).format(dueDate.getTime()));
        ((EditText) findViewById(R.id.demand_pane_range)).setText(range.toString());
        // ((TextView) findViewById(R.id.demand_pane_range_unit)).setText(range_unit);

        // Get the current city information
        Integer cityIdx = null;
        if (extras != null) {
            City city = new City(locationKey, null, null, getString(R.string.preference_region_list_need_update)); // Temporary object for a lookup
            cityIdx = City.lookupCityIndex(city);
            if (cityIdx == null) {
                cityIdx = City.keepCity(city, true);
            }
        }

        // Update the Spinner with the list of regions
        ArrayAdapter<City> adapter = new ArrayAdapter<City>(this, android.R.layout.simple_spinner_item, City.getRegisteredCities());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = ((Spinner) findViewById(R.id.demand_pane_region));
        spinner.setAdapter(adapter);
        spinner.setSelection(cityIdx == null ? 0 : cityIdx.intValue());

        // Connect the Date & Time buttons
        findViewById(R.id.demand_pane_pick_date_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showDialog(DATE_PICKER_DIALOG);
            }
        });
        findViewById(R.id.demand_pane_pick_time_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showDialog(TIME_PICKER_DIALOG);
            }
        });

        // Update the "See Proposals" button state
        Button seeProposalsButton = (Button) findViewById(R.id.demand_pane_see_proposals_button);
        final long[] proposalKeys = extras == null || extras.get(Demand.PROPOSAL_KEYS) == null ? null : extras.getLongArray(Demand.PROPOSAL_KEYS);
        if (proposalKeys == null || proposalKeys.length == 0) {
            seeProposalsButton.setEnabled(false);
        }
        else {
            seeProposalsButton.setEnabled(true);
            seeProposalsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // Open the Proposal Viewer pane
                    Intent intent = new Intent(DemandEdit.this, ProposalView.class);
                    intent.putExtra("index", 0);
                    intent.putExtra("keys", proposalKeys);
                    startActivity(intent);
                }
            });
        }

        // Update the "Update" button label
        Button updateButton = (Button) findViewById(R.id.demand_pane_update_button);
        updateButton.setText(extras == null ? getString(R.string.create_demand) : getString(R.string.update_demand));

        // Attach the "Update" button to the transfer the updated data
        updateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Update the parameters
                Bundle bundle = new Bundle();
                if (key != null) { bundle.putLong(Entity.KEY, key); }
                City city = (City) ((Spinner) findViewById(R.id.demand_pane_region)).getSelectedItem();
                bundle.putLong(Command.LOCATION_KEY, city.getKey());
                bundle.putDouble(Demand.RANGE, Double.valueOf(((EditText) findViewById(R.id.demand_pane_range)).getText().toString()));
                // bundle.putString(CommandDef.RANGE_UNIT, Long.valueOf(((EditText) findViewById(R.id.demand_pane_range_unit)).getText().toString()));
                bundle.putLong(Command.DUE_DATE, dueDate.getTimeInMillis());
                bundle.putLong(Command.QUANTITY, Long.valueOf(((EditText) findViewById(R.id.demand_pane_quantity)).getText().toString()));
                bundle.putString(Command.CRITERIA, ((EditText) findViewById(R.id.demand_pane_criteria)).getText().toString());
                bundle.putString(Command.CC, ((EditText) findViewById(R.id.demand_pane_cc)).getText().toString());

                // Open the Demand Edition pane
                Intent intent = new Intent();
                intent.putExtras(bundle) ;
                setResult(RESULT_OK, intent);
                finish();
            }

        });

        // Update the "Get Address" button label
        ImageButton getAddressButton = (ImageButton) findViewById(R.id.demand_pane_get_contact_address);
        getAddressButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, People.CONTENT_URI);
                startActivityForResult(intent, PICK_ADDRESS);
            }
        });

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DATE_PICKER_DIALOG:
            return new DatePickerDialog(
                    this,
                    dateSetListener,
                    dueDate.get(Calendar.YEAR),
                    dueDate.get(Calendar.MONTH),
                    dueDate.get(Calendar.DATE)
            );
        case TIME_PICKER_DIALOG:
            return new TimePickerDialog(
                    this,
                    timeSetListener,
                    dueDate.get(Calendar.HOUR),
                    dueDate.get(Calendar.MINUTE),
                    false
            );
        }
        return null;
    }

    // Callback received when the user "sets" the date in the dialog
    private DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dueDate.set(Calendar.YEAR, year);
            dueDate.set(Calendar.MONTH, monthOfYear);
            dueDate.set(Calendar.DATE, dayOfMonth);
            ((TextView) findViewById(R.id.demand_pane_date)).setText(DateFormat.getMediumDateFormat(findViewById(R.id.demand_pane_date).getContext()).format(dueDate.getTime()));
        }
    };

    // Callback received when the user "sets" the date in the dialog
    private TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            dueDate.set(Calendar.HOUR, hourOfDay);
            dueDate.set(Calendar.MINUTE, minute);
            dueDate.set(Calendar.SECOND, 0);
            dueDate.set(Calendar.MILLISECOND, 0);
            ((TextView) findViewById(R.id.demand_pane_time)).setText(DateFormat.getTimeFormat(findViewById(R.id.demand_pane_time).getContext()).format(dueDate.getTime()));
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        //
        // Source of information: http://developer.android.com/guide/topics/providers/content-providers.html
        //

        switch(requestCode) {
        case PICK_ADDRESS:
            if (resultCode == Activity.RESULT_OK) {
                Uri contactData = intent.getData(); // Data for the selected contact
                final Cursor selectedEmails = managedQuery(
                        Uri.withAppendedPath(contactData, People.ContactMethods.CONTENT_DIRECTORY), // To get additional contact information
                        new String[] { People.ContactMethods.DATA, People.ContactMethods.TYPE }, // PROJECTION
                        People.ContactMethods.KIND + "=" + Contacts.KIND_EMAIL, // WHERE clause
                        null, // WHERE parameters
                        null // ORDER BY
                );
                if (selectedEmails.getCount() == 0) {
                    Toast.makeText(this, "No Email set for this contact!", Toast.LENGTH_LONG).show();
                }
                else if (selectedEmails.getCount() == 1) {
                    EditText ccField = (EditText) findViewById(R.id.demand_pane_cc);
                    String currentCC = ccField.getText().toString();
                    selectedEmails.moveToFirst();
                    String selectedEmail = selectedEmails.getString(selectedEmails.getColumnIndex(People.ContactMethods.DATA));
                    if (currentCC.indexOf(selectedEmail) != -1) {
                        String message = getString(R.string.demand_pane_alert_no_email_address);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                    else {
                        Long emailType = selectedEmails.getLong(selectedEmails.getColumnIndex(People.ContactMethods.TYPE));
                        String message = getString(R.string.demand_pane_alert_one_email_added);
                        switch(emailType.intValue()) {
                        case People.ContactMethods.TYPE_HOME: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_home)); break;
                        case People.ContactMethods.TYPE_WORK: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_work)); break;
                        case People.ContactMethods.TYPE_OTHER: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_other)); break;
                        // case People.ContactMethods.TYPE_MOBILE: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_mobile)); break;
                        case People.ContactMethods.TYPE_CUSTOM: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_custom)); break;
                        default: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_unknown));
                        }
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        ccField.setText(currentCC.length() == 0 ? selectedEmail : currentCC + " " + selectedEmail);
                    }
                }
                else { // if (1 < selectedEmails.getCount()) {
                    new AlertDialog.Builder(DemandEdit.this)
                        .setTitle("Alert!")
                        .setMessage("This contact has many Email addresses. For now, only the first one is considered. In a future release, a popup will allow you to select the one you want to populate.")
                        .setIcon(R.drawable.logo_48x48)
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                EditText ccField = (EditText) findViewById(R.id.demand_pane_cc);
                                String currentCC = ccField.getText().toString();
                                selectedEmails.moveToFirst();
                                String selectedEmail = selectedEmails.getString(selectedEmails.getColumnIndex(People.ContactMethods.DATA));
                                if (currentCC.indexOf(selectedEmail) != -1) {
                                    String message = getString(R.string.demand_pane_alert_no_email_address);
                                    Toast.makeText(DemandEdit.this, message, Toast.LENGTH_LONG).show();
                                }
                                else {
                                    Long emailType = selectedEmails.getLong(selectedEmails.getColumnIndex(People.ContactMethods.TYPE));
                                    String message = getString(R.string.demand_pane_alert_one_email_added);
                                    switch(emailType.intValue()) {
                                    case People.ContactMethods.TYPE_HOME: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_home)); break;
                                    case People.ContactMethods.TYPE_WORK: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_work)); break;
                                    case People.ContactMethods.TYPE_OTHER: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_other)); break;
                                    // case People.ContactMethods.TYPE_MOBILE: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_mobile)); break;
                                    case People.ContactMethods.TYPE_CUSTOM: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_custom)); break;
                                    default: message = message.replaceFirst("\\{0\\}", getString(R.string.email_type_unknown));
                                    }
                                    Toast.makeText(DemandEdit.this, message, Toast.LENGTH_SHORT).show();
                                    ccField.setText(currentCC.length() == 0 ? selectedEmail : currentCC + " " + selectedEmail);
                                }
                            }
                        }).show();
                }
            }
            break;
        }
    }

    /*
    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(myEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);

    Toast.makeText(ActivityName.this,"i! Bright Hub", Toast.LENGTH_SHORT/LENGTH_LONG).show();

        new AlertDialog.Builder(DemandEdit.this)
        .setTitle("Alert!")
        .setMessage("Google Android How-to guides in the Bright Hub")
        .setIcon(R.drawable.icon)
        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
     */
}
