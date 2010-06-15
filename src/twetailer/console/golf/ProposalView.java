package twetailer.console.golf;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import twetailer.dto.Command;
import twetailer.dto.Entity;
import twetailer.dto.Proposal;
import twetailer.dto.Store;
import twetailer.http.HttpConnector;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import domderrien.i18n.DateUtils;

/**
 * Controller of the Proposal viewer pane
 *
 * @author Dom Derrien
 */
public class ProposalView extends Activity {

    public final static int MODE_CREATE = 0;
    public final static int MODE_EDIT = 1;
    public final static int PICK_ADDRESS = 2;

    private static int proposalIdx;
    private static long[] proposalKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Attach the view
        setContentView(R.layout.proposal);

        // Get the parameters
        Bundle extras = getIntent().getExtras();
        proposalIdx = extras.getInt("index");
        proposalKeys = extras.getLongArray("keys");

        // Update the "Previous" button state
        Button button = (Button) findViewById(R.id.proposal_pane_previous_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Toast.makeText(ProposalView.this, "Not yet implemented!", Toast.LENGTH_LONG).show();
            }
        });

        // Update the "Next" button state
        button = (Button) findViewById(R.id.proposal_pane_next_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Toast.makeText(ProposalView.this, "Not yet implemented!", Toast.LENGTH_LONG).show();
            }
        });

        setupNavigation(null);

        // Update the "Confirm" button state
        button = (Button) findViewById(R.id.proposal_pane_confirm_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                confirmProposal();
            }
        });

        // Update the "Decline" button state
        button = (Button) findViewById(R.id.proposal_pane_decline_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Toast.makeText(ProposalView.this, "Not yet implemented!", Toast.LENGTH_LONG).show();
            }
        });

        // Load the corresponding Proposal
        loadProposal();
    }

    private void setupNavigation(String state) {
        // Update the "Previous" button state
        Button button = (Button) findViewById(R.id.proposal_pane_previous_button);
        button.setEnabled(0 < proposalIdx);

        // Update the "Next" button state
        button = (Button) findViewById(R.id.proposal_pane_next_button);
        button.setEnabled(proposalIdx + 1 != proposalKeys.length);

        // Update the "Confirm" button state
        button = (Button) findViewById(R.id.proposal_pane_confirm_button);
        button.setEnabled(Entity.STATE_PUBLISHED.equals(state));

        // Update the "Decline" button state
        button = (Button) findViewById(R.id.proposal_pane_decline_button);
        button.setEnabled(Entity.STATE_PUBLISHED.equals(state));
    }

    private ProgressDialog progressDialog;
    private final static int LOAD_OPERATION = 1;
    private final static int CONFIRM_OPERATION = 2;
    private final static int DECLINE_OPERATION = 3;

    protected void loadProposal() {
        progressDialog = ProgressDialog.show(ProposalView.this, "", getString(R.string.dashboard_fetching_data_message));
        new Thread() {
            public void run() {
                Message message = Message.obtain();
                message.what = LOAD_OPERATION;
                Bundle bundle = new Bundle();
                try {
                    String urlParameters = "includeLocaleCodes=true";
                    String response = HttpConnector.getHelper(
                            Command.PROPOSAL_ENTITY,
                            String.valueOf(proposalKeys[proposalIdx]),
                            urlParameters
                    );
                    bundle.putString("response", "" + response);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    bundle.putString(Command.ERROR_MESSAGE, "Error while getting data from back-end: " + ex.getMessage());
                }
                message.setData(bundle);
                messageHandler.sendMessage(message);

            }
        }.start();
    }

    protected void confirmProposal() {
        progressDialog = ProgressDialog.show(ProposalView.this, "", getString(R.string.dashboard_fetching_data_message));
        new Thread() {
            public void run() {
                Message message = Message.obtain();
                message.what = CONFIRM_OPERATION;
                Bundle bundle = new Bundle();
                try {
                    long proposalKey = proposalKeys[proposalIdx];
                    String urlParameters = "{'"+Entity.KEY+"':"+proposalKey+",'"+Entity.STATE+"':'"+Entity.STATE_CONFIRMED+"'}";
                    String response = HttpConnector.putHelper(
                            Command.PROPOSAL_ENTITY,
                            String.valueOf(proposalKey),
                            urlParameters
                    );
                    bundle.putString("response", "" + response);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    bundle.putString(Command.ERROR_MESSAGE, "Error while getting data from back-end: " + ex.getMessage());
                }
                message.setData(bundle);
                messageHandler.sendMessage(message);

            }
        }.start();
    }

    JSONObject currentProposal;
    JSONObject currentStore;

    private Handler messageHandler = new Handler() {
        public void handleMessage(Message message) {
            super.handleMessage(message);

            String errorMessage = message.getData().getString(Command.ERROR_MESSAGE);
            if (errorMessage == null) {
                try {
                    String response = message.getData().getString("response");
                    JSONObject data = (JSONObject) new JSONTokener(response).nextValue();
                    if (data.getBoolean("success")) {
                        switch (message.what) {
                        case LOAD_OPERATION:
                            currentProposal = data.getJSONObject("resource");
                            currentStore = data.getJSONObject("relatedResource");
                            fillData(ProposalView.this, currentProposal, currentStore);
                            break;
                        case CONFIRM_OPERATION:
                            currentProposal.put(Entity.STATE, Entity.STATE_CONFIRMED);
                            if (1 < proposalKeys.length) {
                                proposalKeys = new long[] { proposalKeys[proposalIdx] };
                                proposalIdx = 0;
                            }
                            fillData(ProposalView.this, currentProposal, currentStore);
                            break;
                        case DECLINE_OPERATION:
                            break;
                        }
                    }
                    else {
                        errorMessage = "Error extracting received data for the Property pane -- " + data.getString("reason");
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    errorMessage = "Error extracting received data for the Property pane -- " + ex.getMessage();
                }
            }

            if (errorMessage != null) {
                new AlertDialog.Builder(ProposalView.this)
                .setTitle("Alert!")
                .setMessage(errorMessage)
                .show();
            }

            progressDialog.dismiss();
        }
    };

    private void fillData(Context context, JSONObject proposal, JSONObject store) {
        String state = proposal.optString(Entity.STATE);
        int stateId = R.string.invalid_state;
        if (Entity.STATE_OPEN.equals(state)) { stateId = R.string.open_state; }
        else if (Entity.STATE_PUBLISHED.equals(state)) { stateId = R.string.published_state; }
        else if (Entity.STATE_CONFIRMED.equals(state)) { stateId = R.string.confirmed_state; }
        else if (Entity.STATE_DECLINED.equals(state)) { stateId = R.string.declined_state; }
        else if (Entity.STATE_CANCELLED.equals(state)) { stateId = R.string.cancelled_state; }
        else if (Entity.STATE_CLOSED.equals(state)) { stateId = R.string.closed_state; }
        ((TextView) findViewById(R.id.proposal_pane_state)).setText(ProposalView.this.getString(stateId));

        setupNavigation(state);

        try {
            Date dueDate = DateUtils.isoToDate(proposal.getString(Command.DUE_DATE));
            ((TextView) findViewById(R.id.proposal_pane_date)).setText(DateFormat.getMediumDateFormat(this).format(dueDate.getTime()));
            ((TextView) findViewById(R.id.proposal_pane_time)).setText(DateFormat.getTimeFormat(this).format(dueDate.getTime()));
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("Cannot parse the Proposal dueDate", ex);
        }

        Long quantity = proposal.optLong(Command.QUANTITY);
        ((TextView) findViewById(R.id.proposal_pane_quantity)).setText(quantity.toString());

        Double price = proposal.optDouble(Proposal.PRICE);
        if (price != null) {
            ((TextView) findViewById(R.id.proposal_pane_price)).setText(
                    context.getString(R.string.money_pattern).replaceFirst("\\{0\\}", price.toString()).replaceFirst("\\{1\\}", context.getString(R.string.money_dollar_US))
            );
        }

        Double total = proposal.optDouble(Proposal.TOTAL);
        if (total != null) {
            ((TextView) findViewById(R.id.proposal_pane_total)).setText(
                    context.getString(R.string.money_pattern).replaceFirst("\\{0\\}", total.toString()).replaceFirst("\\{1\\}", context.getString(R.string.money_dollar_US))
            );
        }

        JSONArray criteria = proposal.optJSONArray(Command.CRITERIA);
        StringBuilder criterionSeries = new StringBuilder();
        if (criteria != null) {
            for (int idx=0; idx<criteria.length(); idx++) {
                criterionSeries.append(criteria.optString(idx)).append(" ");
            }
        }
        ((TextView) findViewById(R.id.proposal_pane_criteria)).setText(criterionSeries.toString());

        StringBuilder storeInformation = new StringBuilder();
        storeInformation.append(store.optString(Store.NAME)).append("\n");
        storeInformation.append(store.optString(Store.ADDRESS)).append("\n");
        storeInformation.append(store.optString(Store.PHONE_NUMBER)).append("\n");
        storeInformation.append(store.optString(Store.EMAIL)).append("\n");
        ((TextView) findViewById(R.id.proposal_pane_store)).setText(storeInformation.toString());
    }
}
